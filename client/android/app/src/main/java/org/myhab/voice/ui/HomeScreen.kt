package org.myhab.voice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.myhab.voice.data.Session
import org.myhab.voice.voice.VoiceController
import org.myhab.voice.voice.VoiceController.Status

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onTapToTalk: () -> Unit,
    onToggleWake: (Boolean) -> Unit
) {
    val status by VoiceController.status.collectAsState()
    val log by VoiceController.log.collectAsState()
    val wakeEnabled by Session.wakeEnabled.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) listState.animateScrollToItem(log.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("myHAB Voice") },
                actions = {
                    IconButton(onClick = { VoiceController.newConversation() }) {
                        Icon(AppIcons.Add, contentDescription = "New conversation")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(AppIcons.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            WakeRow(wakeEnabled = wakeEnabled, onToggleWake = onToggleWake)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(log) { ex -> ExchangeBubble(ex) }
            }

            MicArea(status = status, onTapToTalk = onTapToTalk)
        }
    }
}

@Composable
private fun WakeRow(wakeEnabled: Boolean, onToggleWake: (Boolean) -> Unit) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Wake word", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Listen for “Hey myHAB” in the background",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = wakeEnabled, onCheckedChange = onToggleWake)
        }
    }
}

@Composable
private fun ExchangeBubble(ex: VoiceController.Exchange) {
    val isUser = ex.user != null
    val text = ex.user ?: ex.assistant ?: ""
    val bubbleColor = when {
        ex.isError -> MaterialTheme.colorScheme.errorContainer
        isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(bubbleColor, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            if (ex.actions.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                ex.actions.forEach { a ->
                    Text(
                        "✓ $a",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun MicArea(status: Status, onTapToTalk: () -> Unit) {
    val statusText = when (status) {
        Status.IDLE -> "Tap to talk"
        Status.WAKE -> "Listening for “Hey myHAB”"
        Status.LISTENING -> "Listening…"
        Status.THINKING -> "Thinking…"
        Status.SPEAKING -> "Speaking…"
    }
    val idle = status == Status.IDLE || status == Status.WAKE

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            FilledIconButton(
                onClick = onTapToTalk,
                enabled = idle,
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (status == Status.LISTENING)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    AppIcons.Mic,
                    contentDescription = "Tap to talk",
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            statusText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
