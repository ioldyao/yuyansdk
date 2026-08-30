package com.yuyan.imemodule.keyboard.gesture

import android.view.KeyEvent
import android.view.MotionEvent

/** Performs one cursor step for the legacy space/zero horizontal swipe. */
class SpaceCursorSwipeRecognizer : GestureRecognizer {
    override fun recognize(session: TouchSession, event: MotionEvent, context: GestureContext): GestureAction? {
        if (context.keyCode !in setOf(KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_0)) return null
        val (x, y) = session.coordinates(event) ?: return null
        val dx = x - session.downX
        val dy = y - session.downY
        if (!GestureDirection.isHorizontalSwipe(dx, dy, context.cursorThreshold)) return null
        return GestureAction.KeyEvent(
            if (dx > 0f) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        )
    }
}
