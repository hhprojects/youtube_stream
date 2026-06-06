package com.youtubestream.app.ui.player

import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.youtubestream.app.playback.PlaybackConnection
import com.youtubestream.app.ui.components.MiniPlayer
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Docked player that lives above the nav bar (collapsed) and grows to full screen (expanded).
 * Renders nothing when idle — its children (MiniPlayer / PlayerScreen) early-return on no track.
 */
@Composable
fun PlayerSheet(
    connection: PlaybackConnection,
    sheet: PlayerSheetState,
    peekHeightPx: Int,
    navBarHeightPx: Int,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val progress = sheet.progress

    // Bridge the expanded player's list scroll with the sheet drag:
    //  - while the sheet sits between anchors, it owns the vertical drag (don't scroll the list)
    //  - fully expanded + list at top + dragging down → leftover lands here and starts the collapse
    //  - on release, snap to the nearer anchor by progress.
    val nested = remember(sheet) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val o = sheet.draggable.offset
                return if (!o.isNaN() && o > 0f && available.y != 0f) {
                    Offset(0f, sheet.draggable.dispatchRawDelta(available.y))
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                return if (available.y > 0f) {
                    Offset(0f, sheet.draggable.dispatchRawDelta(available.y))
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val o = sheet.draggable.offset
                if (!o.isNaN() && o > 0f) {
                    if (sheet.progress > 0.5f) sheet.expand() else sheet.collapse()
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .nestedScroll(nested)
            .onSizeChanged { size ->
                // Collapsed: pushed down so only the peek shows above the nav bar. Expanded: offset 0.
                val collapsed = (size.height - peekHeightPx - navBarHeightPx).toFloat().coerceAtLeast(0f)
                sheet.draggable.updateAnchors(
                    DraggableAnchors {
                        SheetAnchor.Collapsed at collapsed
                        SheetAnchor.Expanded at 0f
                    },
                )
            }
            .offset {
                // Guard the pre-measure frame: park off-screen until anchors snap the offset to Collapsed.
                val o = sheet.draggable.offset
                IntOffset(0, if (o.isNaN()) 100_000 else o.roundToInt())
            }
            .clip(RoundedCornerShape(topStart = lerp(20.dp, 0.dp, progress), topEnd = lerp(20.dp, 0.dp, progress))),
    ) {
        // Expanded body — GATE on progress, not just alpha: alpha(0) still hit-tests, so an always-composed
        // full-height PlayerScreen would overlay (and kill) the nav bar while collapsed. Scales in subtly too.
        if (progress > 0f) {
            PlayerScreen(
                connection = connection,
                onMinimize = { scope.launch { sheet.collapse() } },
                onBrowseLibrary = { scope.launch { sheet.collapse() } },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(progress)
                    .graphicsLayer {
                        val s = 0.96f + 0.04f * progress
                        scaleX = s
                        scaleY = s
                    },
            )
        }
        // Collapsed peek — also gated: when fully expanded its invisible click/drag would swallow taps
        // meant for the chevron / overflow ⋮. The two layers co-exist only mid-drag (0 < progress < 1).
        if (progress < 1f) {
            MiniPlayer(
                controller = connection,
                onClick = { scope.launch { sheet.expand() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .alpha(1f - progress)
                    .anchoredDraggable(sheet.draggable, Orientation.Vertical),
            )
        }
    }
}
