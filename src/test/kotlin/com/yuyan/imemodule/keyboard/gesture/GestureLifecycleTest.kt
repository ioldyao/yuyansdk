package com.yuyan.imemodule.keyboard.gesture

import kotlin.test.Test
import kotlin.test.assertEquals

class GestureLifecycleTest {
    @Test
    fun `a discrete session remains locked`() {
        val session = TouchSession(0, 0f, 0f, 0L, null)
        session.lifecycle = GestureLifecycle.DISCRETE_CONSUMED
        assertEquals(GestureLifecycle.DISCRETE_CONSUMED, session.lifecycle)
    }

    @Test
    fun `new session starts undecided`() {
        assertEquals(
            GestureLifecycle.UNDECIDED,
            TouchSession(0, 0f, 0f, 0L, null).lifecycle,
        )
    }
}
