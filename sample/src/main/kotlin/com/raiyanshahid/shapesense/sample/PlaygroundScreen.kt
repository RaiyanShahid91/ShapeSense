package com.raiyanshahid.shapesense.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.raiyanshahid.shapesense.ShapeRecognizer
import com.raiyanshahid.shapesense.ShapeTemplate
import com.raiyanshahid.shapesense.Shapes
import com.raiyanshahid.shapesense.onShapeDrawn

/** Draw anything; see what ShapeSense thinks it is. Teach it your own shapes. */
@Composable
internal fun PlaygroundScreen() {
    var recognizer by remember { mutableStateOf(ShapeRecognizer.Default) }
    var headline by remember { mutableStateOf("Draw a shape in the box") }
    var detail by remember { mutableStateOf("Circle, check, X, +, triangle, rectangle, star, zigzag, arrows") }
    // Remembers the last drawing so the "Teach" button can turn it into a template.
    var lastDrawing by remember { mutableStateOf<((String) -> ShapeTemplate)?>(null) }
    var newName by remember { mutableStateOf("") }
    var taught by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Text(headline, style = MaterialTheme.typography.headlineSmall)
        Text(detail, style = MaterialTheme.typography.bodyMedium)

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onShapeDrawn(
                    recognizer = recognizer,
                    onUnrecognized = { gesture ->
                        headline = "Not sure what that is"
                        detail = gesture.bestGuess?.let { "Closest: ${label(it.name)} (${percent(it.score)})" }
                            ?: "Try a bigger drawing"
                        lastDrawing = { name -> gesture.toTemplate(name) }
                    },
                ) { result ->
                    headline = "${label(result.name)}  ${percent(result.score)}"
                    detail = result.alternatives.drop(1)
                        .joinToString(prefix = "Runner-ups: ") { "${label(it.name)} ${percent(it.score)}" }
                    lastDrawing = { name -> result.toTemplate(name) }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("draw here", color = MaterialTheme.colorScheme.outline)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Name for last drawing") },
                singleLine = true,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            Button(
                enabled = newName.isNotBlank() && lastDrawing != null,
                onClick = {
                    lastDrawing?.let { make ->
                        recognizer = recognizer + make(newName.trim())
                        taught++
                        headline = "Taught \"${newName.trim()}\""
                        detail = "Draw it again. Teaching the same name more than once makes it more reliable."
                    }
                },
            ) { Text("Teach") }
        }
        if (taught > 0) Text("$taught example(s) taught so far", style = MaterialTheme.typography.labelMedium)
    }
}

private fun percent(score: Float) = "${(score * 100).toInt()}%"

internal fun label(name: String): String = when (name) {
    Shapes.CIRCLE -> "Circle"
    Shapes.CHECK -> "Check mark"
    Shapes.CROSS -> "Cross (X)"
    Shapes.PLUS -> "Plus"
    Shapes.TRIANGLE -> "Triangle"
    Shapes.RECTANGLE -> "Rectangle"
    Shapes.STAR -> "Star"
    Shapes.ZIGZAG -> "Zigzag"
    Shapes.ARROW_UP -> "Arrow up"
    Shapes.ARROW_DOWN -> "Arrow down"
    Shapes.ARROW_LEFT -> "Arrow left"
    Shapes.ARROW_RIGHT -> "Arrow right"
    else -> name
}
