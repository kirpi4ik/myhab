package org.myhab.services.dsl.action

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.myhab.config.CfgKey
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.services.navimow.MowerCommand
import org.myhab.services.navimow.NavimowApiClient
import org.myhab.services.navimow.NavimowApiException

/**
 * DSL action that controls a Segway Navimow mower over its cloud REST API.
 *
 * <p>Wires the scenario DSL (and the GraphQL {@code mowerCommand} mutation)
 * into {@link NavimowApiClient}. Implements {@link DslCommand} so that bare
 * calls like {@code mowerCommand([deviceId: 42, action: 'DOCK'])} resolve
 * via {@link org.myhab.services.dsl.ScenarioService#methodMissing}, the same
 * way {@code switchOn} → {@link PowerService} works.</p>
 *
 * <p>Failure mode: any problem (missing config, unknown action, Segway-side
 * rejection) is rethrown as {@link NavimowApiException}. The GraphQL resolver
 * catches it and returns {@code {success: false, error}} to the UI; scenario
 * authors can wrap in {@code try / catch} if they want to recover.</p>
 *
 * @see NavimowApiClient
 * @see MowerCommand
 */
@Slf4j
@Transactional(readOnly = true)
class NavimowCommandService implements DslCommand {

    NavimowApiClient navimowApiClient

    /**
     * @param args Map containing:
     *             <ul>
     *               <li><b>deviceId</b> (Long) — myHAB {@link Device} id</li>
     *               <li><b>action</b> (String or {@link MowerCommand}) — START / STOP / PAUSE / RESUME / DOCK</li>
     *             </ul>
     */
    @Override
    def execute(args) {
        if (!(args instanceof Map)) {
            throw new NavimowApiException("mowerCommand requires a Map argument (got ${args?.class?.name})")
        }
        Map params = args as Map

        Long deviceId = params.deviceId == null ? null : (params.deviceId as Long)
        if (deviceId == null) {
            throw new NavimowApiException('mowerCommand: deviceId is required')
        }
        Device device = Device.get(deviceId)
        if (device == null) {
            throw new NavimowApiException("mowerCommand: Device ${deviceId} not found")
        }
        if (device.model != DeviceModel.NAVIMOW_SEGWAY) {
            throw new NavimowApiException("mowerCommand: Device ${deviceId} is not a NAVIMOW_SEGWAY (model=${device.model})")
        }

        MowerCommand cmd = params.action instanceof MowerCommand
                ? (params.action as MowerCommand)
                : MowerCommand.fromName(params.action as String)
        if (cmd == null) {
            throw new NavimowApiException("mowerCommand: unknown action '${params.action}'. Valid: ${MowerCommand.values()*.name()}")
        }

        String tokenKey = CfgKey.DEVICE.DEVICE_OAUTH_ACCESS_TOKEN.key()
        String baseUrlKey = CfgKey.DEVICE.DEVICE_NAVIMOW_API_BASE_URL.key()
        String segwayIdKey = CfgKey.DEVICE.DEVICE_NAVIMOW_DEVICE_ID.key()
        String token = configValue(device, tokenKey)
        String baseUrl = configValue(device, baseUrlKey)
        String segwayDeviceId = configValue(device, segwayIdKey)
        if (!token)          throw new NavimowApiException("mowerCommand: missing per-device config '${tokenKey}' on device ${deviceId}")
        if (!baseUrl)        throw new NavimowApiException("mowerCommand: missing per-device config '${baseUrlKey}' on device ${deviceId}")
        if (!segwayDeviceId) throw new NavimowApiException("mowerCommand: missing per-device config '${segwayIdKey}' on device ${deviceId}")

        log.info("Navimow command ${cmd.name()} → device ${device.code} (segwayId=${segwayDeviceId})")
        navimowApiClient.sendCommand(token, baseUrl, segwayDeviceId, cmd)
        return [success: true, action: cmd.name(), deviceId: deviceId]
    }

    private static String configValue(Device device, String cfgKey) {
        Configuration row = Configuration.where {
            entityId == device.id && entityType == EntityType.DEVICE && key == cfgKey
        }.find()
        String v = row?.value
        return v?.trim() ? v.trim() : null
    }
}
