package com.yuyan.imemodule.keyboard.gesture

import kotlin.math.abs

/** Shared coordinate predicates; recognizers remain responsible for their own key/layout rules. */
object GestureDirection {
    fun isUpSwipe(dx: Float, dy: Float, threshold: Float): Boolean =
        dy < -threshold && abs(dx) * 1.5f < abs(dy)

    fun isDownSwipe(dx: Float, dy: Float, threshold: Float): Boolean =
        dy > threshold && abs(dx) * 1.5f < abs(dy)

    fun isHorizontalSwipe(dx: Float, dy: Float, threshold: Float): Boolean =
        abs(dx) > threshold && abs(dx) > abs(dy)
}
