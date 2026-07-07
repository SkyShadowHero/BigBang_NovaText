package com.cashewteam.novatext.android.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies hover + press interactive effects to a component:
 * - Hover: scales up to [hoverScale] with smooth animation, shifts toward cursor
 * - Press: scales down to [pressScale] with smooth animation
 *
 * Must be used together with [androidx.compose.foundation.hoverable] and
 * [androidx.compose.foundation.clickable] sharing the same [interactionSource].
 *
 * Call this in a @Composable context (inside a Composable function or lambda).
 */
@Composable
fun Modifier.interactiveHover(
    interactionSource: MutableInteractionSource,
    hoverScale: Float = 1.15f,
    pressScale: Float = 0.92f,
    hoverMaxShift: Dp = 3.dp,
    springDampingRatio: Float = 0.7f,
    springStiffness: Float = 120f,
    enabled: Boolean = true,
): Modifier {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    var hoverPos by remember { mutableStateOf(Offset.Zero) }

    val targetScale = when {
        isPressed -> pressScale
        isHovered -> hoverScale
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = springDampingRatio, stiffness = springStiffness),
        label = "fx_scale",
    )
    val posMod = if (enabled) {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    when (event.type) {
                        PointerEventType.Enter,
                        PointerEventType.Move -> {
                            hoverPos = event.changes.firstOrNull()?.position ?: Offset.Zero
                        }
                        PointerEventType.Exit -> {
                            // 不移零 — 让 scale 动画自然回位到 1f 时 translation 也归零
                        }
                        else -> {}
                    }
                }
            }
        }
    } else {
        Modifier
    }

    return posMod
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            val center = size.width / 2f
            val maxShiftPx = hoverMaxShift.toPx() * (scale - 1f) * 5f
            translationX = ((hoverPos.x - center) / center).coerceIn(-1f, 1f) * maxShiftPx
            translationY = ((hoverPos.y - center) / center).coerceIn(-1f, 1f) * maxShiftPx
        }
}
