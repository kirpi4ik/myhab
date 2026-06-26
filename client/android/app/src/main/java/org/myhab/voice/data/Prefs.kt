package org.myhab.voice.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Keystore-backed encrypted key/value store. The JWT never touches plaintext
 * prefs: [EncryptedSharedPreferences] encrypts both keys and values with a master
 * key held in the Android Keystore (AES256-GCM).
 *
 * On the rare event of a corrupted keyset (e.g. an app restored to a device
 * without the original key), we wipe and recreate rather than crash on launch.
 */
object Prefs {

    private const val FILE = "myhab_secure_prefs"

    @Volatile
    private var instance: SharedPreferences? = null

    fun get(context: Context): SharedPreferences {
        return instance ?: synchronized(this) {
            instance ?: create(context.applicationContext).also { instance = it }
        }
    }

    private fun create(appContext: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return try {
            build(appContext, masterKey)
        } catch (e: Exception) {
            // Corrupt keyset — drop the file and rebuild empty (forces re-login).
            appContext.deleteSharedPreferences(FILE)
            build(appContext, masterKey)
        }
    }

    private fun build(appContext: Context, masterKey: MasterKey): SharedPreferences =
        EncryptedSharedPreferences.create(
            appContext,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
}
