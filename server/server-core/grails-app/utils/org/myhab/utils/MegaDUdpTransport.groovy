package org.myhab.utils

import groovy.util.logging.Slf4j

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketAddress
import java.net.SocketTimeoutException

/**
 * UDP transport for MegaD-328/2561 controllers.
 *
 * Ports the network slice of the megad-cli (D:\git\myhab-megad) — discovery,
 * ping, and IP-change. Firmware flashing is intentionally omitted.
 *
 * Protocol summary:
 *   AA {seq} {cmd} [DA CA] [payload]
 *   - HEADER 0xAA, sequence byte, command byte
 *   - "new bootloader" devices expect the marker DA CA after the cmd byte
 *   - device listens on UDP 52000, responds to UDP 42000
 */
@Slf4j
class MegaDUdpTransport {

    static final byte HEADER = (byte) 0xAA

    static final byte CMD_PING      = (byte) 0x00
    static final byte CMD_CHANGE_IP = (byte) 0x04
    static final byte CMD_SCAN      = (byte) 0x0C

    static final byte[] NEW_BL_MARKER = [(byte) 0xDA, (byte) 0xCA] as byte[]

    static final byte SCAN_BOOTLOADER_MODE = (byte) 0x0C

    static final int UDP_DEVICE_PORT = 52000
    static final int UDP_LOCAL_PORT  = 42000

    static final int PASSWORD_FIELD_LEN = 5

    static final String DEFAULT_BOOTLOADER_IP = '192.168.0.14'

    static final int SCAN_DURATION_MS = 1500
    static final int RECEIVE_TIMEOUT_MS = 300

    /**
     * Broadcast a scan packet and collect all responses for ~1.5s.
     * Returns a list of mutable maps so callers can enrich entries
     * (e.g. stamp mqttId from HTTP).
     *
     * @return list of [ip: String, bootloaderMode: boolean, mqttId: null]
     */
    List<Map<String, Object>> discoverDevices() {
        List<Map<String, Object>> result = []
        String localIp = detectLocalIp()
        if (!localIp) {
            log.warn("Could not auto-detect local IP for UDP scan")
            return result
        }

        DatagramSocket receiveSocket = null
        DatagramSocket sendSocket = null
        try {
            InetAddress broadcast = InetAddress.getByName(deriveBroadcast(localIp))

            receiveSocket = new DatagramSocket((SocketAddress) null)
            receiveSocket.setReuseAddress(true)
            receiveSocket.bind(new InetSocketAddress(localIp, UDP_LOCAL_PORT))
            receiveSocket.setSoTimeout(RECEIVE_TIMEOUT_MS)

            sendSocket = new DatagramSocket()
            sendSocket.setBroadcast(true)

            byte[] scanPacket = buildPacket(0, CMD_SCAN, true, null)
            DatagramPacket out = new DatagramPacket(scanPacket, scanPacket.length, broadcast, UDP_DEVICE_PORT)
            sendSocket.send(out)

            Set<String> seen = new HashSet<>()
            long deadline = System.currentTimeMillis() + SCAN_DURATION_MS
            byte[] buf = new byte[512]
            while (System.currentTimeMillis() < deadline) {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length)
                try {
                    receiveSocket.receive(pkt)
                } catch (SocketTimeoutException ignored) {
                    continue
                }
                byte[] payload = Arrays.copyOf(pkt.getData(), pkt.getLength())
                Map<String, Object> info = parseScanResponse(payload)
                if (info != null && seen.add((String) info.ip)) {
                    info.mqttId = null
                    result << info
                }
            }
        } catch (Exception e) {
            log.warn("UDP discovery failed: {}", e.message)
        } finally {
            try { receiveSocket?.close() } catch (Exception ignored) {}
            try { sendSocket?.close() } catch (Exception ignored) {}
        }
        return result
    }

    /**
     * Send a unicast ping (CMD_PING) and report whether the device replies.
     */
    boolean pingDevice(String ip) {
        String localIp = detectLocalIp()
        if (!localIp) {
            log.warn("Could not auto-detect local IP for UDP ping")
            return false
        }

        DatagramSocket socket = null
        try {
            socket = new DatagramSocket((SocketAddress) null)
            socket.setReuseAddress(true)
            socket.bind(new InetSocketAddress(localIp, UDP_LOCAL_PORT))
            socket.setSoTimeout(RECEIVE_TIMEOUT_MS)

            InetAddress target = InetAddress.getByName(ip)
            byte[] packet = buildPacket(0, CMD_PING, true, null)
            DatagramPacket out = new DatagramPacket(packet, packet.length, target, UDP_DEVICE_PORT)
            socket.send(out)

            byte[] buf = new byte[64]
            DatagramPacket pkt = new DatagramPacket(buf, buf.length)
            try {
                socket.receive(pkt)
                byte[] resp = Arrays.copyOf(pkt.getData(), pkt.getLength())
                return resp.length >= 2 && (resp[0] & 0xFF) == 0xAA
            } catch (SocketTimeoutException ignored) {
                return false
            }
        } catch (Exception e) {
            log.warn("UDP ping failed for {}: {}", ip, e.message)
            return false
        } finally {
            try { socket?.close() } catch (Exception ignored) {}
        }
    }

    /**
     * Issue CMD_CHANGE_IP. Tries the old protocol (no DA CA marker) first,
     * then the new-bootloader protocol if no answer arrives.
     *
     * @return true if the device replied with success (response[1] == 0x01)
     */
    boolean changeIp(String oldIp, String newIp, String password) {
        if (!isValidIp(oldIp) || !isValidIp(newIp)) {
            log.warn("changeIp called with invalid addresses old={} new={}", oldIp, newIp)
            return false
        }
        if (password != null && password.length() > PASSWORD_FIELD_LEN) {
            log.warn("changeIp password too long ({} chars, max {})", password.length(), PASSWORD_FIELD_LEN)
            return false
        }

        String localIp = detectLocalIp()
        if (!localIp) {
            log.warn("Could not auto-detect local IP for UDP IP-change")
            return false
        }

        DatagramSocket receiveSocket = null
        DatagramSocket sendSocket = null
        try {
            InetAddress broadcast = InetAddress.getByName(deriveBroadcast(localIp))

            receiveSocket = new DatagramSocket((SocketAddress) null)
            receiveSocket.setReuseAddress(true)
            receiveSocket.bind(new InetSocketAddress(localIp, UDP_LOCAL_PORT))
            receiveSocket.setSoTimeout(RECEIVE_TIMEOUT_MS)

            sendSocket = new DatagramSocket()
            sendSocket.setBroadcast(true)

            // Try old protocol (no DA CA), then new protocol
            for (boolean newBootloader : [false, true]) {
                byte[] payload = buildIpChangePayload(password, oldIp, newIp)
                byte[] packet = buildPacket(0, CMD_CHANGE_IP, newBootloader, payload)

                sendSocket.send(new DatagramPacket(packet, packet.length, broadcast, UDP_DEVICE_PORT))

                byte[] buf = new byte[64]
                DatagramPacket pkt = new DatagramPacket(buf, buf.length)
                try {
                    receiveSocket.receive(pkt)
                    byte[] resp = Arrays.copyOf(pkt.getData(), pkt.getLength())
                    if (resp.length >= 2 && (resp[0] & 0xFF) == 0xAA) {
                        int code = resp[1] & 0xFF
                        if (code == 1) {
                            return true
                        }
                        if (code == 2) {
                            log.warn("changeIp rejected: wrong password (old={}, new={})", oldIp, newIp)
                            return false
                        }
                        log.warn("changeIp unexpected response code {} for old={}", code, oldIp)
                        return false
                    }
                } catch (SocketTimeoutException ignored) {
                    // try next protocol variant
                }
            }
            log.warn("changeIp: device {} did not respond", oldIp)
            return false
        } catch (Exception e) {
            log.warn("UDP changeIp failed: {}", e.message)
            return false
        } finally {
            try { receiveSocket?.close() } catch (Exception ignored) {}
            try { sendSocket?.close() } catch (Exception ignored) {}
        }
    }

    // ==================== Packet helpers ====================

    private static byte[] buildPacket(int seq, byte cmd, boolean newBootloader, byte[] payload) {
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        out.write(HEADER as int)
        out.write(seq & 0xFF)
        out.write(cmd as int)
        if (newBootloader) {
            out.write(NEW_BL_MARKER, 0, NEW_BL_MARKER.length)
        }
        if (payload != null && payload.length > 0) {
            out.write(payload, 0, payload.length)
        }
        return out.toByteArray()
    }

    private static byte[] buildIpChangePayload(String password, String oldIp, String newIp) {
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        byte[] pwd = new byte[PASSWORD_FIELD_LEN]
        if (password) {
            byte[] raw = password.getBytes('US-ASCII')
            System.arraycopy(raw, 0, pwd, 0, Math.min(raw.length, PASSWORD_FIELD_LEN))
        }
        out.write(pwd, 0, pwd.length)
        writeIpBytes(out, oldIp)
        writeIpBytes(out, newIp)
        return out.toByteArray()
    }

    private static void writeIpBytes(ByteArrayOutputStream out, String ip) {
        ip.split('\\.').each { String part -> out.write(Integer.parseInt(part) & 0xFF) }
    }

    private static Map<String, Object> parseScanResponse(byte[] pkt) {
        if (pkt == null || pkt.length < 5) {
            return null
        }
        if ((pkt[0] & 0xFF) != 0xAA) {
            return null
        }
        int byte2 = pkt[2] & 0xFF
        if (byte2 == (SCAN_BOOTLOADER_MODE & 0xFF)) {
            if (pkt.length >= 7
                    && (pkt[3] & 0xFF) == 0xFF
                    && (pkt[4] & 0xFF) == 0xFF
                    && (pkt[5] & 0xFF) == 0xFF
                    && (pkt[6] & 0xFF) == 0xFF) {
                return [ip: DEFAULT_BOOTLOADER_IP, bootloaderMode: true]
            }
            if (pkt.length < 7) {
                return null
            }
            String ip = "${pkt[3] & 0xFF}.${pkt[4] & 0xFF}.${pkt[5] & 0xFF}.${pkt[6] & 0xFF}"
            return [ip: ip, bootloaderMode: true]
        }
        String ip = "${pkt[1] & 0xFF}.${pkt[2] & 0xFF}.${pkt[3] & 0xFF}.${pkt[4] & 0xFF}"
        return [ip: ip, bootloaderMode: false]
    }

    // ==================== Network helpers ====================

    static String detectLocalIp() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces()
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement()
                if (iface.isLoopback() || !iface.isUp()) {
                    continue
                }
                for (def addr : iface.getInterfaceAddresses()) {
                    String ip = addr.getAddress().getHostAddress()
                    if (ip == null || ip.contains(':')) {
                        continue
                    }
                    if (ip.startsWith('192.168.') || ip.startsWith('10.') || ip.startsWith('172.16.')) {
                        return ip
                    }
                }
            }
        } catch (Exception e) {
            log.debug("detectLocalIp failed: {}", e.message)
        }
        return null
    }

    private static String deriveBroadcast(String ip) {
        String[] parts = ip.split('\\.')
        return "${parts[0]}.${parts[1]}.${parts[2]}.255"
    }

    private static boolean isValidIp(String ip) {
        if (!ip) return false
        String[] parts = ip.split('\\.')
        if (parts.length != 4) return false
        for (String part : parts) {
            try {
                int v = Integer.parseInt(part)
                if (v < 0 || v > 255) return false
            } catch (NumberFormatException e) {
                return false
            }
        }
        return true
    }
}
