package com.youtubestream.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.youtubestream.app.data.settings.normalizeServerUrl
import com.youtubestream.app.ui.appViewModel

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val vm = appViewModel { SettingsViewModel(it.settings, it.piLibraryRepository) }
    val saved by vm.serverUrl.collectAsStateWithLifecycle()
    val test by vm.test.collectAsStateWithLifecycle()
    var field by remember { mutableStateOf(saved) }
    val focusManager = LocalFocusManager.current

    // Sync the field once the persisted value arrives.
    LaunchedEffect(saved) { field = saved }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Server URL (scheme://host:port)")
        OutlinedTextField(
            value = field,
            onValueChange = { field = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = {
                vm.save(field)
                focusManager.clearFocus()
            }),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.save(field) }) { Text("Save") }
            OutlinedButton(onClick = { vm.reset() }) { Text("Reset") }
            // Test the *saved* URL — enabled when the field already matches what's saved (normalized too, so a
            // schemeless retype of the saved host doesn't leave Test stuck disabled on a conflated StateFlow).
            OutlinedButton(
                onClick = { vm.testConnection() },
                enabled = field == saved || normalizeServerUrl(field) == saved,
            ) { Text("Test connection") }
        }
        Text(
            when (val t = test) {
                is TestResult.Idle -> "Save, then test the connection."
                is TestResult.Testing -> "Testing…"
                is TestResult.Ok -> "✓ Connected — library has ${t.count} songs"
                is TestResult.Failed -> "✗ ${t.message}"
            }
        )
    }
}
