package com.limelight.utils

import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamScreenshotTest {

    @Test
    fun `off gesture never triggers`() {
        var hits = 0
        val detector = ScreenshotGestureDetector(ScreenshotGesture.OFF, density = 2f) { hits++ }
        assertFalse(detector.onMotion(MotionEvent.ACTION_DOWN, 1, 100L, floatArrayOf(100f), floatArrayOf(100f)))
        assertFalse(detector.onMotion(MotionEvent.ACTION_POINTER_DOWN, 2, 120L, floatArrayOf(100f, 200f), floatArrayOf(100f, 100f)))
        assertFalse(detector.onMotion(MotionEvent.ACTION_UP, 1, 180L, floatArrayOf(100f), floatArrayOf(100f)))
        assertEquals(0, hits)
    }

    @Test
    fun `two finger tap triggers on quick two-finger release`() {
        var hits = 0
        val detector = ScreenshotGestureDetector(ScreenshotGesture.TWO_FINGER_TAP, density = 2f) { hits++ }
        assertFalse(detector.onMotion(MotionEvent.ACTION_DOWN, 1, 100L, floatArrayOf(100f), floatArrayOf(100f)))
        assertFalse(detector.onMotion(MotionEvent.ACTION_POINTER_DOWN, 2, 120L, floatArrayOf(100f, 300f), floatArrayOf(100f, 100f)))
        assertTrue(detector.onMotion(MotionEvent.ACTION_UP, 1, 200L, floatArrayOf(100f), floatArrayOf(100f)))
        assertEquals(1, hits)
    }

    @Test
    fun `three finger tap ignores two fingers and fires on three`() {
        var hits = 0
        val detector = ScreenshotGestureDetector(ScreenshotGesture.THREE_FINGER_TAP, density = 2f) { hits++ }
        detector.onMotion(MotionEvent.ACTION_DOWN, 1, 100L, floatArrayOf(100f), floatArrayOf(100f))
        detector.onMotion(MotionEvent.ACTION_POINTER_DOWN, 2, 110L, floatArrayOf(100f, 200f), floatArrayOf(100f, 100f))
        assertFalse(detector.onMotion(MotionEvent.ACTION_UP, 1, 160L, floatArrayOf(100f), floatArrayOf(100f)))
        assertEquals(0, hits)

        detector.onMotion(MotionEvent.ACTION_DOWN, 1, 300L, floatArrayOf(100f), floatArrayOf(100f))
        detector.onMotion(MotionEvent.ACTION_POINTER_DOWN, 2, 310L, floatArrayOf(100f, 200f), floatArrayOf(100f, 100f))
        detector.onMotion(MotionEvent.ACTION_POINTER_DOWN, 3, 320L, floatArrayOf(100f, 200f, 300f), floatArrayOf(100f, 100f, 100f))
        assertTrue(detector.onMotion(MotionEvent.ACTION_UP, 1, 400L, floatArrayOf(100f), floatArrayOf(100f)))
        assertEquals(1, hits)
    }

    @Test
    fun `four finger tap cancels when finger moves too far or times out`() {
        var hits = 0
        val detector = ScreenshotGestureDetector(ScreenshotGesture.FOUR_FINGER_TAP, density = 2f) { hits++ }

        // Moved too far (> 20dp * 2 = 40px)
        detector.onMotion(MotionEvent.ACTION_DOWN, 1, 100L, floatArrayOf(100f), floatArrayOf(100f))
        detector.onMotion(MotionEvent.ACTION_POINTER_DOWN, 2, 110L, floatArrayOf(100f, 200f), floatArrayOf(100f, 100f))
        detector.onMotion(MotionEvent.ACTION_POINTER_DOWN, 3, 115L, floatArrayOf(100f, 200f, 300f), floatArrayOf(100f, 100f, 100f))
        detector.onMotion(MotionEvent.ACTION_POINTER_DOWN, 4, 120L, floatArrayOf(100f, 200f, 300f, 400f), floatArrayOf(100f, 100f, 100f, 100f))
        detector.onMotion(MotionEvent.ACTION_MOVE, 4, 150L, floatArrayOf(200f, 200f, 300f, 400f), floatArrayOf(100f, 100f, 100f, 100f))
        assertFalse(detector.onMotion(MotionEvent.ACTION_UP, 1, 200L, floatArrayOf(200f), floatArrayOf(100f)))
        assertEquals(0, hits)

        // Timeout (> 350ms)
        detector.onMotion(MotionEvent.ACTION_DOWN, 1, 1000L, floatArrayOf(100f), floatArrayOf(100f))
        detector.onMotion(MotionEvent.ACTION_POINTER_DOWN, 2, 1010L, floatArrayOf(100f, 200f), floatArrayOf(100f, 100f))
        detector.onMotion(MotionEvent.ACTION_POINTER_DOWN, 3, 1020L, floatArrayOf(100f, 200f, 300f), floatArrayOf(100f, 100f, 100f))
        detector.onMotion(MotionEvent.ACTION_POINTER_DOWN, 4, 1030L, floatArrayOf(100f, 200f, 300f, 400f), floatArrayOf(100f, 100f, 100f, 100f))
        assertFalse(detector.onMotion(MotionEvent.ACTION_UP, 1, 1500L, floatArrayOf(100f), floatArrayOf(100f)))
        assertEquals(0, hits)
    }

    @Test
    fun `pinch in triggers once when two fingers shrink past threshold`() {
        var hits = 0
        val detector = ScreenshotGestureDetector(ScreenshotGesture.PINCH_IN, density = 2f) { hits++ }
        detector.onMotion(MotionEvent.ACTION_DOWN, 1, 100L, floatArrayOf(100f), floatArrayOf(200f))
        detector.onMotion(MotionEvent.ACTION_POINTER_DOWN, 2, 120L, floatArrayOf(100f, 400f), floatArrayOf(200f, 200f))
        // Still wide (250px vs 300px) -> not yet
        assertFalse(detector.onMotion(MotionEvent.ACTION_MOVE, 2, 140L, floatArrayOf(125f, 375f), floatArrayOf(200f, 200f)))
        // Shrunk to 150px (<= 65% of 300px) -> triggers once
        assertTrue(detector.onMotion(MotionEvent.ACTION_MOVE, 2, 160L, floatArrayOf(175f, 325f), floatArrayOf(200f, 200f)))
        // Further move in same gesture does not re-trigger
        assertFalse(detector.onMotion(MotionEvent.ACTION_MOVE, 2, 180L, floatArrayOf(200f, 300f), floatArrayOf(200f, 200f)))
        assertEquals(1, hits)
    }

    @Test
    fun `gesture step wraps cleanly`() {
        assertEquals(ScreenshotGesture.TWO_FINGER_TAP, ScreenshotGesture.OFF.step(1))
        assertEquals(ScreenshotGesture.PINCH_IN, ScreenshotGesture.OFF.step(-1))
        assertEquals(ScreenshotGesture.OFF, ScreenshotGesture.PINCH_IN.step(1))
    }
}
