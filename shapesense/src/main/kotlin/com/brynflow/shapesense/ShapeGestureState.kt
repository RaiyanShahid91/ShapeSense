package com.brynflow.shapesense

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset

/** Holds the strokes being drawn and runs the recognizer. Create it with [rememberShapeGestureState]. */
@Stable
public class ShapeGestureState internal constructor(
    internal val recognizer: ShapeRecognizer,
    internal val config: ShapeGestureConfig,
    private val onRecognized: (ShapeResult) -> Unit,
    private val onUnrecognized: (UnrecognizedGesture) -> Unit,
) {
    internal val completedStrokes = mutableStateListOf<List<Offset>>()
    internal val activeStroke = mutableStateListOf<Offset>()
    internal var trailAlpha by mutableFloatStateOf(1f)
    private var fading = false

    /** `true` while the user is drawing, or between strokes of a multi-stroke drawing. */
    public val isDrawing: Boolean get() = activeStroke.isNotEmpty() || (completedStrokes.isNotEmpty() && !fading)

    internal fun onStrokeStart(position: Offset) {
        if (fading) clearTrail() // a new drawing begins while the old trail is still fading
        activeStroke.clear()
        activeStroke.add(position)
    }

    internal fun onStrokeMove(position: Offset) {
        activeStroke.add(position)
    }

    internal fun onStrokeEnd(minLengthPx: Float) {
        val stroke = activeStroke.toList()
        activeStroke.clear()
        if (strokeLength(stroke) >= minLengthPx) completedStrokes.add(stroke)
    }

    /** Judges everything drawn so far, reports the outcome and fades the trail away. */
    internal suspend fun finish() {
        if (completedStrokes.isEmpty()) return
        val strokes = completedStrokes.toList()
        val ranked = recognizer.rank(strokes.toGesturePoints(), config.allowedShapes)
        val best = ranked.firstOrNull()
        val second = ranked.getOrNull(1)
        fading = true
        if (best != null &&
            best.score >= config.minScore &&
            (second == null || best.score - second.score >= config.minMargin)
        ) {
            onRecognized(ShapeResult(best.name, best.score, strokes.boundsOrZero(), strokes, ranked.take(3)))
        } else {
            onUnrecognized(UnrecognizedGesture(strokes.boundsOrZero(), strokes, best))
        }
        fadeOutTrail()
    }

    private suspend fun fadeOutTrail() {
        val duration = config.fadeOutMillis
        if (!config.showTrail || duration <= 0) {
            clearTrail()
            return
        }
        var start = -1L
        while (true) {
            val now = withFrameNanos { it }
            if (start < 0) start = now
            val t = ((now - start) / 1_000_000f / duration).coerceIn(0f, 1f)
            trailAlpha = 1f - t
            if (t >= 1f) break
        }
        clearTrail()
    }

    private fun clearTrail() {
        completedStrokes.clear()
        fading = false
        trailAlpha = 1f
    }

    private fun strokeLength(points: List<Offset>): Float {
        var length = 0f
        for (i in 1 until points.size) length += (points[i] - points[i - 1]).getDistance()
        return length
    }
}

/**
 * Remembers a [ShapeGestureState].
 *
 * @param onUnrecognized called when something was drawn but matched no shape well enough.
 * @param onShape called when a shape was recognized.
 */
@Composable
public fun rememberShapeGestureState(
    recognizer: ShapeRecognizer = ShapeRecognizer.Default,
    config: ShapeGestureConfig = ShapeGestureConfig(),
    onUnrecognized: ((UnrecognizedGesture) -> Unit)? = null,
    onShape: (ShapeResult) -> Unit,
): ShapeGestureState {
    val currentOnShape by rememberUpdatedState(onShape)
    val currentOnUnrecognized by rememberUpdatedState(onUnrecognized)
    return remember(recognizer, config) {
        ShapeGestureState(
            recognizer = recognizer,
            config = config,
            onRecognized = { currentOnShape(it) },
            onUnrecognized = { currentOnUnrecognized?.invoke(it) },
        )
    }
}
