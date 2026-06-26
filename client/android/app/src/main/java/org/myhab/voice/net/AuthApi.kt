package org.myhab.voice.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Authentication against the Grails Spring-Security-REST endpoint.
 * `POST {baseUrl}/api/login` with `{username, password}` returns a JWT.
 */
object AuthApi {

    /**
     * @return the JWT access token on success.
     * @throws IllegalStateException with a user-facing message on bad credentials
     *         or a transport/parse failure.
     */
    suspend fun login(baseUrl: String, username: String, password: String): String =
        withContext(Dispatchers.IO) {
            val body = Http.json.encodeToString(LoginRequest(username, password))
                .toRequestBody(Http.JSON_MEDIA)
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/login")
                .post(body)
                .header("Accept", "application/json")
                .build()

            Http.client.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (resp.code == 401 || resp.code == 403) {
                    throw IllegalStateException("Invalid username or password")
                }
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Login failed (HTTP ${resp.code})")
                }
                val jwt = runCatching {
                    Http.json.decodeFromString<LoginResponse>(text).jwt()
                }.getOrNull()
                jwt?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Login response had no token")
            }
        }
}
