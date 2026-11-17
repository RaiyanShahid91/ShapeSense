package com.raiyanshahid.shapesense

import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** A gesture resampled to a fixed number of points, scaled to a unit box and centred on the origin. */
internal class PointCloud(val xs: DoubleArray, val ys: DoubleArray) {
    val size: Int get() = xs.size
}

/**
 * The "$P" point-cloud recognizer (Vatavu, Anthony & Wobbrock, 2012).
 *
 * It treats a gesture as an unordered cloud of points, so it ignores stroke order, stroke direction
 * and stroke count. It is *not* rotation invariant, which is what we want: an arrow pointing up must
 * not match an arrow pointing right.
 */
internal object PointCloudMath {

    private const val EPSILON = 0.5

    private class P(val x: Double, val y: Double, val id: Int)

    fun normalize(points: List<GesturePoint>, n: Int): PointCloud? {
        if (points.size < 2) return null
        val raw = points.map { P(it.x.toDouble(), it.y.toDouble(), it.strokeId) }
        val total = pathLength(raw)
        if (total < 1e-6) return null

        val resampled = resample(raw, n, total)

        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        for (p in resampled) {
            minX = min(minX, p.x); maxX = max(maxX, p.x)
            minY = min(minY, p.y); maxY = max(maxY, p.y)
        }
        // Uniform scale: keeps the aspect ratio, so a wide rectangle stays different from a square.
        val size = max(maxX - minX, maxY - minY)
        if (size < 1e-9) return null

        val xs = DoubleArray(n)
        val ys = DoubleArray(n)
        var cx = 0.0
        var cy = 0.0
        for (i in 0 until n) {
            xs[i] = (resampled[i].x - minX) / size
            ys[i] = (resampled[i].y - minY) / size
            cx += xs[i]
            cy += ys[i]
        }
        cx /= n
        cy /= n
        for (i in 0 until n) {
            xs[i] -= cx
            ys[i] -= cy
        }
        return PointCloud(xs, ys)
    }

    /** Rotates [points] by [degrees] around their bounding-box centre (screen coordinates, clockwise). */
    fun rotated(points: List<GesturePoint>, degrees: Float): List<GesturePoint> {
        if (degrees == 0f || points.isEmpty()) return points
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        val cx = (minX + maxX) / 2.0
        val cy = (minY + maxY) / 2.0
        val r = Math.toRadians(degrees.toDouble())
        val c = kotlin.math.cos(r)
        val s = kotlin.math.sin(r)
        return points.map {
            val dx = it.x - cx
            val dy = it.y - cy
            GesturePoint((cx + dx * c - dy * s).toFloat(), (cy + dx * s + dy * c).toFloat(), it.strokeId)
        }
    }

    /** Lower is better. Compares both directions and several start points (greedy cloud match). */
    fun distance(a: PointCloud, b: PointCloud): Double {
        val n = a.size
        val step = floor(n.toDouble().pow(1.0 - EPSILON)).toInt().coerceAtLeast(1)
        val matched = BooleanArray(n)
        var best = Double.MAX_VALUE
        var i = 0
        while (i < n) {
            best = min(best, cloudDistance(a, b, i, matched))
            best = min(best, cloudDistance(b, a, i, matched))
            i += step
        }
        return best
    }

    /** Maps a [distance] to a 0..1 score. */
    fun score(distance: Double): Float = max((2.0 - distance) / 2.0, 0.0).toFloat()

    private fun cloudDistance(p: PointCloud, t: PointCloud, start: Int, matched: BooleanArray): Double {
        val n = p.size
        matched.fill(false)
        var sum = 0.0
        var i = start
        do {
            var index = -1
            var minSq = Double.MAX_VALUE
            for (j in 0 until n) {
                if (matched[j]) continue
                val dx = p.xs[i] - t.xs[j]
                val dy = p.ys[i] - t.ys[j]
                val sq = dx * dx + dy * dy
                if (sq < minSq) {
                    minSq = sq
                    index = j
                }
            }
            matched[index] = true
            val weight = 1.0 - ((i - start + n) % n).toDouble() / n
            sum += weight * sqrt(minSq)
            i = (i + 1) % n
        } while (i != start)
        return sum
    }

    private fun pathLength(points: List<P>): Double {
        var d = 0.0
        for (i in 1 until points.size) {
            if (points[i].id == points[i - 1].id) {
                d += hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
            }
        }
        return d
    }

    private fun resample(points: List<P>, n: Int, total: Double): List<P> {
        val interval = total / (n - 1)
        val pts = points.toMutableList()
        val out = ArrayList<P>(n)
        out += pts[0]
        var acc = 0.0
        var i = 1
        while (i < pts.size) {
            val prev = pts[i - 1]
            val cur = pts[i]
            if (cur.id == prev.id) {
                val d = hypot(cur.x - prev.x, cur.y - prev.y)
                if (d > 0.0 && acc + d >= interval) {
                    val t = (interval - acc) / d
                    val q = P(prev.x + t * (cur.x - prev.x), prev.y + t * (cur.y - prev.y), cur.id)
                    out += q
                    pts.add(i, q) // q becomes the new "previous" point on the next iteration
                    acc = 0.0
                } else {
                    acc += d
                }
            }
            i++
        }
        // Floating point rounding can leave us one point short.
        while (out.size < n) out += pts.last()
        return if (out.size > n) out.subList(0, n) else out
    }
}
