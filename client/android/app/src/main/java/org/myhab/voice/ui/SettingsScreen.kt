package org.myhab.voice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.myhab.voice.data.Session

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onToggleWake: (Boolean) -> Unit
) {
    val language by Session.language.collectAsState()
    val wakeEnabled by Session.wakeEnabled.collectAsState()
    val speakerEnabled by Session.speakerEnabled.collectAsState()

    var baseUrl by remember { mutableStateOf(Session.baseUrl.value) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Server URL
            Text("Server", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            OutlinedButton(
                onClick = { Session.setBaseUrl(baseUrl) },
                modifier = Modifier.padding(top = 8.dp)
            ) { Text("Save URL") }

            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            // Language
            Text("Language", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = language == "en",
                    onClick = { Session.setLanguage("en") },
                    label = { Text("English") }
                )
                FilterChip(
                    selected = language == "ro",
                    onClick = { Session.setLanguage("ro") },
                    label = { Text("Română") }
                )
            }

            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            // Wake word
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Wake word", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Always-on, on-device detection (microWakeWord)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = wakeEnabled, onCheckedChange = onToggleWake)
            }
            Text(
                "Requires a microWakeWord model bundled in the app (assets/wake_word.json " +
                    "+ its .tflite). No account or key needed. See the project README.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            // Audio output
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Respond through phone speaker", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Play replies on the built-in loudspeaker even if a headset " +
                            "or Bluetooth device is connected",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = speakerEnabled,
                    onCheckedChange = { Session.setSpeakerEnabled(it) }
                )
            }

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Log out") }

            Spacer(Modifier.height(8.dp))
            Text(
                "Signed in as ${Session.username ?: "—"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
