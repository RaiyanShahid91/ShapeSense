package com.raiyanshahid.shapesense.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    SampleApp()
                }
            }
        }
    }
}

@Composable
private fun SampleApp() {
    var screen by remember { mutableStateOf(Screen.Playground) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        Text("ShapeSense", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.padding(vertical = 8.dp)) {
            for (s in Screen.entries) {
                val modifier = Modifier.padding(end = 8.dp)
                if (s == screen) {
                    Button(onClick = { screen = s }, modifier = modifier) { Text(s.title) }
                } else {
                    OutlinedButton(onClick = { screen = s }, modifier = modifier) { Text(s.title) }
                }
            }
        }
        when (screen) {
            Screen.Playground -> PlaygroundScreen()
            Screen.Shortcuts -> ShortcutsScreen()
        }
    }
}

private enum class Screen(val title: String) {
    Playground("Playground"),
    Shortcuts("Shortcuts"),
}
