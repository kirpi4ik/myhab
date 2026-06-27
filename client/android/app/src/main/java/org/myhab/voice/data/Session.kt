package org.myhab.voice.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for app state that outlives a screen: the auth token,
 * the server base URL, the spoken language and the wake-word toggle. Durable
 * values are persisted in [Prefs] (encrypted); the conversation [conversationId]
 * is in-memory only (multi-turn context that should not survive an app restart).
 *
 * Mutable values the UI reacts to are exposed as [StateFlow]s.
 */
object Session {

    private const val K_TOKEN = "token"
    private const val K_USERNAME = "username"
    private const val K_BASE_URL = "baseUrl"
    private const val K_LANGUAGE = "language"
    private const val K_WAKE_ENABLED = "wakeEnabled"
    private const val K_SPEAKER_ENABLED = "speakerEnabled"

    /** Emulator → host loopback. On a real device, set the LAN address in Settings. */
    private const val DEFAULT_BASE_URL = "https://madhouse.app"
    private const val DEFAULT_LANGUAGE = "en"

    private lateinit var appContext: Context

    private val _loggedIn = MutableStateFlow(false)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _baseUrl = MutableStateFlow(DEFAULT_BASE_URL)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _language = MutableStateFlow(DEFAULT_LANGUAGE)
    val language: StateFlow<String> = _language.asStateFlow()

    private val _wakeEnabled = MutableStateFlow(false)
    val wakeEnabled: StateFlow<Boolean> = _wakeEnabled.asStateFlow()

    /** When true, spoken replies are forced to the built-in loudspeaker. */
    private val _speakerEnabled = MutableStateFlow(false)
    val speakerEnabled: StateFlow<Boolean> = _speakerEnabled.asStateFlow()

    /** In-memory multi-turn conversation id; null starts a fresh conversation. */
    @Volatile
    var conversationId: String? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val p = Prefs.get(appContext)
        _baseUrl.value = p.getString(K_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        _language.value = p.getString(K_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        _wakeEnabled.value = p.getBoolean(K_WAKE_ENABLED, false)
        _speakerEnabled.value = p.getBoolean(K_SPEAKER_ENABLED, false)
        _loggedIn.value = !p.getString(K_TOKEN, null).isNullOrBlank()
    }

    // ---- auth ---------------------------------------------------------------

    val token: String? get() = Prefs.get(appContext).getString(K_TOKEN, null)
    val username: String? get() = Prefs.get(appContext).getString(K_USERNAME, null)

    fun saveLogin(token: String, username: String) {
        Prefs.get(appContext).edit()
            .putString(K_TOKEN, token)
            .putString(K_USERNAME, username)
            .apply()
        _loggedIn.value = true
    }

    fun logout() {
        Prefs.get(appContext).edit()
            .remove(K_TOKEN)
            .remove(K_USERNAME)
            .apply()
        conversationId = null
        _loggedIn.value = false
    }

    // ---- settings -----------------------------------------------------------

    fun setBaseUrl(value: String) {
        val v = value.trim().removeSuffix("/")
        Prefs.get(appContext).edit().putString(K_BASE_URL, v).apply()
        _baseUrl.value = v
    }

    fun setLanguage(value: String) {
        Prefs.get(appContext).edit().putString(K_LANGUAGE, value).apply()
        _language.value = value
    }

    fun setWakeEnabled(value: Boolean) {
        Prefs.get(appContext).edit().putBoolean(K_WAKE_ENABLED, value).apply()
        _wakeEnabled.value = value
    }

    fun setSpeakerEnabled(value: Boolean) {
        Prefs.get(appContext).edit().putBoolean(K_SPEAKER_ENABLED, value).apply()
        _speakerEnabled.value = value
    }

    /** BCP-47 tag for STT/TTS, derived from the chosen language. */
    fun speechTag(): String = if (_language.value == "ro") "ro-RO" else "en-US"
}
