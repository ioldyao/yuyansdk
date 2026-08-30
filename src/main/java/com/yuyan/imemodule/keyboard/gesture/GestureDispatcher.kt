package com.yuyan.imemodule.keyboard.gesture

import android.view.MotionEvent

/** Coordinates discrete recognizers and the single continuous cursor controller. */
class GestureDispatcher(
    private val discrete: DiscreteGestureDispatcher,
    private val cursor: CursorSwipeController,
    private val execute: GestureActionExecutor,
) {
    fun reset() = cursor.reset()

    fun dispatch(session: TouchSession, event: MotionEvent, context: GestureContext): Boolean {
        when (session.lifecycle) {
            GestureLifecycle.DISCRETE_CONSUMED -> return true
            GestureLifecycle.CONTINUOUS -> {
                cursor.update(session, event, context)
                return true
            }
            GestureLifecycle.UNDECIDED -> Unit
        }

        val action = discrete.recognize(session, event, context)
        if (action != null) {
            execute.execute(action)
            session.lifecycle = GestureLifecycle.DISCRETE_CONSUMED
            session.abortKey = true
            return true
        }

        if (cursor.canStart(session, event, context)) {
            session.lifecycle = GestureLifecycle.CONTINUOUS
            cursor.start(session, event, context)
            session.abortKey = true
            return true
        }
        return false
    }

    fun finish() = cursor.finish()
    fun cancel() = cursor.cancel()
}
