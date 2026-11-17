package com.brynflow.shapesense

/**
 * Recognizes hand-drawn shapes. Pure Kotlin, no Android dependency, so it is easy to unit test.
 *
 * ```
 * val recognizer = ShapeRecognizer()
 * val match = recognizer.recognize(points)   // ShapeMatch("circle", 0.91f) or null
 * ```
 *
 * Instances are immutable and thread-safe. Use [plus] to add your own shapes.
 */
public class ShapeRecognizer(
    public val templates: List<ShapeTemplate> = BuiltInShapes.templates(),
    public val resolution: Int = DEFAULT_RESOLUTION,
    /**
     * How far (in degrees) a drawing may be tilted and still match. Real hands never draw perfectly
     * upright shapes. Keep it well below 45 so "arrow up" and "arrow right" stay different.
     * Use 0 for strict, rotation-sensitive matching.
     */
    public val tiltToleranceDegrees: Float = DEFAULT_TILT_TOLERANCE,
) {
    private class Prepared(val name: String, val cloud: PointCloud)

    private val prepared: List<Prepared> = templates.flatMap { t ->
        val angles = if (tiltToleranceDegrees <= 0f) listOf(0f)
        else listOf(-tiltToleranceDegrees, -tiltToleranceDegrees / 2, 0f, tiltToleranceDegrees / 2, tiltToleranceDegrees)
        angles.mapNotNull { a ->
            PointCloudMath.normalize(PointCloudMath.rotated(t.points, a), resolution)?.let { Prepared(t.name, it) }
        }
    }

    /** Names of all shapes this recognizer knows. */
    public val shapeNames: Set<String> = prepared.mapTo(LinkedHashSet()) { it.name }

    /**
     * Every known shape (or only those in [allowed]) with its score, best first.
     * Each shape appears once, scored by its best-matching template.
     */
    public fun rank(points: List<GesturePoint>, allowed: Set<String>? = null): List<ShapeMatch> {
        val cloud = PointCloudMath.normalize(points, resolution) ?: return emptyList()
        val best = LinkedHashMap<String, Double>()
        for (t in prepared) {
            if (allowed != null && t.name !in allowed) continue
            val d = PointCloudMath.distance(cloud, t.cloud)
            val old = best[t.name]
            if (old == null || d < old) best[t.name] = d
        }
        return best.map { (name, d) -> ShapeMatch(name, PointCloudMath.score(d)) }
            .sortedByDescending { it.score }
    }

    /**
     * The best matching shape, or `null` when nothing scores at least [minScore], or when the winner
     * does not beat the runner-up by at least [minMargin] (the drawing is ambiguous).
     */
    public fun recognize(
        points: List<GesturePoint>,
        allowed: Set<String>? = null,
        minScore: Float = 0f,
        minMargin: Float = 0f,
    ): ShapeMatch? {
        val ranked = rank(points, allowed)
        val top = ranked.firstOrNull() ?: return null
        if (top.score < minScore) return null
        val second = ranked.getOrNull(1)
        if (second != null && top.score - second.score < minMargin) return null
        return top
    }

    /** A new recognizer that also knows [template]. */
    public operator fun plus(template: ShapeTemplate): ShapeRecognizer =
        ShapeRecognizer(templates + template, resolution, tiltToleranceDegrees)

    public companion object {
        public const val DEFAULT_RESOLUTION: Int = 32
        public const val DEFAULT_TILT_TOLERANCE: Float = 12f

        /** Shared recognizer with the built-in shapes. */
        public val Default: ShapeRecognizer by lazy { ShapeRecognizer() }
    }
}
