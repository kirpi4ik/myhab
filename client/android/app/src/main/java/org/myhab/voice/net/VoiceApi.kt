package org.myhab.voice.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.myhab.voice.data.Session

/**
 * Calls the `voiceCommand` GraphQL mutation. Mirrors the document in
 * client/web-vue3/src/graphql/queries/voiceCommand.js so the native and web
 * clients exercise the identical server contract.
 */
object VoiceApi {

    private val MUTATION = """
        mutation voiceCommand(${'$'}transcript: String!, ${'$'}locale: String, ${'$'}sessionId: String) {
          voiceCommand(transcript: ${'$'}transcript, locale: ${'$'}locale, sessionId: ${'$'}sessionId) {
            success
            error
            transcript
            spokenResponse
            sessionId
            awaitingReply
            actions
            audioContent
            audioMime
          }
        }
    """.trimIndent()

    /**
     * @throws UnauthorizedException on HTTP 401 (token expired/invalid).
     * @throws IllegalStateException on other transport/parse errors.
     */
    suspend fun voiceCommand(transcript: String): VoiceResult =
        withContext(Dispatchers.IO) {
            val token = Session.token
                ?: throw UnauthorizedException()

            val payload = GraphQlRequest(
                query = MUTATION,
                variables = VoiceVariables(
                    transcript = transcript,
                    locale = Session.speechTag(),
                    sessionId = Session.conversationId
                )
            )
            val body = Http.json.encodeToString(payload).toRequestBody(Http.JSON_MEDIA)
            val request = Request.Builder()
                .url("${Session.baseUrl.value.trimEnd('/')}/graphql")
                .post(body)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $token")
                .build()

            Http.client.newCall(request).execute().use { resp ->
                if (resp.code == 401) throw UnauthorizedException()
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Server error (HTTP ${resp.code})")
                }
                val result = runCatching {
                    Http.json.decodeFromString<GraphQlResponse>(text).data?.voiceCommand
                }.getOrNull()
                    ?: throw IllegalStateException("Could not parse voice response")

                // Carry the conversation forward for multi-turn clarification.
                result.sessionId?.let { Session.conversationId = it }
                result
            }
        }
}
