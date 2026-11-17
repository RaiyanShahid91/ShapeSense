package com.raiyanshahid.shapesense

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class ShapeRecognizerTest {

    private val recognizer = ShapeRecognizer()

    private fun top(points: List<GesturePoint>) = recognizer.rank(points).firstOrNull()

    /** Drawings come from a seeded simulator, so these tests are deterministic. */
    @Test
    fun recognizesEveryBuiltInShapeMostOfTheTime() {
        val sim = GestureSimulator(seed = 2026)
        var total = 0
        var correct = 0
        for (shape in Shapes.all) {
            var shapeCorrect = 0
            repeat(150) {
                total++
                if (top(sim.draw(shape))?.name == shape) {
                    correct++
                    shapeCorrect++
                }
            }
            assertTrue("$shape recognised only $shapeCorrect/150 times", shapeCorrect >= 120)
        }
        val accuracy = correct.toDouble() / total
        assertTrue("overall accuracy $accuracy", accuracy >= 0.95)
    }

    @Test
    fun rejectsScribblesLinesAndLetters() {
        val sim = GestureSimulator(seed = 99)
        var accepted = 0
        repeat(600) {
            val match = recognizer.recognize(sim.junk(it), minScore = ShapeGestureDefaults.MIN_SCORE)
            if (match != null) accepted++
        }
        assertTrue("$accepted of 600 junk gestures were accepted", accepted <= 12)
    }

    @Test
    fun arrowDirectionMatters() {
        val sim = GestureSimulator(seed = 5)
        repeat(100) {
            assertEquals(Shapes.ARROW_UP, top(sim.draw(Shapes.ARROW_UP))?.name)
            assertEquals(Shapes.ARROW_LEFT, top(sim.draw(Shapes.ARROW_LEFT))?.name)
        }
    }

    @Test
    fun sizeAndPositionDoNotMatter() {
        val circle = circlePoints(radius = 50f)
        val moved = circle.map { GesturePoint(it.x * 7f + 1000f, it.y * 7f - 300f, it.strokeId) }
        assertEquals(Shapes.CIRCLE, top(circle)?.name)
        assertEquals(Shapes.CIRCLE, top(moved)?.name)
        assertEquals(top(circle)!!.score, top(moved)!!.score, 0.01f)
    }

    @Test
    fun startPointAndDirectionDoNotMatterForClosedShapes() {
        val clockwise = circlePoints(radius = 100f)
        val counterClockwise = clockwise.reversed()
        val rotatedStart = clockwise.drop(17) + clockwise.take(17)
        for (pts in listOf(clockwise, counterClockwise, rotatedStart)) {
            assertEquals(Shapes.CIRCLE, top(pts)?.name)
        }
    }

    @Test
    fun multiStrokeCrossIsRecognised() {
        val a = (0..20).map { GesturePoint(it * 5f, it * 5f, strokeId = 0) }
        val b = (0..20).map { GesturePoint(100f - it * 5f, it * 5f, strokeId = 1) }
        assertEquals(Shapes.CROSS, top(a + b)?.name)
        // order and stroke direction are irrelevant
        assertEquals(Shapes.CROSS, top(b.reversed().map { it.copy(strokeId = 0) } + a.reversed().map { it.copy(strokeId = 1) })?.name)
    }

    @Test
    fun allowedShapesRestrictTheCandidates() {
        val pts = circlePoints(radius = 80f)
        val ranked = recognizer.rank(pts, allowed = setOf(Shapes.CHECK, Shapes.CROSS))
        assertEquals(setOf(Shapes.CHECK, Shapes.CROSS), ranked.map { it.name }.toSet())
    }

    @Test
    fun minScoreAndMarginCanRejectAmbiguousDrawings() {
        val pts = circlePoints(radius = 80f)
        assertNotNull(recognizer.recognize(pts, minScore = 0.5f))
        assertNull(recognizer.recognize(pts, minScore = 1.01f))
        assertNull(recognizer.recognize(pts, minMargin = 5f))
    }

    @Test
    fun degenerateInputNeverCrashes() {
        assertNull(recognizer.recognize(emptyList()))
        assertNull(recognizer.recognize(listOf(GesturePoint(1f, 1f))))
        assertNull(recognizer.recognize(List(10) { GesturePoint(5f, 5f) }))
        assertTrue(recognizer.rank(List(3) { GesturePoint(it.toFloat(), 0f) }).isNotEmpty())
        // a perfectly straight horizontal line has zero height
        assertNotNull(recognizer.rank((0..30).map { GesturePoint(it * 4f, 10f) }).firstOrNull())
    }

    @Test
    fun customTemplatesAreLearned() {
        // an "L" shape
        val l = ShapeTemplate.ofStrokes("L", listOf(listOf(0f to 0f, 0f to 100f, 70f to 100f)))
        val custom = recognizer + l
        val drawn = (0..20).map { GesturePoint(200f, 300f + it * 5f) } + (1..14).map { GesturePoint(200f + it * 5f, 400f) }
        assertEquals("L", custom.recognize(drawn)?.name)
        assertTrue("L" in custom.shapeNames)
        // the original recognizer is unchanged
        assertTrue("L" !in recognizer.shapeNames)
    }

    @Test
    fun strictModeStillRecognisesUprightShapes() {
        val strict = ShapeRecognizer(tiltToleranceDegrees = 0f)
        assertEquals(Shapes.CIRCLE, strict.recognize(circlePoints(60f))?.name)
    }

    private fun circlePoints(radius: Float, n: Int = 60): List<GesturePoint> =
        (0..n).map {
            val a = 2.0 * Math.PI * it / n
            GesturePoint(200f + radius * cos(a).toFloat(), 200f + radius * sin(a).toFloat())
        }
}
