package com.yuyan.imemodule.keyboard.gesture

import android.view.MotionEvent

/** Recognizes one discrete action without executing it. */
fun interface GestureRecognizer {
    fun recognize(session: TouchSession, event: MotionEvent, context: GestureContext): GestureAction?
}

/** Ordered recognizers; the first matching action wins. */
class DiscreteGestureDispatcher(
    private val recognizers: List<GestureRecognizer>,
) {
    fun recognize(session: TouchSession, event: MotionEvent, context: GestureContext): GestureAction? {
        if (session.lifecycle != GestureLifecycle.UNDECIDED) return null
        return recognizers.firstNotNullOfOrNull { it.recognize(session, event, context) }
    }
}
