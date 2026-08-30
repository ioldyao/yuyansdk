package com.yuyan.imemodule.service

import android.content.res.Configuration
import android.graphics.Rect
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.text.InputType
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.yuyan.imemodule.candidate.CandidateView
import com.yuyan.imemodule.data.emojicon.YuyanEmojiCompat
import com.yuyan.imemodule.data.theme.Theme
import com.yuyan.imemodule.data.theme.ThemeManager.OnThemeChangeListener
import com.yuyan.imemodule.data.theme.ThemeManager.addOnChangedListener
import com.yuyan.imemodule.data.theme.ThemeManager.onSystemDarkModeChange
import com.yuyan.imemodule.data.theme.ThemeManager.removeOnChangedListener
import com.yuyan.imemodule.keyboard.InputView
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.keyboard.container.ClipBoardContainer
import com.yuyan.imemodule.prefs.AppPrefs.Companion.getInstance
import com.yuyan.imemodule.prefs.behavior.SkbMenuMode
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.utils.KeyboardLoaderUtil
import com.yuyan.imemodule.utils.StringUtils
import com.yuyan.imemodule.utils.isDarkMode
import com.yuyan.imemodule.view.preference.ManagedPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import splitties.bitflags.hasFlag

/**
 * Main class of the Pinyin input method. 输入法服务
 */
class ImeService : InputMethodService() {
    private var isHardwareKeyboard = false
    private var isSoftKeyboard = false
    private lateinit var mInputView: InputView
    private lateinit var mCandidateView: CandidateView
    private val onThemeChangeListener = OnThemeChangeListener { _: Theme? -> if (isHardwareKeyboard) mCandidateView.updateTheme() else mInputView.updateTheme()}
    private val clipboardUpdateContent = getInstance().internal.clipboardUpdateContent
    private val clipboardUpdateContentListener = ManagedPreference.OnChangeListener<String> { _, value ->
        if(isSoftKeyboard && getInstance().clipboard.clipboardSuggestion.getValue()){
            if(value.isNotBlank()) {
                if(KeyboardManager.instance.currentContainer is ClipBoardContainer
                    && (KeyboardManager.instance.currentContainer as ClipBoardContainer).getMenuMode() == SkbMenuMode.ClipBoard ){
                    (KeyboardManager.instance.currentContainer as ClipBoardContainer).showClipBoardView(SkbMenuMode.ClipBoard)
                } else {
                    mInputView.showSymbols(arrayOf(value))
                }
            }
        }
    }
    override fun onCreate() {
        super.onCreate()
        addOnChangedListener(onThemeChangeListener)
        clipboardUpdateContent.registerOnChangeListener(clipboardUpdateContentListener)
    }

    override fun onCreateInputView(): View {
        mInputView = InputView(baseContext, this)
        return mInputView
    }

    override fun onCreateCandidatesView(): View {
        mCandidateView = CandidateView(baseContext, this)
        return mCandidateView
    }

    override fun onEvaluateInputViewShown(): Boolean {
        return if(getInstance().keyboardSetting.showVirtualKeyboardOnPhysicalKeyboard.getValue()) true else super.onEvaluateInputViewShown()
    }

    override fun onStartInput(editorInfo: EditorInfo?, restarting: Boolean) {
        YuyanEmojiCompat.setEditorInfo(editorInfo)
        handleHardwareKeyboard()
        if (isHardwareKeyboard)mCandidateView.onStartInput(editorInfo, restarting)
        super.onStartInput(editorInfo, restarting)
    }

    override fun onStartInputView(editorInfo: EditorInfo, restarting: Boolean) {
        if (isSoftKeyboard)mInputView.onStartInputView(editorInfo, restarting)
        super.onStartInputView(editorInfo, restarting)
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOnChangedListener(onThemeChangeListener)
        clipboardUpdateContent.unregisterOnChangeListener(clipboardUpdateContentListener)
    }

    /**
     * 横竖屏切换
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleHardwareKeyboard(newConfig)
        CoroutineScope(Dispatchers.Main).launch {
            delay(200) //延时，解决获取屏幕尺寸不准确。
            EnvironmentSingleton.instance.initData(baseContext)
            if (isSoftKeyboard) {
                KeyboardLoaderUtil.instance.clearKeyboardMap()
                KeyboardManager.instance.clearKeyboard()
                KeyboardManager.instance.switchKeyboard()
            } else if(isHardwareKeyboard){
                mCandidateView.initView()
            }
        }
        onSystemDarkModeChange(newConfig.isDarkMode())
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Keep BACK handling in InputMethodService so its down/up tracking remains intact.
        if (keyCode == KeyEvent.KEYCODE_BACK) return super.onKeyDown(keyCode, event)

        // Long-presses and modifier combinations are handled by the framework.
        return if (event.repeatCount != 0 || event.isShiftPressed || event.isMetaPressed) {
            super.onKeyDown(keyCode, event)
        } else if (event.isCtrlPressed && keyCode != KeyEvent.KEYCODE_SPACE) {
            super.onKeyDown(keyCode, event)
        } else if (isSoftKeyboard && ::mInputView.isInitialized) {
            mInputView.processKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
        } else if (isHardwareKeyboard && ::mCandidateView.isInitialized) {
            mCandidateView.processKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return super.onKeyUp(keyCode, event)

        return if (event.repeatCount != 0 || event.isShiftPressed || event.isMetaPressed) {
            super.onKeyUp(keyCode, event)
        } else if (event.isCtrlPressed && keyCode != KeyEvent.KEYCODE_SPACE) {
            super.onKeyUp(keyCode, event)
        } else if (isSoftKeyboard && ::mInputView.isInitialized) {
            mInputView.processKeyUp(event) || super.onKeyUp(keyCode, event)
        } else if (isHardwareKeyboard && ::mCandidateView.isInitialized) {
            mCandidateView.processKeyUp(event) || super.onKeyUp(keyCode, event)
        } else {
            super.onKeyUp(keyCode, event)
        }
    }

    override fun setInputView(view: View) {
        super.setInputView(view)
        val layoutParams = view.layoutParams
        if (layoutParams != null && layoutParams.height != ViewGroup.LayoutParams.MATCH_PARENT) {
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            view.setLayoutParams(layoutParams)
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false //修复横屏之后输入框遮挡问题


    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)

        if (isSoftKeyboard && isInputViewShown && ::mInputView.isInitialized) {
            val inputRoot = mInputView.mSkbRoot
            if (!inputRoot.isAttachedToWindow || !inputRoot.isLaidOut || inputRoot.width <= 0 || inputRoot.height <= 0) return

            val location = IntArray(2)
            if (mInputView.isAddPhrases) {
                mInputView.mAddPhrasesLayout.getLocationInWindow(location)
            } else {
                inputRoot.getLocationInWindow(location)
            }
            if (EnvironmentSingleton.instance.keyboardModeFloat) {
                val rootLocation = IntArray(2)
                inputRoot.getLocationInWindow(rootLocation)
                val region = Rect(
                    rootLocation[0],
                    rootLocation[1],
                    rootLocation[0] + inputRoot.width,
                    rootLocation[1] + inputRoot.height
                )
                if (mInputView.isAddPhrases) {
                    val phrasesView = mInputView.mAddPhrasesLayout
                    if (phrasesView.isAttachedToWindow && phrasesView.isLaidOut && phrasesView.width > 0 && phrasesView.height > 0) {
                        val phrasesLocation = IntArray(2)
                        phrasesView.getLocationInWindow(phrasesLocation)
                        region.union(
                            phrasesLocation[0],
                            phrasesLocation[1],
                            phrasesLocation[0] + phrasesView.width,
                            phrasesLocation[1] + phrasesView.height
                        )
                    }
                }
                outInsets.apply {
                    contentTopInsets = EnvironmentSingleton.instance.mScreenHeight
                    visibleTopInsets = EnvironmentSingleton.instance.mScreenHeight
                    touchableInsets = Insets.TOUCHABLE_INSETS_REGION
                    touchableRegion.set(region)
                }
            } else {
                outInsets.apply {
                    contentTopInsets = location[1]
                    visibleTopInsets = location[1]
                    touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
                    touchableRegion.setEmpty()
                }
            }
        } else if (isHardwareKeyboard && ::mCandidateView.isInitialized) {
            val candidateRoot = mCandidateView.mSkbRoot
            if (!candidateRoot.isAttachedToWindow || !candidateRoot.isLaidOut || candidateRoot.width <= 0 || candidateRoot.height <= 0) return

            val location = IntArray(2)
            candidateRoot.getLocationInWindow(location)
            outInsets.apply {
                contentTopInsets = EnvironmentSingleton.instance.mScreenHeight
                visibleTopInsets = EnvironmentSingleton.instance.mScreenHeight
                touchableInsets = Insets.TOUCHABLE_INSETS_REGION
                touchableRegion.set(
                    location[0],
                    location[1],
                    location[0] + candidateRoot.width,
                    location[1] + candidateRoot.height
                )
            }
        }
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (isSoftKeyboard) mInputView.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesEnd)
    }

    private val cursorAnchorPosition = FloatArray(2)
    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo?) {
        super.onUpdateCursorAnchorInfo(cursorAnchorInfo)
        if (!isHardwareKeyboard || cursorAnchorInfo == null) return
        cursorAnchorPosition[0] = cursorAnchorInfo.insertionMarkerHorizontal
        cursorAnchorPosition[1] = cursorAnchorInfo.insertionMarkerBottom
        val matrix = cursorAnchorInfo.getMatrix()
        if (matrix != null) {
            matrix.mapPoints(cursorAnchorPosition)
        }
        mCandidateView.updatePosition(cursorAnchorPosition)
    }

    override fun onWindowShown() {
        if (isSoftKeyboard) mInputView.onWindowShown()
        super.onWindowShown()
    }

    override fun onWindowHidden() {
        if(isSoftKeyboard) mInputView.onWindowHidden()
        super.onWindowHidden()
    }

    /**
     * 模拟Enter按键点击
     */
    fun sendEnterKeyEvent() {
        val inputConnection = getCurrentInputConnection()
        YuyanEmojiCompat.mEditorInfo?.run {
            if (inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_NULL || imeOptions.hasFlag(EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            } else if (!actionLabel.isNullOrEmpty() && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                inputConnection.performEditorAction(actionId)
            } else when (val action = imeOptions and EditorInfo.IME_MASK_ACTION) {
                EditorInfo.IME_ACTION_UNSPECIFIED, EditorInfo.IME_ACTION_NONE -> sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                else -> inputConnection.performEditorAction(action)
            }
        }
    }

    fun sendCombinationKeyEvents(keyEventCode: Int, alt: Boolean = false, ctrl: Boolean = false, shift: Boolean = false) {
        var metaState = 0
        if (alt) metaState = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (ctrl) metaState = metaState or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (shift) metaState = metaState or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        val eventTime = SystemClock.uptimeMillis()
        if (alt) sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_ALT_LEFT)
        if (ctrl) sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_CTRL_LEFT)
        if (shift) sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_SHIFT_LEFT)
        sendDownKeyEvent(eventTime, keyEventCode, metaState)
        sendUpKeyEvent(eventTime, keyEventCode, metaState)
        if (shift) sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_SHIFT_LEFT)
        if (ctrl) sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_CTRL_LEFT)
        if (alt) sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_ALT_LEFT)
    }

    fun sendDownKeyEvent(eventTime: Long, keyEventCode: Int, metaState: Int = 0) {
        currentInputConnection?.sendKeyEvent(
            KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyEventCode, 0, metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD, keyEventCode, KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE)
        )
    }

    fun sendUpKeyEvent(eventTime: Long, keyEventCode: Int, metaState: Int = 0) {
        currentInputConnection.sendKeyEvent(
            KeyEvent(eventTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyEventCode, 0, metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD, keyEventCode, KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE)
        )
    }

    /**
     * 向输入框提交预选词
     */
    fun setComposingText(text: CharSequence) {
        currentInputConnection.setComposingText(text, 1)
    }


    /**
     * 结束提交预选词
     */
    fun finishComposingText() {
        currentInputConnection.finishComposingText()
    }

    /**
     * 发送字符串给编辑框
     */
    fun commitText(text: String) {
        currentInputConnection.commitText(StringUtils.converted2FlowerTypeface(text), 1)
    }

    /**
     * 发送字符串给编辑框
     */
    fun commitText(text: String, newCursorPosition: Int) {
        currentInputConnection.commitText(StringUtils.converted2FlowerTypeface(text), newCursorPosition)
    }

    fun getTextBeforeCursor(length:Int) : String {
        return currentInputConnection.getTextBeforeCursor(length, 0).toString()
    }

    fun commitTextEditMenu(id:Int) {
        currentInputConnection.performContextMenuAction(id)
    }

    fun performEditorAction(editorAction:Int) {
        currentInputConnection.performEditorAction(editorAction)
    }

    fun deleteSurroundingText(length:Int) {
        currentInputConnection.deleteSurroundingText(length, 0)
    }

    fun setSelection(start: Int, end: Int) {
        currentInputConnection.setSelection(start, end)
    }

    fun handleHardwareKeyboard(newConfig: Configuration? = null) {
        val hardwareKeyboard = if (getInstance().keyboardSetting.showVirtualKeyboardOnPhysicalKeyboard.getValue()) false
            else if (newConfig != null) (newConfig.keyboard != Configuration.KEYBOARD_NOKEYS)
            else resources.configuration.keyboard != Configuration.KEYBOARD_NOKEYS
        isSoftKeyboard = !hardwareKeyboard
        isHardwareKeyboard = hardwareKeyboard
        setCandidatesViewShown(isHardwareKeyboard)
        currentInputConnection.requestCursorUpdates(if(isHardwareKeyboard)InputConnection.CURSOR_UPDATE_MONITOR else 0)
    }

}
