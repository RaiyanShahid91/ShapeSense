package com.raiyanshahid.shapesense

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Tuning knobs for [onShapeDrawn]. The defaults work well for most apps. */
@Immutable
public data class ShapeGestureConfig(
    /** Minimum match score (0..1) to accept a drawing. See [ShapeGestureDefaults.MIN_SCORE]. */
    val minScore: Float = ShapeGestureDefaults.MIN_SCORE,
    /** The winner must beat the runner-up by this much, otherwise the drawing counts as ambiguous. */
    val minMargin: Float = 0f,
    /**
     * After the finger lifts, wait this long for another stroke before judging the drawing.
     * This is what lets a two-stroke "X" work. Use a small value for single-stroke shapes only.
     */
    val idleTimeoutMillis: Long = ShapeGestureDefaults.IDLE_TIMEOUT_MILLIS,
    /** Strokes shorter than this (taps, tiny jitters) are ignored. */
    val minStrokeLength: Dp = 24.dp,
    /** Only these shapes are considered. `null` means all shapes the recognizer knows. */
    val allowedShapes: Set<String>? = null,
    /**
     * Consume the touch events so children (clickables, scrollables) do not also react.
     * When `false` (default) the drawing is observed on the side and everything underneath keeps working.
     */
    val consumeEvents: Boolean = false,
    /** Draw the user's finger trail on top of the content. */
    val showTrail: Boolean = true,
    val trailColor: Color = Color(0xFF7C4DFF),
    val trailWidth: Dp = 4.dp,
    /** How long the trail takes to fade out once the drawing was judged. */
    val fadeOutMillis: Int = 350,
)
