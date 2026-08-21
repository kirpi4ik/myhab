package org.myhab.services

import grails.util.Holders
import groovy.util.logging.Slf4j
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.web.util.matcher.IpAddressMatcher

/**
 * Resolves the real client IP behind the reverse proxy, and decides whether it
 * belongs to the trusted LAN.
 *
 * <p>The reverse-proxy contract this assumes, which a deployment must satisfy:</p>
 * <ul>
 *   <li>{@code X-Real-IP $remote_addr} — overwrites whatever the client sent,
 *       so it is trustworthy <em>when the socket peer is that proxy</em>.</li>
 *   <li>{@code X-Forwarded-For $proxy_add_x_forwarded_for} — <em>appends</em> to
 *       the client-supplied value, so the first entry is attacker-controlled and
 *       only the last one was written by us.</li>
 * </ul>
 *
 * <p>Devices on the LAN (macvlan) also reach the backend directly on :8181, in
 * which case there is no proxy and no forwarding headers at all.</p>
 *
 * <p>{@code server.forward-headers-strategy} must stay unset — see application.yml.
 * Turning it on would rewrite {@code request.remoteAddr} from the very headers
 * this class exists to distrust.</p>
 */
@Slf4j
class ClientIpService {

    static transactional = false

    /**
     * Empty by default: a deployment must name its own proxy via
     * {@code myhab.security.trustedProxies}. Restrictive, not permissive — with no
     * entry, no forwarding header is believed and the socket peer is used as-is.
     * Shipping one installation's proxy address as a product default would silently
     * grant that address header-spoofing trust everywhere else.
     */
    private static final List<String> DEFAULT_TRUSTED_PROXIES = []
    /** RFC1918 + loopback. ::1/128 matters: IpAddressMatcher denies across address families. */
    private static final List<String> DEFAULT_LAN_CIDRS = [
            '192.168.0.0/16', '10.0.0.0/8', '172.16.0.0/12', '127.0.0.0/8', '::1/128'
    ]

    private List<IpAddressMatcher> trustedProxyMatchers
    private List<IpAddressMatcher> lanMatchers

    /**
     * Client IP, or null when the request cannot be attributed to one we trust:
     * a proxied request with no forwarding header, or forwarding headers arriving
     * from a peer that is not a configured proxy (i.e. a spoof attempt).
     */
    String resolve(HttpServletRequest request) {
        String peer = request?.remoteAddr
        if (!peer) {
            return null
        }

        String realIp = firstNonBlank(request.getHeader('X-Real-IP'))
        String forwardedFor = firstNonBlank(request.getHeader('X-Forwarded-For'))

        if (matchesAny(trustedProxies(), peer)) {
            // Last X-Forwarded-For entry is the one our proxy appended; earlier
            // entries came from the caller and mean nothing.
            String forwarded = realIp ?: lastEntry(forwardedFor)
            if (!forwarded) {
                log.warn("Request via trusted proxy ${peer} carried no X-Real-IP/X-Forwarded-For")
            }
            return forwarded
        }

        if (realIp || forwardedFor) {
            log.warn("Discarding forwarding headers from untrusted peer ${peer}")
            return null
        }

        return peer
    }

    /** True when the request demonstrably originates on the trusted LAN. */
    boolean isTrustedLan(HttpServletRequest request) {
        String ip = resolve(request)
        return ip ? matchesAny(lanCidrs(), ip) : false
    }

    private List<IpAddressMatcher> trustedProxies() {
        if (trustedProxyMatchers == null) {
            trustedProxyMatchers = compile('myhab.security.trustedProxies', DEFAULT_TRUSTED_PROXIES)
        }
        return trustedProxyMatchers
    }

    private List<IpAddressMatcher> lanCidrs() {
        if (lanMatchers == null) {
            lanMatchers = compile('myhab.security.lanCidrs', DEFAULT_LAN_CIDRS)
        }
        return lanMatchers
    }

    private static List<IpAddressMatcher> compile(String key, List<String> fallback) {
        List<String> cidrs = Holders.grailsApplication?.config?.getProperty(key, List, fallback) ?: fallback
        return cidrs.collect { it.toString().trim() }.findAll { it }.collect { String cidr ->
            try {
                return new IpAddressMatcher(cidr)
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Invalid CIDR '${cidr}' in ${key}", e)
            }
        }
    }

    /** IpAddressMatcher throws on anything that isn't an IP — a denial, not an error. */
    private static boolean matchesAny(List<IpAddressMatcher> matchers, String ip) {
        return matchers.any { IpAddressMatcher matcher ->
            try {
                return matcher.matches(ip)
            } catch (IllegalArgumentException ignored) {
                return false
            }
        }
    }

    private static String firstNonBlank(String value) {
        return value?.trim() ?: null
    }

    private static String lastEntry(String forwardedFor) {
        return forwardedFor ? firstNonBlank(forwardedFor.split(',')[-1]) : null
    }
}
