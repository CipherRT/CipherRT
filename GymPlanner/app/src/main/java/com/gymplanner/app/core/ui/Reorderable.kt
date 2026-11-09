package com.gymplanner.app.core.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * A short list whose rows can be dragged into a new order using a handle.
 * [content] receives a Modifier to put on the drag handle. [onReorder] is called once,
 * with the new order, when the finger lifts.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    onReorder: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (item: T, handle: Modifier, isDragging: Boolean, index: Int) -> Unit,
) {
    // While dragging (and until the saved order comes back), show our local copy.
    var local by remember { mutableStateOf<List<T>?>(null) }
    var draggingKey by remember { mutableStateOf<Any?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<Any, Int>() }
    val view = LocalView.current
    val latestOnReorder by rememberUpdatedState(onReorder)
    val currentItems by rememberUpdatedState(items)
    LaunchedEffect(items) { if (draggingKey == null) local = null }
    val shown = local ?: items

    Column(modifier) {
        shown.forEachIndexed { index, item ->
            val k = key(item)
            key(k) {
                val dragging = draggingKey == k
                val handle = Modifier.pointerInput(k) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            local = (local ?: currentItems).toList()
                            draggingKey = k; offset = 0f
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        },
                        onDragEnd = {
                            draggingKey = null; offset = 0f
                            local?.let { latestOnReorder(it) }
                        },
                        onDragCancel = { draggingKey = null; offset = 0f },
                        onVerticalDrag = { change, delta ->
                            change.consume()
                            offset += delta
                            val list = local ?: return@detectVerticalDragGestures
                            val i = list.indexOfFirst { key(it) == k }
                            if (offset > 0 && i in 0 until list.lastIndex) {
                                val nextH = heights[key(list[i + 1])] ?: 0
                                if (offset > nextH / 2f) {
                                    local = list.toMutableList().apply { add(i + 1, removeAt(i)) }; offset -= nextH
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            } else if (offset < 0 && i > 0) {
                                val prevH = heights[key(list[i - 1])] ?: 0
                                if (-offset > prevH / 2f) {
                                    local = list.toMutableList().apply { add(i - 1, removeAt(i)) }; offset += prevH
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            }
                        },
                    )
                }
                Column(
                    Modifier
                        .onSizeChanged { heights[k] = it.height }
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer { translationY = if (dragging) offset else 0f }
                        .then(if (dragging) Modifier.shadow(8.dp) else Modifier)
                ) {
                    content(item, handle, dragging, index)
                }
            }
        }
    }
}
