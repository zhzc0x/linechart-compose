package com.zhzc0x.compose.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*

@Composable
fun ComposeBaseWindow(
    onCloseRequest: (() -> Unit),
    placement: WindowPlacement = WindowPlacement.Maximized,
    size: DpSize = DpSize(1280.dp, 750.dp),
    visible: Boolean = true,
    title: String = "ComposeBaseWindow",
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean)? = null,
    onKeyEnter: (() -> Unit)? = null,
    onKeyEscape: (() -> Unit)? = null,
    onKeyEvent: ((KeyEvent) -> Boolean)? = null,
    content: @Composable FrameWindowScope.() -> Unit
) = Window(onCloseRequest, rememberWindowState(placement, size = size, position = WindowPosition(Alignment.Center)),
    visible, title, icon, undecorated, transparent, resizable, enabled, focusable, alwaysOnTop,
    onPreviewKeyEvent ?: {
        baseKeyEvent(it, onKeyEnter, onKeyEscape, onCloseRequest)
    }, onKeyEvent ?: {
        false
    }, content
)

@Composable
fun ComposeBaseDialog(
    onCloseRequest: () -> Unit,
    size: DpSize = DpSize(400.dp, 300.dp),
    visible: Boolean = true,
    title: String = "ComposeBaseDialog",
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean)? = null,
    onKeyEnter: (() -> Unit)? = null,
    onKeyEscape: (() -> Unit)? = null,
    onKeyEvent: ((KeyEvent) -> Boolean)? = null,
    content: @Composable DialogWindowScope.() -> Unit
) = Dialog(onCloseRequest, rememberDialogState(size = size), visible, title, icon, undecorated, transparent, resizable,
    enabled, focusable, onPreviewKeyEvent ?: {
        baseKeyEvent(it, onKeyEnter, onKeyEscape, onCloseRequest)
    }, onKeyEvent ?: {
        false
    }, content
)

@OptIn(ExperimentalComposeUiApi::class)
private fun baseKeyEvent(
    keyEvent: KeyEvent, onKeyEnter: (() -> Unit)? = null, onKeyEscape: (() -> Unit)? = null,
    onClose: () -> Unit
): Boolean {
    if (keyEvent.type == KeyEventType.KeyUp) {
        when (keyEvent.key) {
            Key.Enter -> {
                return if(onKeyEnter == null){
                    false
                } else {
                    onKeyEnter.invoke()
                    true
                }
            }
            Key.Escape -> {
                onKeyEscape?.invoke() ?: onClose()
                return true
            }
        }
    }
    return false
}