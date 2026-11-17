package com.brynflow.shapesense

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Lets the user draw shapes on this composable and tells you what they drew.
 *
 * ```
 * Box(
 *     Modifier.fillMaxSize().onShapeDrawn { result ->
 *         if (result.name == Shapes.CHECK) markDone()
 *     }
 * )
 * ```
 *
 * The drawing is observed on the side: taps, scrolling and other gestures underneath keep working
 * unless you set [ShapeGestureConfig.consumeEvents].
 */
@Suppress("ComposableModifierFactory")
@Composable
public fun Modifier.onShapeDrawn(
    recognizer: ShapeRecognizer = ShapeRecognizer.Default,
    config: ShapeGestureConfig = ShapeGestureConfig(),
    onUnrecognized: ((UnrecognizedGesture) -> Unit)? = null,
    onShape: (ShapeResult) -> Unit,
): Modifier = onShapeDrawn(rememberShapeGestureState(recognizer, config, onUnrecognized, onShape))

/** Same as the other overload, but with a [ShapeGestureState] you created yourself. */
public fun Modifier.onShapeDrawn(state: ShapeGestureState): Modifier =
    this
        .pointerInput(state) { detectShapeGestures(state) }
        .drawWithContent {
            drawContent()
            drawShapeTrail(state)
        }

private suspend fun PointerInputScope.detectShapeGestures(state: ShapeGestureState) {
    val minLengthPx = state.config.minStrokeLength.toPx()
    val idleMillis = state.config.idleTimeoutMillis
    val consume = state.config.consumeEvents
    coroutineScope {
        val scope = this
        var pending: Job? = null
        awaitPointerEventScope {
            while (true) {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pending?.cancel() // another stroke arrived: keep collecting
                state.onStrokeStart(down.position)
                if (consume) down.consume()
                val pointerId = down.id
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                    if (consume) change.consume()
                    if (!change.pressed) break
                    state.onStrokeMove(change.position)
                }
                state.onStrokeEnd(minLengthPx)
                pending = scope.launch {
                    delay(idleMillis)
                    state.finish()
                }
            }
        }
    }
}

private fun ContentDrawScope.drawShapeTrail(state: ShapeGestureState) {
    val config = state.config
    if (!config.showTrail) return
    val stroke = Stroke(width = config.trailWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val faded = config.trailColor.copy(alpha = config.trailColor.alpha * state.trailAlpha)
    for (completed in state.completedStrokes) drawTrail(completed, faded, stroke)
    drawTrail(state.activeStroke, config.trailColor, stroke)
}

private fun DrawScope.drawTrail(points: List<Offset>, color: Color, stroke: Stroke) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(color, radius = stroke.width / 2f, center = points[0])
        return
    }
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }
    drawPath(path, color, style = stroke)
}
