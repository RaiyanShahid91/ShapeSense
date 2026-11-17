package com.raiyanshahid.shapesense

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates human-like drawings of each shape for tests: random size, aspect ratio, tilt, start
 * point, direction, overshoot, wobble and touch-sampling density. The shapes here are written
 * independently of the built-in templates on purpose, so tests do not just replay the templates.
 */
private typealias Poly = List<Pair<Double, Double>>

internal class GestureSimulator(seed: Long) {
    private val rnd = Random(seed)

    private fun u(a: Double, b: Double) = a + rnd.nextDouble() * (b - a)
    private fun coin() = rnd.nextBoolean()

    /** Draws one gesture of [shape]; returns screen-pixel points. */
    fun draw(shape: String): List<GesturePoint> = render(strokesFor(shape), tilt = u(-12.0, 12.0), aspect = null)

    /** Things that must NOT be recognized as a shape. */
    fun junk(kind: Int): List<GesturePoint> {
        val strokes: List<Poly> = when (kind % 6) {
            0 -> listOf(line(0.0, u(0.0, 1.0), 1.0, u(0.0, 1.0)))                       // straight line
            1 -> listOf(arc(u(0.2, 0.5)))                                               // gentle arc / half circle-ish
            2 -> listOf(sCurve())                                                       // S curve
            3 -> listOf(randomWalk())                                                   // scribble
            4 -> listOf(letterM())                                                      // letter M
            else -> listOf(spiral())                                                    // spiral
        }
        return render(strokes, tilt = u(-180.0, 180.0), aspect = null)
    }

    // ---- shape generators (unit box, y down) ----

    private fun strokesFor(shape: String): List<Poly> = when (shape) {
        Shapes.CIRCLE -> listOf(circle())
        Shapes.CHECK -> listOf(check())
        Shapes.CROSS -> cross()
        Shapes.PLUS -> plus()
        Shapes.TRIANGLE -> listOf(triangle())
        Shapes.RECTANGLE -> listOf(rectangle())
        Shapes.STAR -> listOf(star())
        Shapes.ZIGZAG -> listOf(zigzag())
        Shapes.ARROW_RIGHT -> arrow(0)
        Shapes.ARROW_DOWN -> arrow(1)
        Shapes.ARROW_LEFT -> arrow(2)
        Shapes.ARROW_UP -> arrow(3)
        else -> error("unknown shape $shape")
    }

    private fun circle(): Poly {
        val turns = u(1.0, 1.18)
        val start = u(0.0, 2 * PI)
        val dir = if (coin()) 1 else -1
        val r0 = u(0.9, 1.0)
        val r1 = u(0.9, 1.1)
        val ar = u(0.75, 1.3)
        val n = 80
        return (0..n).map { i ->
            val t = i.toDouble() / n
            val a = start + dir * 2 * PI * turns * t
            val r = 0.5 * (r0 + (r1 - r0) * t)
            (0.5 + r * cos(a)) to (0.5 + r * ar * sin(a))
        }
    }

    private fun check(): Poly {
        val kx = u(0.22, 0.45)
        val sy = u(0.40, 0.65)
        val ty = u(0.0, 0.15)
        return listOf(0.0 to sy, kx to u(0.88, 1.0), 1.0 to ty)
    }

    private fun cross(): List<Poly> {
        val a = line(u(0.0, 0.1), u(0.0, 0.1), u(0.9, 1.0), u(0.9, 1.0))
        val b = line(u(0.9, 1.0), u(0.0, 0.1), u(0.0, 0.1), u(0.9, 1.0))
        val s = listOf(a, b).map { if (coin()) it else it.reversed() }
        return (if (coin()) s else s.reversed())
    }

    private fun plus(): List<Poly> {
        val cx = u(0.42, 0.58)
        val cy = u(0.42, 0.58)
        val v = line(cx, u(0.0, 0.08), cx, u(0.92, 1.0))
        val h = line(u(0.0, 0.08), cy, u(0.92, 1.0), cy)
        val s = listOf(v, h).map { if (coin()) it else it.reversed() }
        return if (coin()) s else s.reversed()
    }

    private fun triangle(): Poly {
        val apex = u(0.3, 0.7)
        val v = listOf(apex to 0.0, 1.0 to 1.0, 0.0 to 1.0)
        val k = rnd.nextInt(3)
        val ring = (0..3).map { v[(k + it) % 3] }
        val p = if (coin()) ring else ring.reversed()
        return round(p + listOf(p[1].first to p[1].second).take(if (coin()) 1 else 0))
    }

    private fun rectangle(): Poly {
        val w = 1.0
        val h = u(0.5, 1.0)
        val v = listOf(0.0 to 0.0, w to 0.0, w to h, 0.0 to h)
        val k = rnd.nextInt(4)
        val ring = (0..4).map { v[(k + it) % 4] }
        return round(if (coin()) ring else ring.reversed())
    }

    private fun star(): Poly {
        val k0 = rnd.nextInt(5)
        val dir = if (coin()) 1 else -1
        val pts = (0..5).map { i ->
            val idx = (k0 + dir * 2 * i + 50) % 5
            val a = -PI / 2 + 2 * PI * idx / 5
            (0.5 + 0.5 * cos(a)) to (0.5 + 0.5 * sin(a))
        }
        return round(pts)
    }

    private fun zigzag(): Poly {
        val turns = rnd.nextInt(3, 6)
        val top = u(0.1, 0.35)
        val bottom = u(0.65, 0.9)
        val pts = (0..turns).map { i -> (i.toDouble() / turns) to (if (i % 2 == 0) bottom else top) }
        return round(if (coin()) pts else pts.reversed())
    }

    private fun arrow(quarterTurns: Int): List<Poly> {
        val head = u(0.62, 0.8)
        val spread = u(0.18, 0.32)
        val shaft = line(0.0, 0.5, 1.0, 0.5)
        val tip = listOf(head to (0.5 - spread), 1.0 to 0.5, head to (0.5 + spread))
        val strokes: List<Poly> =
            if (coin()) listOf(shaft, tip)
            else listOf(listOf(0.0 to 0.5, 1.0 to 0.5, head to (0.5 - spread)), listOf(1.0 to 0.5, head to (0.5 + spread)))
        val ordered = if (coin()) strokes else strokes.reversed()
        return ordered.map { s ->
            s.map { (x, y) ->
                var px = x - 0.5
                var py = y - 0.5
                repeat(quarterTurns) { val nx = -py; py = px; px = nx }
                (px + 0.5) to (py + 0.5)
            }
        }
    }

    // ---- junk generators ----

    private fun arc(sweepFrac: Double): Poly {
        val start = u(0.0, 2 * PI)
        return (0..40).map { i ->
            val a = start + 2 * PI * sweepFrac * i / 40
            (0.5 + 0.5 * cos(a)) to (0.5 + 0.5 * sin(a))
        }
    }

    private fun sCurve(): Poly = (0..40).map { i ->
        val t = i / 40.0
        t to (0.5 + 0.4 * sin(2 * PI * t))
    }

    private fun randomWalk(): Poly {
        var x = 0.5
        var y = 0.5
        var a = u(0.0, 2 * PI)
        return (0..60).map {
            a += u(-1.2, 1.2)
            x += 0.08 * cos(a)
            y += 0.08 * sin(a)
            x to y
        }
    }

    private fun letterM(): Poly = listOf(0.0 to 1.0, 0.0 to 0.0, 0.5 to 0.6, 1.0 to 0.0, 1.0 to 1.0)

    private fun spiral(): Poly = (0..80).map { i ->
        val t = i / 80.0
        val a = 2 * PI * 2.2 * t
        (0.5 + 0.5 * t * cos(a)) to (0.5 + 0.5 * t * sin(a))
    }

    // ---- helpers ----

    private fun line(x1: Double, y1: Double, x2: Double, y2: Double): Poly = listOf(x1 to y1, x2 to y2)

    /** Cuts corners a little, like a finger does. */
    private fun round(p: Poly): Poly {
        val out = ArrayList<Pair<Double, Double>>()
        out += p.first()
        for (i in 1 until p.size - 1) {
            val c = u(0.0, 0.12)
            val (px, py) = p[i - 1]
            val (x, y) = p[i]
            val (nx, ny) = p[i + 1]
            out += (x + (px - x) * c) to (y + (py - y) * c)
            out += (x + (nx - x) * c) to (y + (ny - y) * c)
        }
        out += p.last()
        return out
    }

    /** Turns strokes in the unit box into noisy, tilted touch samples in pixels. */
    private fun render(strokes: List<Poly>, tilt: Double, aspect: Double?): List<GesturePoint> {
        val size = u(160.0, 520.0)
        val sx = size * (aspect ?: u(0.8, 1.25))
        val sy = size
        val ox = u(0.0, 400.0)
        val oy = u(0.0, 800.0)
        val rot = tilt * PI / 180.0
        val noise = size * u(0.006, 0.02)
        val step = u(6.0, 18.0)

        val out = ArrayList<GesturePoint>()
        strokes.forEachIndexed { id, poly ->
            val dense = densify(poly.map { (x, y) -> (x - 0.5) * sx to (y - 0.5) * sy }, step)
            // low-frequency wobble + per-point jitter
            val wobA = u(0.0, 2 * PI)
            val wobF = u(2.0, 5.0)
            val wobAmp = size * u(0.0, 0.025)
            dense.forEachIndexed { i, (x, y) ->
                val t = i.toDouble() / max(1, dense.size - 1)
                val wx = wobAmp * sin(wobA + wobF * 2 * PI * t)
                val wy = wobAmp * cos(wobA * 1.3 + wobF * 2 * PI * t)
                val jx = x + wx + u(-noise, noise)
                val jy = y + wy + u(-noise, noise)
                val rx = jx * cos(rot) - jy * sin(rot)
                val ry = jx * sin(rot) + jy * cos(rot)
                out += GesturePoint((rx + ox + sx / 2).toFloat(), (ry + oy + sy / 2).toFloat(), id)
            }
        }
        return out
    }

    private fun densify(p: List<Pair<Double, Double>>, step: Double): List<Pair<Double, Double>> {
        val out = ArrayList<Pair<Double, Double>>()
        out += p.first()
        for (i in 1 until p.size) {
            val (x0, y0) = p[i - 1]
            val (x1, y1) = p[i]
            val d = hypot(x1 - x0, y1 - y0)
            val n = max(1, (d / step).toInt())
            for (k in 1..n) {
                val t = k.toDouble() / n
                out += (x0 + (x1 - x0) * t) to (y0 + (y1 - y0) * t)
            }
        }
        return out
    }

}
