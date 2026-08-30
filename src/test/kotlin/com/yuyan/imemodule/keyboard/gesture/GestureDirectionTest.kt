package com.yuyan.imemodule.keyboard.gesture

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GestureDirectionTest {
    @Test
    fun `directions require strict threshold and dominant axis`() {
        assertTrue(GestureDirection.isUpSwipe(1f, -11f, 10f))
        assertTrue(GestureDirection.isDownSwipe(1f, 11f, 10f))
        assertTrue(GestureDirection.isHorizontalSwipe(11f, 1f, 10f))
        assertFalse(GestureDirection.isUpSwipe(0f, -10f, 10f))
        assertFalse(GestureDirection.isDownSwipe(11f, 12f, 10f))
        assertFalse(GestureDirection.isHorizontalSwipe(11f, 12f, 10f))
    }
}
