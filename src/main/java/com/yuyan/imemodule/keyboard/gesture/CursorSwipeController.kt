package com.yuyan.imemodule.keyboard.gesture

import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs

/** Owns the only gesture that may remain active across MOVE events. */
class CursorSwipeController(
    private val leftKeys: Set<Int>,
    private val rightKeys: Set<Int>,
    private val stepPx: () -> Float,
    private val holdDelayMs: Long = 300L,
    private val repeatIntervalMs: Long = 120L,
    private val emit: (GestureAction) -> Unit,
) : ContinuousController {
    private var started = false
    private var consumed = false
    private var direction = 0
    private var startX = 0f
    private var lastX = 0f
    private var startTime = 0L
    private var lastDispatchTime = 0L

    override fun reset() {
        started = false
        consumed = false
        direction = 0
        startX = 0f
        lastX = 0f
        startTime = 0L
        lastDispatchTime = 0L
    }

    override fun canStart(session: TouchSession, event: MotionEvent, context: GestureContext): Boolean {
        if (started || context.keyCode == null || !context.cursorKeyEligible) return false
        val (x, y) = session.coordinates(event) ?: return false
        val dx = x - session.downX
        val dy = y - session.downY
        return GestureDirection.isHorizontalSwipe(dx, dy, context.cursorThreshold) &&
            directionFor(context.keyCode, dx) != null
    }

    override fun start(session: TouchSession, event: MotionEvent, context: GestureContext) {
        val (x, _) = session.coordinates(event) ?: return
        started = true
        consumed = false
        startX = session.downX
        lastX = x
        startTime = event.eventTime
        lastDispatchTime = 0L
        direction = 0
        update(session, event, context)
    }

    override fun update(session: TouchSession, event: MotionEvent, context: GestureContext) {
        if (!started) return
        val (x, _) = session.coordinates(event) ?: return
        val dx = x - startX
        val nextDirection = directionFor(context.keyCode, dx) ?: return
        if (!consumed) {
            if (abs(dx) < stepPx()) return
            emit(GestureAction.KeyEvent(nextDirection))
            consumed = true
            direction = nextDirection
            lastX = x
            lastDispatchTime = event.eventTime
            return
        }
        if (nextDirection != direction) {
            direction = nextDirection
            lastX = x
            return
        }
        if (event.eventTime - startTime < holdDelayMs ||
            event.eventTime - lastDispatchTime < repeatIntervalMs ||
            abs(x - lastX) < stepPx()
        ) return
        emit(GestureAction.KeyEvent(nextDirection))
        lastX = x
        lastDispatchTime = event.eventTime
    }

    override fun finish() = reset()
    override fun cancel() = reset()

    private fun directionFor(keyCode: Int?, dx: Float): Int? {
        if (keyCode == null || dx == 0f) return null
        val direction = if (dx > 0f) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        return if ((direction == KeyEvent.KEYCODE_DPAD_LEFT && keyCode in leftKeys) ||
            (direction == KeyEvent.KEYCODE_DPAD_RIGHT && keyCode in rightKeys)
        ) direction else null
    }
}
