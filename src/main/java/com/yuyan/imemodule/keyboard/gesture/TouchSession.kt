package com.yuyan.imemodule.keyboard.gesture

import android.view.MotionEvent
import com.yuyan.imemodule.entity.keyboard.SoftKey

/** State owned by one primary pointer from DOWN until UP or CANCEL. */
data class TouchSession(
    val pointerId: Int,
    val downX: Float,
    val downY: Float,
    val downTime: Long,
    val downKey: SoftKey?,
    var lastX: Float = downX,
    var lastY: Float = downY,
    var lifecycle: GestureLifecycle = GestureLifecycle.UNDECIDED,
    var abortKey: Boolean = false,
    var longPress: Boolean = false,
    var currentDistanceX: Float = 0f,
    var currentDistanceY: Float = 0f,
) {
    fun coordinates(event: MotionEvent): Pair<Float, Float>? {
        val index = event.findPointerIndex(pointerId)
        if (index < 0) return null
        return event.getX(index) to event.getY(index)
    }
}
