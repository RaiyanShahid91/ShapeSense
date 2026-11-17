package com.raiyanshahid.shapesense

/** Defaults shared by the recognizer and the Compose modifier. */
public object ShapeGestureDefaults {
    /**
     * Minimum score (0..1) for a drawing to count as a shape. Measured on simulated hand-drawn
     * gestures: 0.55 keeps about 86% of real shapes and rejects about 99.8% of scribbles, lines and
     * letters. Lower it (0.45-0.5) to be more forgiving; raise it for destructive actions.
     */
    public const val MIN_SCORE: Float = 0.55f

    /** How long to wait after the finger lifts before deciding the drawing is finished. */
    public const val IDLE_TIMEOUT_MILLIS: Long = 450L
}
