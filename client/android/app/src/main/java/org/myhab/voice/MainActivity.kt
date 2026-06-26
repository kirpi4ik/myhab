package org.myhab.voice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.myhab.voice.data.Session
import org.myhab.voice.ui.HomeScreen
import org.myhab.voice.ui.LoginScreen
import org.myhab.voice.ui.SettingsScreen
import org.myhab.voice.ui.theme.MyHabTheme
import org.myhab.voice.voice.VoiceController
import org.myhab.voice.voice.WakeWordService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyHabTheme {
                App(this)
            }
        }
    }
}

/** What to do once the mic (and notifications) permission request returns. */
private enum class Pending { NONE, TALK, WAKE }

@Composable
private fun App(activity: ComponentActivity) {
    val loggedIn by Session.loggedIn.collectAsState()

    if (!loggedIn) {
        LoginScreen()
        return
    }

    var pending by remember { mutableStateOf(Pending.NONE) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val micGranted = result[Manifest.permission.RECORD_AUDIO] == true ||
            hasMic(activity)
        when (pending) {
            Pending.TALK -> if (micGranted) VoiceController.beginTurn()
            Pending.WAKE -> if (micGranted) {
                Session.setWakeEnabled(true)
                WakeWordService.start(activity)
            }
            Pending.NONE -> {}
        }
        pending = Pending.NONE
    }

    fun requiredPermissions(): Array<String> = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    fun onTapToTalk() {
        if (hasMic(activity)) VoiceController.beginTurn()
        else {
            pending = Pending.TALK
            permissionLauncher.launch(requiredPermissions())
        }
    }

    fun onToggleWake(enabled: Boolean) {
        if (!enabled) {
            Session.setWakeEnabled(false)
            WakeWordService.stop(activity)
            return
        }
        if (hasMic(activity)) {
            Session.setWakeEnabled(true)
            WakeWordService.start(activity)
        } else {
            pending = Pending.WAKE
            permissionLauncher.launch(requiredPermissions())
        }
    }

    // Restore the wake-word service when returning to a logged-in app.
    LaunchedEffect(loggedIn) {
        if (loggedIn && Session.wakeEnabled.value && hasMic(activity)) {
            WakeWordService.start(activity)
        }
    }

    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onOpenSettings = { nav.navigate("settings") },
                onTapToTalk = { onTapToTalk() },
                onToggleWake = { onToggleWake(it) }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onLogout = {
                    WakeWordService.stop(activity)
                    Session.logout()
                },
                onToggleWake = { onToggleWake(it) }
            )
        }
    }
}

private fun hasMic(activity: ComponentActivity): Boolean =
    ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
