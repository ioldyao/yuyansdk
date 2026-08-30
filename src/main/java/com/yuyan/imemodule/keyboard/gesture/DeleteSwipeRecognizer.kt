package com.yuyan.imemodule.keyboard.gesture

import android.view.KeyEvent
import android.view.MotionEvent

/** Emits one clear action for a left swipe on Delete. */
class DeleteSwipeRecognizer : GestureRecognizer {
    override fun recognize(session: TouchSession, event: MotionEvent, context: GestureContext): GestureAction? {
        if (context.keyCode != KeyEvent.KEYCODE_DEL) return null
        val (x, y) = session.coordinates(event) ?: return null
        val dx = x - session.downX
        val dy = y - session.downY
        return GestureAction.KeyEvent(KeyEvent.KEYCODE_CLEAR).takeIf {
            dx < -context.deleteThreshold &&
                GestureDirection.isHorizontalSwipe(dx, dy, context.deleteThreshold)
        }
    }
}
