package com.raiyanshahid.shapesense

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Names of the built-in shapes. Custom shapes can use any string. */
public object Shapes {
    public const val CIRCLE: String = "circle"
    public const val CHECK: String = "check"
    public const val CROSS: String = "cross"
    public const val PLUS: String = "plus"
    public const val TRIANGLE: String = "triangle"
    public const val RECTANGLE: String = "rectangle"
    public const val STAR: String = "star"
    public const val ZIGZAG: String = "zigzag"
    public const val ARROW_UP: String = "arrow_up"
    public const val ARROW_DOWN: String = "arrow_down"
    public const val ARROW_LEFT: String = "arrow_left"
    public const val ARROW_RIGHT: String = "arrow_right"

    /** All built-in names. */
    public val all: List<String> = listOf(
        CIRCLE, CHECK, CROSS, PLUS, TRIANGLE, RECTANGLE, STAR, ZIGZAG,
        ARROW_UP, ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT,
    )
}

private typealias Stroke = List<Pair<Float, Float>>

/** The templates behind [Shapes]. Coordinates are screen-like: x right, y down. */
public object BuiltInShapes {

    public fun templates(): List<ShapeTemplate> = buildList {
        // Circle and ovals. Start point and direction do not matter for a point cloud.
        for ((rx, ry) in listOf(1f to 1f, 1f to 0.65f, 0.65f to 1f)) {
            add(ShapeTemplate.ofStrokes(Shapes.CIRCLE, listOf(ellipse(rx, ry))))
        }

        // Check mark: short down-stroke, long up-stroke.
        add(one(Shapes.CHECK, stroke(0f to 0.55f, 0.30f to 0.95f, 1f to 0f)))
        add(one(Shapes.CHECK, stroke(0f to 0.50f, 0.38f to 1.00f, 1f to 0.05f)))
        add(one(Shapes.CHECK, stroke(0.10f to 0.60f, 0.35f to 0.90f, 1f to 0f)))

        // X: two diagonals.
        add(ShapeTemplate.ofStrokes(Shapes.CROSS, listOf(stroke(0f to 0f, 1f to 1f), stroke(1f to 0f, 0f to 1f))))
        add(ShapeTemplate.ofStrokes(Shapes.CROSS, listOf(stroke(0f to 0.15f, 1f to 0.85f), stroke(1f to 0.15f, 0f to 0.85f))))

        // Plus: vertical and horizontal.
        add(ShapeTemplate.ofStrokes(Shapes.PLUS, listOf(stroke(0.5f to 0f, 0.5f to 1f), stroke(0f to 0.5f, 1f to 0.5f))))

        // Triangle, pointing up (plus a flatter one).
        add(one(Shapes.TRIANGLE, stroke(0.5f to 0f, 1f to 1f, 0f to 1f, 0.5f to 0f)))
        add(one(Shapes.TRIANGLE, stroke(0.5f to 0f, 1f to 0.8f, 0f to 0.8f, 0.5f to 0f)))

        // Rectangle: square, wide and tall, with sharp and with rounded corners (fingers round corners).
        for ((w, h) in listOf(1f to 1f, 1f to 0.6f, 0.6f to 1f, 1f to 0.4f)) {
            add(one(Shapes.RECTANGLE, stroke(0f to 0f, w to 0f, w to h, 0f to h, 0f to 0f)))
            add(one(Shapes.RECTANGLE, roundedRect(w, h, 0.12f)))
        }

        // Five-pointed star drawn in one stroke.
        add(one(Shapes.STAR, star()))

        // Zigzag.
        add(one(Shapes.ZIGZAG, stroke(0f to 0.8f, 0.25f to 0.2f, 0.5f to 0.8f, 0.75f to 0.2f, 1f to 0.8f)))
        add(one(Shapes.ZIGZAG, stroke(0f to 0.8f, 0.33f to 0.2f, 0.66f to 0.8f, 1f to 0.2f)))

        // Arrows: a shaft and a head, in four directions.
        for ((head, spread) in listOf(0.70f to 0.30f, 0.78f to 0.22f)) {
            val shaft = stroke(0f to 0.5f, 1f to 0.5f)
            val tip = stroke(head to (0.5f - spread), 1f to 0.5f, head to (0.5f + spread))
            add(arrow(Shapes.ARROW_RIGHT, shaft, tip, quarterTurns = 0))
            add(arrow(Shapes.ARROW_DOWN, shaft, tip, quarterTurns = 1))
            add(arrow(Shapes.ARROW_LEFT, shaft, tip, quarterTurns = 2))
            add(arrow(Shapes.ARROW_UP, shaft, tip, quarterTurns = 3))
        }
    }

    private fun stroke(vararg points: Pair<Float, Float>): Stroke = points.toList()

    private fun one(name: String, stroke: Stroke) = ShapeTemplate.ofStrokes(name, listOf(stroke))

    private fun ellipse(rx: Float, ry: Float, segments: Int = 48): Stroke =
        (0..segments).map { i ->
            val a = 2.0 * PI * i / segments
            (0.5 + 0.5 * rx * cos(a)).toFloat() to (0.5 + 0.5 * ry * sin(a)).toFloat()
        }

    /** A closed rectangle outline whose corners are cut by [cut] (fraction of the shorter side). */
    private fun roundedRect(w: Float, h: Float, cut: Float): Stroke {
        val c = cut * minOf(w, h)
        return stroke(
            c to 0f, (w - c) to 0f, w to c, w to (h - c), (w - c) to h,
            c to h, 0f to (h - c), 0f to c, c to 0f,
        )
    }

    private fun star(): Stroke {
        // Pentagram: visit every second vertex of a pentagon.
        val vertex = { k: Int ->
            val a = -PI / 2 + 2.0 * PI * k / 5
            (0.5 + 0.5 * cos(a)).toFloat() to (0.5 + 0.5 * sin(a)).toFloat()
        }
        return (0..5).map { vertex((it * 2) % 5) }
    }

    /** Rotates the right-pointing arrow by [quarterTurns] x 90 degrees clockwise (y points down). */
    private fun arrow(name: String, shaft: Stroke, tip: Stroke, quarterTurns: Int): ShapeTemplate {
        fun rotate(p: Pair<Float, Float>): Pair<Float, Float> {
            var x = p.first - 0.5f
            var y = p.second - 0.5f
            repeat(quarterTurns) {
                val nx = -y
                val ny = x
                x = nx
                y = ny
            }
            return (x + 0.5f) to (y + 0.5f)
        }
        return ShapeTemplate.ofStrokes(name, listOf(shaft.map(::rotate), tip.map(::rotate)))
    }
}
