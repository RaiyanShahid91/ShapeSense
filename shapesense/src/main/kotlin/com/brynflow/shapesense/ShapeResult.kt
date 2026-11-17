package com.brynflow.shapesense

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/** A drawing that was recognized as a shape. */
@Immutable
public data class ShapeResult(
    /** Shape name, e.g. [Shapes.CIRCLE] or the name of one of your own templates. */
    val name: String,
    /** Match quality from 0 to 1. */
    val score: Float,
    /** Bounding box of the drawing, in the local coordinates of the composable it was drawn on. */
    val bounds: Rect,
    /** The raw strokes, in local coordinates. */
    val strokes: List<List<Offset>>,
    /** The best few candidates, best first (includes the winner). */
    val alternatives: List<ShapeMatch> = emptyList(),
) {
    /** Centre of [bounds]. */
    val center: Offset get() = bounds.center

    /** Turns this drawing into a template, so the user can teach your app a new shape. */
    public fun toTemplate(name: String): ShapeTemplate = strokes.toShapeTemplate(name)
}

/** A drawing that did not match any shape well enough. */
@Immutable
public data class UnrecognizedGesture(
    val bounds: Rect,
    val strokes: List<List<Offset>>,
    /** The closest shape, even though its score was too low (or too ambiguous) to accept. */
    val bestGuess: ShapeMatch?,
) {
    /** Turns this drawing into a template, so the user can teach your app a new shape. */
    public fun toTemplate(name: String): ShapeTemplate = strokes.toShapeTemplate(name)
}

internal fun List<List<Offset>>.toGesturePoints(): List<GesturePoint> =
    flatMapIndexed { id, stroke -> stroke.map { GesturePoint(it.x, it.y, id) } }

internal fun List<List<Offset>>.toShapeTemplate(name: String): ShapeTemplate =
    ShapeTemplate(name, toGesturePoints())

internal fun List<List<Offset>>.boundsOrZero(): Rect {
    var left = Float.MAX_VALUE
    var top = Float.MAX_VALUE
    var right = -Float.MAX_VALUE
    var bottom = -Float.MAX_VALUE
    for (stroke in this) for (p in stroke) {
        if (p.x < left) left = p.x
        if (p.x > right) right = p.x
        if (p.y < top) top = p.y
        if (p.y > bottom) bottom = p.y
    }
    return if (left > right) Rect.Zero else Rect(left, top, right, bottom)
}
