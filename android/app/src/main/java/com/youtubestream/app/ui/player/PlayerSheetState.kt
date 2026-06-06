package com.youtubestream.app.ui.player

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Thin wrapper over [AnchoredDraggableState] so the app shell deals in a small vocabulary:
 * a 0..1 [progress] for the morph and suspend expand()/collapse() for tap/chevron/back.
 */
class PlayerSheetState(val draggable: AnchoredDraggableState<SheetAnchor>) {

    /** 0f collapsed … 1f expanded. Safe before anchors are measured (returns 0). */
    val progress: Float
        get() {
            val a = draggable.anchors
            if (a.size < 2) return 0f
            val collapsed = a.positionOf(SheetAnchor.Collapsed)
            val expanded = a.positionOf(SheetAnchor.Expanded)
            val o = draggable.offset
            return progressFor(if (o.isNaN()) collapsed else o, collapsed, expanded)
        }

    val isExpanded: Boolean get() = draggable.currentValue == SheetAnchor.Expanded

    suspend fun expand() = draggable.animateTo(SheetAnchor.Expanded)
    suspend fun collapse() = draggable.animateTo(SheetAnchor.Collapsed)
}

@Composable
fun rememberPlayerSheetState(): PlayerSheetState {
    val draggable = remember { AnchoredDraggableState(initialValue = SheetAnchor.Collapsed) }
    return remember { PlayerSheetState(draggable) }
}
