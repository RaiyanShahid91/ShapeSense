package com.raiyanshahid.shapesense.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.raiyanshahid.shapesense.ShapeGestureConfig
import com.raiyanshahid.shapesense.Shapes
import com.raiyanshahid.shapesense.onShapeDrawn

private data class Task(val id: Int, val title: String, val done: Boolean = false, val starred: Boolean = false)

/**
 * A realistic use: draw directly on a list row.
 * Check = done, X = delete, circle = star. Only those three shapes are considered per row.
 */
@Composable
internal fun ShortcutsScreen() {
    val tasks = remember {
        mutableStateListOf(
            Task(1, "Buy milk"),
            Task(2, "Reply to recruiter"),
            Task(3, "Publish my first Compose library"),
            Task(4, "Walk 5,000 steps"),
            Task(5, "Fix the flaky test"),
        )
    }
    val rowConfig = remember {
        ShapeGestureConfig(allowedShapes = setOf(Shapes.CHECK, Shapes.CROSS, Shapes.CIRCLE), minScore = 0.5f)
    }

    Column {
        Text(
            "Draw on a row:  ✓ done   ✗ delete   ○ star",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyColumn {
            items(tasks, key = { it.id }) { task ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .height(88.dp)
                        .onShapeDrawn(config = rowConfig) { result ->
                            val index = tasks.indexOfFirst { it.id == task.id }
                            if (index >= 0) {
                                when (result.name) {
                                    Shapes.CHECK -> tasks[index] = task.copy(done = !task.done)
                                    Shapes.CIRCLE -> tasks[index] = task.copy(starred = !task.starred)
                                    Shapes.CROSS -> tasks.removeAt(index)
                                }
                            }
                        },
                ) {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                        Text(
                            (if (task.starred) "★ " else "") + task.title,
                            style = MaterialTheme.typography.titleMedium,
                            textDecoration = if (task.done) TextDecoration.LineThrough else null,
                        )
                    }
                }
            }
        }
    }
}
