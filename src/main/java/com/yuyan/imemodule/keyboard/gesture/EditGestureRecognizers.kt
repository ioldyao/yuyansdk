package com.yuyan.imemodule.keyboard.gesture

import android.view.KeyEvent
import android.view.MotionEvent

private fun swipeDelta(session: TouchSession, event: MotionEvent): Pair<Float, Float> =
    (event.x - session.downX) to (event.y - session.downY)

class LX17EditSwipeRecognizer : GestureRecognizer {
    override fun recognize(session: TouchSession, event: MotionEvent, context: GestureContext): GestureAction? {
        if (context.layout != context.lx17Layout) return null
        val command = context.editCommands[context.keyCode] ?: return null
        val (dx, dy) = swipeDelta(session, event)
        return GestureAction.UserCommand(command).takeIf {
            GestureDirection.isDownSwipe(dx, dy, context.symbolThreshold)
        }
    }
}

class EnglishEditSwipeRecognizer : GestureRecognizer {
    override fun recognize(session: TouchSession, event: MotionEvent, context: GestureContext): GestureAction? {
        if (context.layout != context.englishLayout || !context.isEnglish) return null
        val command = context.editCommands[context.keyCode] ?: return null
        val (dx, dy) = swipeDelta(session, event)
        return GestureAction.UserCommand(command).takeIf {
            GestureDirection.isDownSwipe(dx, dy, context.symbolThreshold)
        }
    }
}

class LX17SpaceUpRecognizer : GestureRecognizer {
    override fun recognize(session: TouchSession, event: MotionEvent, context: GestureContext): GestureAction? {
        if (context.layout != context.lx17Layout || !context.isChinese) return null
        if (context.keyCode != context.spaceKeyCode) return null
        val (dx, dy) = swipeDelta(session, event)
        return GestureAction.ToggleLX17English.takeIf {
            GestureDirection.isUpSwipe(dx, dy, context.symbolThreshold)
        }
    }
}

class KeySymbolUpRecognizer : GestureRecognizer {
    override fun recognize(session: TouchSession, event: MotionEvent, context: GestureContext): GestureAction? {
        val symbol = context.smallLabel?.takeIf { context.symbolEnabled && it.isNotBlank() } ?: return null
        val (dx, dy) = swipeDelta(session, event)
        return GestureAction.PopupText(symbol).takeIf {
            GestureDirection.isUpSwipe(dx, dy, context.symbolThreshold)
        }
    }
}
