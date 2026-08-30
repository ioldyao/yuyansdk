package com.yuyan.imemodule.keyboard.gesture

/** A side-effect-free result produced by a gesture recognizer. */
sealed interface GestureAction {
    data class UserCommand(val code: Int) : GestureAction
    data class KeyEvent(val code: Int) : GestureAction
    data class PopupText(val text: String) : GestureAction
    data class PopupMenu(val distanceY: Float) : GestureAction
    data object ToggleLX17English : GestureAction
}

/** Immutable state made available to recognizers for one dispatch pass. */
data class GestureContext(
    val layout: Int,
    val lx17Layout: Int,
    val englishLayout: Int,
    val isChinese: Boolean,
    val isEnglish: Boolean,
    val symbolEnabled: Boolean,
    val symbolThreshold: Float,
    val cursorThreshold: Float,
    val deleteThreshold: Float,
    val cursorKeyEligible: Boolean,
    val longPress: Boolean,
    val keyCode: Int?,
    val spaceKeyCode: Int,
    val editCommands: Map<Int, Int>,
    val smallLabel: String?,
    val keyLabel: String = "",
)

fun interface GestureActionExecutor {
    fun execute(action: GestureAction)
}
