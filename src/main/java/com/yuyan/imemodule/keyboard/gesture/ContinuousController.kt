package com.yuyan.imemodule.keyboard.gesture

import android.view.MotionEvent

/** Small lifecycle contract for a continuous gesture controller. */
interface ContinuousController {
    fun reset()
    fun canStart(session: TouchSession, event: MotionEvent, context: GestureContext): Boolean
    fun start(session: TouchSession, event: MotionEvent, context: GestureContext)
    fun update(session: TouchSession, event: MotionEvent, context: GestureContext)
    fun finish()
    fun cancel()
}
