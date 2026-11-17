package com.brynflow.shapesense

/**
 * A reference shape that drawn gestures are compared against.
 *
 * Several templates may share a [name]; the best-scoring one wins. Giving a shape a few variants
 * (a square and a wide rectangle, say) makes recognition more forgiving.
 */
public class ShapeTemplate(
    public val name: String,
    public val points: List<GesturePoint>,
) {
    public companion object {
        /**
         * Builds a template from strokes of `(x, y)` pairs in any coordinate system
         * (screen coordinates: y grows downwards). Size and position do not matter.
         */
        public fun ofStrokes(name: String, strokes: List<List<Pair<Float, Float>>>): ShapeTemplate =
            ShapeTemplate(
                name,
                strokes.flatMapIndexed { id, stroke -> stroke.map { (x, y) -> GesturePoint(x, y, id) } },
            )
    }
}
