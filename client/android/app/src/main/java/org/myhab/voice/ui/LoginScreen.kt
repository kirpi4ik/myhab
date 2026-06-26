package org.myhab.voice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.myhab.voice.data.Session
import org.myhab.voice.net.AuthApi

@Composable
fun LoginScreen() {
    val scope = rememberCoroutineScope()
    var baseUrl by remember { mutableStateOf(Session.baseUrl.value) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("myHAB Voice", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Sign in to your home server",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it; error = null },
            label = { Text("Server URL") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            enabled = !loading,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        OutlinedTextField(
            value = username,
            onValueChange = { username = it; error = null },
            label = { Text("Username") },
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !loading,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Button(
            enabled = !loading && baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
            onClick = {
                loading = true
                error = null
                val url = baseUrl.trim()
                scope.launch {
                    try {
                        val token = AuthApi.login(url, username.trim(), password)
                        Session.setBaseUrl(url)
                        Session.saveLogin(token, username.trim())
                        // MainActivity observes Session.loggedIn and navigates.
                    } catch (e: Exception) {
                        error = e.message ?: "Login failed"
                    } finally {
                        loading = false
                    }
                }
            }
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign in")
            }
        }
    }
}
