package org.myhab.voice.net

import kotlinx.serialization.Serializable

/** Body of POST /api/login. */
@Serializable
data class LoginRequest(val username: String, val password: String)

/** Relevant fields of the /api/login response (extra fields ignored). */
@Serializable
data class LoginResponse(
    val access_token: String? = null,
    val token: String? = null,
    val login: String? = null
) {
    /** The plugin returns the JWT under access_token; token is an older alias. */
    fun jwt(): String? = access_token ?: token
}

/** GraphQL request envelope: { query, variables }. */
@Serializable
data class GraphQlRequest(
    val query: String,
    val variables: VoiceVariables
)

@Serializable
data class VoiceVariables(
    val transcript: String,
    val locale: String? = null,
    val sessionId: String? = null
)

@Serializable
data class GraphQlResponse(val data: VoiceData? = null)

@Serializable
data class VoiceData(val voiceCommand: VoiceResult? = null)

/**
 * Mirrors VoiceCommandResult in schema.graphqls. [audioContent] is a base64 MP3
 * present only when server-side TTS is enabled.
 */
@Serializable
data class VoiceResult(
    val success: Boolean = false,
    val error: String? = null,
    val transcript: String? = null,
    val spokenResponse: String? = null,
    val sessionId: String? = null,
    val awaitingReply: Boolean = false,
    val actions: List<String>? = null,
    val audioContent: String? = null,
    val audioMime: String? = null
)

/** Thrown when the server rejects the token; the UI clears it and re-prompts login. */
class UnauthorizedException : Exception("Unauthorized")
