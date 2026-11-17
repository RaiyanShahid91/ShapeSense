package com.raiyanshahid.shapesense

/**
 * One sampled touch position.
 *
 * Points that belong to the same stroke share a [strokeId]; this lets the recognizer handle
 * multi-stroke shapes (an "X" drawn as two lines) without inventing a connecting segment.
 */
public data class GesturePoint(
    val x: Float,
    val y: Float,
    val strokeId: Int = 0,
)

/** A candidate shape and how well the drawn gesture matches it, from 0 (no match) to 1 (perfect). */
public data class ShapeMatch(
    val name: String,
    val score: Float,
)
