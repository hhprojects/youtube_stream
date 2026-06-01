package com.youtubestream.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.youtubestream.app.ui.appViewModel

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val vm = appViewModel { SettingsViewModel(it.settings, it.piLibraryRepository) }
    val saved by vm.serverUrl.collectAsState()
    val test by vm.test.collectAsState()
    var field by remember { mutableStateOf(saved) }

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
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.save(field) }) { Text("Save") }
            OutlinedButton(onClick = { vm.reset() }) { Text("Reset") }
            OutlinedButton(onClick = { vm.testConnection() }) { Text("Test connection") }
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
