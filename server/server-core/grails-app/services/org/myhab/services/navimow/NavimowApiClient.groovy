package org.myhab.services.navimow

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import kong.unirest.HttpResponse
import kong.unirest.Unirest
import org.myhab.utils.HttpErrorUtil

/**
 * Thin Groovy port of the Segway Navimow REST surface used by the official
 * Python SDK (segwaynavimow/navimow-sdk, {@code mower_sdk/api.py}).
 *
 * <p>Stateless — token + base URL are passed in per call so the client can
 * serve many devices / many users without holding mutable state. Mirrors the
 * Unirest + JsonSlurper conventions used by {@link org.myhab.jobs.NibeInfoSyncJob}
 * and {@link org.myhab.jobs.HuaweiInfoSyncJob}.</p>
 *
 * <p>All Segway responses come wrapped as {@code {code, data, desc}}.
 * {@code code == 1} is success; anything else is a server-side rejection (auth
 * expired, command not allowed in current state, etc.) and surfaces as a
 * {@link NavimowApiException}.</p>
 */
@Slf4j
class NavimowApiClient {

    /**
     * Fetch the list of Navimow vehicles the authenticated account owns.
     * Used by {@link org.myhab.services.navimow.NavimowOAuthService} right
     * after token exchange to auto-populate the per-device Segway vehicle
     * id (which the user otherwise has to copy by hand from the Segway app).
     *
     * <p>Each returned map is the raw Segway entry — at minimum it carries
     * an {@code id} or {@code deviceId} field; depending on the firmware
     * also {@code name}, {@code model}, {@code sn}, etc.</p>
     *
     * @throws NavimowApiException on HTTP failure or {@code code != 1}.
     */
    List<Map> listAuthorizedDevices(String token, String baseUrl) {
        // Segway returns {"code":1,"data":{"payload":{"devices":[...]}}}.
        // The body is conventionally empty for this call — the auth comes
        // entirely from the Bearer token. We POST with an empty JSON object
        // to stay consistent with the other /openapi/smarthome/* endpoints.
        Map data = post(token, baseUrl, '/openapi/smarthome/authList', [:])
        Map payload = (data?.payload ?: [:]) as Map
        return (payload.devices ?: []) as List<Map>
    }

    /**
     * Fetch current statuses for one or more mowers in a single call.
     *
     * @return map keyed by Segway device id ({@code String}) to the raw status
     *         payload ({@code Map}). Empty map if Segway returned no devices.
     * @throws NavimowApiException on HTTP failure or {@code code != 1}.
     */
    Map<String, Map> getStatuses(String token, String baseUrl, List<String> deviceIds) {
        if (!deviceIds) return [:]
        Map body = [devices: deviceIds.collect { id -> [id: id] }]
        Map data = post(token, baseUrl, '/openapi/smarthome/getVehicleStatus', body)
        Map payload = (data?.payload ?: [:]) as Map
        List devices = (payload.devices ?: []) as List
        Map<String, Map> result = [:]
        devices.each { Map d ->
            String id = (d.id ?: d.deviceId) as String
            if (id) result[id] = d
        }
        return result
    }

    /**
     * Send one of the {@link MowerCommand} actions to a mower.
     * @throws NavimowApiException on HTTP failure, {@code code != 1}, or when
     *         Segway returns {@code status == "ERROR"} for the command result
     *         (with the exception's {@code errorCode} populated from the
     *         per-command result). The {@code alreadyInState} error is treated
     *         as success — same behaviour as the Python SDK.
     */
    void sendCommand(String token, String baseUrl, String segwayDeviceId, MowerCommand cmd) {
        if (!segwayDeviceId) throw new NavimowApiException('segwayDeviceId is blank')
        if (cmd == null)     throw new NavimowApiException('command is null')

        Map execution = [command: cmd.wireCommand]
        if (cmd.wireParams != null) execution.params = cmd.wireParams
        Map body = [commands: [[devices: [[id: segwayDeviceId]], execution: execution]]]

        Map data = post(token, baseUrl, '/openapi/smarthome/sendCommands', body)
        Map payload = (data?.payload ?: [:]) as Map
        List results = (payload.commands ?: []) as List
        results.each { Map r ->
            if (r.status == 'ERROR') {
                String code = (r.errorCode ?: 'COMMAND_FAILED') as String
                if (code == 'alreadyInState') return  // mirror SDK leniency
                throw new NavimowApiException("Navimow command rejected: ${code}", code)
            }
        }
    }

    // ----------------------------------------------------------------------
    // Internal HTTP plumbing
    // ----------------------------------------------------------------------

    private Map post(String token, String baseUrl, String path, Map jsonBody) {
        validate(token, baseUrl)
        String url = "${baseUrl.replaceAll(/\/+$/, '')}${path}"
        String bodyStr = JsonOutput.toJson(jsonBody)
        String requestId = UUID.randomUUID().toString()
        log.debug("Navimow POST ${url} requestId=${requestId} body=${bodyStr}")
        HttpResponse<String> response
        try {
            response = Unirest.post(url)
                    .header('Authorization', "Bearer ${token}")
                    .header('requestId', requestId)
                    .header('Content-Type', 'application/json')
                    .header('Accept', 'application/json')
                    .body(bodyStr)
                    .asString()
        } catch (Exception ex) {
            throw new NavimowApiException("Navimow ${path} transport error: ${ex.message}", ex)
        }
        return parseEnvelope(response, path)
    }

    private static Map parseEnvelope(HttpResponse<String> response, String path) {
        if (response.status >= 400) {
            String detail = HttpErrorUtil.extractErrorMessage(response.status, response.body ?: '')
            throw new NavimowApiException("Navimow ${path} HTTP ${response.status}: ${detail}")
        }
        Map env
        try {
            env = new JsonSlurper().parseText(response.body ?: '{}') as Map
        } catch (Exception ex) {
            throw new NavimowApiException("Navimow ${path} returned non-JSON: ${ex.message}")
        }
        Object code = env.code
        if (code == null || (code as Integer) != 1) {
            throw new NavimowApiException("Navimow ${path} code=${code} desc=${env.desc}", env.desc as String)
        }
        return (env.data ?: [:]) as Map
    }

    private static void validate(String token, String baseUrl) {
        if (!token?.trim())   throw new NavimowApiException('Navimow access token is missing')
        if (!baseUrl?.trim()) throw new NavimowApiException('Navimow API base URL is missing')
    }
}
