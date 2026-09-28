package com.ahn.presentation.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

/** 인덱스가 차지하는 폭. 목록 줄은 이만큼 오른쪽을 비워 두어야 하트 버튼이 가려지지 않는다. */
internal val FastScrollIndexWidth: Dp = 32.dp

private val PillWidth = 22.dp
private val PillVerticalPadding = 6.dp
private val MaxLabelHeight = 18.dp

/** 선택된 라벨의 최대 배율. 과하지 않게 살짝만 키운다. */
private const val WAVE_MAX_SCALE = 1.35f

/** 물결이 퍼지는 반경. 선택된 라벨 위아래로 라벨 몇 개까지 영향을 줄지 정한다. */
private const val WAVE_RADIUS_IN_LABELS = 2.5f

/** 선택된 라벨이 커진 만큼 위아래 라벨을 밀어내는 최대 거리 */
private val WaveMaxPush = 3.dp

/** 선택된 라벨 바로 위아래 라벨을 흐리게 하는 정도. 1이면 완전히 사라진다. */
private const val WAVE_MAX_DIM = 0.7f

/**
 * 물결 중심과 라벨 사이 거리에 따른 커지는 정도입니다.
 *
 * 물결 중심은 1, 반경 끝은 0이고 그 사이는 코사인 곡선으로 부드럽게 줄어듭니다.
 *
 * @param distance 물결 중심과 라벨 중심 사이의 세로 거리입니다. 부호는 무시합니다.
 * @param radius 물결이 퍼지는 반경입니다. 0 이하이면 물결이 없습니다.
 */
internal fun waveInfluence(distance: Float, radius: Float): Float {
    if (radius <= 0f) return 0f

    val ratio = abs(distance) / radius
    if (ratio >= 1f) return 0f

    return ((cos(ratio * PI) + 1) / 2).toFloat()
}

/**
 * 물결 중심 주변 라벨이 밀려나고 흐려지는 정도입니다.
 *
 * 물결 중심과 반경 끝은 0이고, 그 가운데에서 1로 가장 큽니다. 선택된 라벨 바로 위아래 라벨이 가장 크게 반응해
 * 위아래로 출렁이는 느낌을 줍니다.
 *
 * @param distance 물결 중심과 라벨 중심 사이의 세로 거리입니다. 부호는 무시합니다.
 * @param radius 물결이 퍼지는 반경입니다. 0 이하이면 물결이 없습니다.
 */
internal fun waveRipple(distance: Float, radius: Float): Float {
    if (radius <= 0f) return 0f

    val ratio = abs(distance) / radius
    if (ratio >= 1f) return 0f

    return sin(ratio * PI).toFloat()
}

/**
 * 목록 오른쪽에 붙는 빠른 이동 인덱스입니다.
 *
 * 라벨을 누르거나 위아래로 끌면 그 위치의 라벨을 [onLabelSelected]로 알립니다. 같은 라벨 위에서는 다시 알리지 않습니다.
 * 누르는 동안에는 선택된 라벨이 살짝 커지고 위아래 라벨이 흐려지며 밀려나는 물결 효과를 줍니다. 손가락에 가려진 현재 라벨은
 * 목록 쪽에서 [FastScrollIndexPreview]로 크게 보여 줍니다.
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
    var isTouching by remember { mutableStateOf(false) }
    // 손을 뗀 뒤에도 마지막 위치를 남겨 두어, 물결이 그 자리에서 가라앉게 한다.
    var activeIndex by remember { mutableIntStateOf(0) }
    val waveIntensity = animateFloatAsState(
        targetValue = if (isTouching) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "fastScrollWaveIntensity",
    )
    // 물결 중심. 끄는 동안 라벨 사이를 부드럽게 미끄러지고, 처음 누를 때는 누른 자리에 바로 놓인다.
    val waveCenterIndex = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier.clearAndSetSemantics {}) {
        // 라벨이 많거나 목록이 짧으면 라벨 높이를 줄여 전부 들어가게 한다.
        val labelHeight = minOf(MaxLabelHeight, (maxHeight - PillVerticalPadding * 2) / labels.size)
        val density = LocalDensity.current
        val labelHeightPx = with(density) { labelHeight.toPx() }
        val verticalPaddingPx = with(density) { PillVerticalPadding.toPx() }

        fun indexAt(y: Float): Int {
            return ((y - verticalPaddingPx) / labelHeightPx).toInt().coerceIn(0, labels.lastIndex)
        }

        fun track(y: Float, isFirstTouch: Boolean) {
            val index = indexAt(y)
            if (!isFirstTouch && index == activeIndex) return
            activeIndex = index
            scope.launch {
                if (isFirstTouch) {
                    waveCenterIndex.snapTo(index.toFloat())
                } else {
                    waveCenterIndex.animateTo(index.toFloat(), spring(stiffness = Spring.StiffnessMedium))
                }
            }
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            currentOnLabelSelected(labels[index])
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
                        isTouching = true
                        currentOnTouchingChange(true)
                        down.consume()
                        track(down.position.y, isFirstTouch = true)

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change?.pressed == true) {
                                change.consume()
                                track(change.position.y, isFirstTouch = false)
                            }
                        } while (change?.pressed == true)

                        isTouching = false
                        currentOnTouchingChange(false)
                    }
                },
        ) {
            IndexPill(
                labels = labels,
                labelHeight = labelHeight,
                activeIndex = if (isTouching) activeIndex else null,
                wave = IndexWave(
                    labelHeightPx = labelHeightPx,
                    centerIndex = { waveCenterIndex.value },
                    intensity = { waveIntensity.value },
                ),
            )
        }
    }
}

/**
 * 누르고 있는 인덱스 라벨을 목록 가운데에 크게 보여 주는 미리보기입니다.
 *
 * 인덱스 라벨은 손가락에 가려지므로, 지금 어느 구간으로 가는지 여기서 확인하게 합니다.
 */
@Composable
internal fun FastScrollIndexPreview(label: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(80.dp)
            .background(color = Color.Black.copy(alpha = 0.72f), shape = RoundedCornerShape(18.dp))
            .clearAndSetSemantics {},
    ) {
        if (label == FAVORITE_INDEX_LABEL) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(36.dp),
            )
        } else {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * 라벨마다 물결 효과를 계산하는 데 필요한 값입니다.
 *
 * [centerIndex]와 [intensity]는 그리기 단계에서 읽도록 람다로 넘깁니다. 애니메이션이 도는 동안 전체를 다시 구성하지 않고
 * 라벨의 크기·위치·투명도만 다시 그리기 위해서입니다.
 */
private class IndexWave(
    val labelHeightPx: Float,
    val centerIndex: () -> Float,
    val intensity: () -> Float,
) {
    private val radiusPx get() = labelHeightPx * WAVE_RADIUS_IN_LABELS

    /** 물결 중심에서 이 라벨까지의 세로 거리. 아래쪽이 양수다. */
    fun distanceOf(index: Int): Float = (index - centerIndex()) * labelHeightPx

    fun scaleOf(index: Int): Float {
        return 1f + (WAVE_MAX_SCALE - 1f) * intensity() * waveInfluence(distanceOf(index), radiusPx)
    }

    fun pushOf(index: Int, maxPushPx: Float): Float {
        val distance = distanceOf(index)
        return sign(distance) * maxPushPx * intensity() * waveRipple(distance, radiusPx)
    }

    fun alphaOf(index: Int): Float {
        return 1f - WAVE_MAX_DIM * intensity() * waveRipple(distanceOf(index), radiusPx)
    }
}

@Composable
private fun IndexPill(
    labels: List<String>,
    labelHeight: Dp,
    activeIndex: Int?,
    wave: IndexWave,
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
        labels.forEachIndexed { index, label ->
            val isActive = index == activeIndex
            val color = if (isActive) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(labelHeight)
                    .graphicsLayer {
                        val scale = wave.scaleOf(index)
                        scaleX = scale
                        scaleY = scale
                        translationY = wave.pushOf(index, WaveMaxPush.toPx())
                        alpha = wave.alphaOf(index)
                    },
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
