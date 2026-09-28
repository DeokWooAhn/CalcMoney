package com.ahn.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 인덱스가 차지하는 폭. 목록 줄은 이만큼 오른쪽을 비워 두어야 하트 버튼이 가려지지 않는다. */
internal val FastScrollIndexWidth: Dp = 32.dp

private val PillWidth = 22.dp
private val PillVerticalPadding = 6.dp
private val MaxLabelHeight = 18.dp

/**
 * 목록 오른쪽에 붙는 빠른 이동 인덱스입니다.
 *
 * 라벨을 누르거나 위아래로 끌면 그 위치의 라벨을 [onLabelSelected]로 알립니다. 같은 라벨 위에서는 다시 알리지 않습니다.
 * 스크롤 중에만 잠깐 보이는 보조 조작이라 접근성 트리에서는 뺍니다. 목록 자체는 그대로 탐색할 수 있습니다.
 *
 * @param labels 위에서부터 보여줄 인덱스 라벨입니다.
 * @param onLabelSelected 사용자가 고른 라벨을 받습니다.
 * @param onTouchingChange 인덱스를 누르고 있는지 여부를 받습니다. 누르는 동안에는 인덱스를 숨기지 않아야 합니다.
 */
@Composable
internal fun FastScrollIndex(
    labels: List<String>,
    onLabelSelected: (String) -> Unit,
    onTouchingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (labels.isEmpty()) return

    val haptic = LocalHapticFeedback.current
    val currentOnLabelSelected by rememberUpdatedState(onLabelSelected)
    val currentOnTouchingChange by rememberUpdatedState(onTouchingChange)
    var activeLabel by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(modifier = modifier.clearAndSetSemantics {}) {
        // 라벨이 많거나 목록이 짧으면 라벨 높이를 줄여 전부 들어가게 한다.
        val labelHeight = minOf(MaxLabelHeight, (maxHeight - PillVerticalPadding * 2) / labels.size)
        val density = LocalDensity.current
        val labelHeightPx = with(density) { labelHeight.toPx() }
        val verticalPaddingPx = with(density) { PillVerticalPadding.toPx() }

        fun labelAt(y: Float): String {
            val index = ((y - verticalPaddingPx) / labelHeightPx).toInt().coerceIn(0, labels.lastIndex)
            return labels[index]
        }

        fun select(label: String) {
            if (label == activeLabel) return
            activeLabel = label
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            currentOnLabelSelected(label)
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(FastScrollIndexWidth)
                .height(labelHeight * labels.size + PillVerticalPadding * 2)
                .pointerInput(labels, labelHeightPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        currentOnTouchingChange(true)
                        down.consume()
                        select(labelAt(down.position.y))

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change?.pressed == true) {
                                change.consume()
                                select(labelAt(change.position.y))
                            }
                        } while (change?.pressed == true)

                        activeLabel = null
                        currentOnTouchingChange(false)
                    }
                },
        ) {
            IndexPill(labels = labels, labelHeight = labelHeight, activeLabel = activeLabel)
        }
    }
}

@Composable
private fun IndexPill(
    labels: List<String>,
    labelHeight: Dp,
    activeLabel: String?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(PillWidth)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                shape = RoundedCornerShape(PillWidth / 2),
            )
            .padding(vertical = PillVerticalPadding),
    ) {
        labels.forEach { label ->
            val isActive = label == activeLabel
            val color = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(labelHeight),
            ) {
                if (label == FAVORITE_INDEX_LABEL) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(12.dp),
                    )
                } else {
                    Text(
                        text = label,
                        color = color,
                        fontSize = 11.sp,
                        lineHeight = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}
