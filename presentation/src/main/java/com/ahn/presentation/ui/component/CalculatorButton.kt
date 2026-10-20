package com.ahn.presentation.ui.component

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahn.presentation.ui.theme.KeyPressGlowSoft
import com.ahn.presentation.ui.theme.LocalCalcMoneyDarkTheme
import com.ahn.presentation.ui.theme.Orange
import com.ahn.presentation.ui.theme.buttonFunction
import com.ahn.presentation.ui.theme.buttonNumber
import com.ahn.presentation.ui.theme.buttonTextPrimary
import com.ahn.presentation.ui.theme.buttonTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun CalculatorButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.buttonNumber,
    textColor: Color = MaterialTheme.colorScheme.buttonTextPrimary,
    pressedTextColor: Color = textColor,
    pressColors: KeyPressColors = KeyPressColors.Default,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressProgress = rememberKeyPressProgress(interactionSource)

    CalculatorKeySurface(
        pressProgress = pressProgress,
        backgroundColor = backgroundColor,
        pressColors = pressColors,
        modifier = modifier,
        inputModifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        ),
    ) {
        Text(
            text = text,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
            color = lerp(textColor, pressedTextColor, pressProgress.value),
        )
    }
}

@Composable
fun CalculatorIconButton(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.buttonFunction,
    contentColor: Color = MaterialTheme.colorScheme.buttonTextSecondary,
    pressedContentColor: Color = contentColor,
    pressColors: KeyPressColors = KeyPressColors.Default,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressProgress = rememberKeyPressProgress(interactionSource)

    CalculatorKeySurface(
        pressProgress = pressProgress,
        backgroundColor = backgroundColor,
        pressColors = pressColors,
        modifier = modifier,
        inputModifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        ),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = lerp(contentColor, pressedContentColor, pressProgress.value),
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
fun DeleteCalculatorButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.buttonNumber,
    textColor: Color = MaterialTheme.colorScheme.buttonTextPrimary,
    pressedTextColor: Color = textColor,
    pressColors: KeyPressColors = KeyPressColors.Default,
    onDeleteAction: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val pressProgress = rememberKeyPressProgress(interactionSource)

    CalculatorKeySurface(
        pressProgress = pressProgress,
        backgroundColor = backgroundColor,
        pressColors = pressColors,
        modifier = modifier,
        inputModifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = { offset ->
                    // 길게 누르면 반복 삭제하느라 clickable 대신 직접 제스처를 받는다.
                    // 누름 효과는 다른 키와 같은 경로를 타도록 눌림을 직접 알린다.
                    val press = PressInteraction.Press(offset)
                    interactionSource.emit(press)
                    val job = scope.launch {
                        onDeleteAction()
                        delay(500)
                        while (isActive) {
                            onDeleteAction()
                            delay(100)
                        }
                    }
                    val released = tryAwaitRelease()

                    job.cancel()
                    interactionSource.emit(
                        if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press),
                    )
                },
            )
        },
    ) {
        Text(
            text = text,
            fontSize = 31.sp,
            fontWeight = FontWeight.Medium,
            color = lerp(textColor, pressedTextColor, pressProgress.value),
        )
    }
}

/**
 * 계산기 키의 원형 바탕과 누름 효과를 그립니다.
 *
 * 누르면 원 둘레로 [KeyPressColors]의 빛이 번지고 바탕이 그 색 쪽으로 물듭니다. 기본은 주황→호박색입니다. 삼성 계산기의 누름 효과를
 * 앱 강조색으로 옮긴 것으로, 손가락 위치에서 퍼지는 기본 물결 효과(ripple)는 쓰지 않습니다.
 */
@Composable
private fun CalculatorKeySurface(
    pressProgress: Animatable<Float, AnimationVector1D>,
    backgroundColor: Color,
    pressColors: KeyPressColors,
    modifier: Modifier,
    inputModifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDarkTheme = LocalCalcMoneyDarkTheme.current
    val glowAlpha = if (isDarkTheme) DARK_GLOW_ALPHA else LIGHT_GLOW_ALPHA
    val tintFraction = if (isDarkTheme) DARK_TINT_FRACTION else LIGHT_TINT_FRACTION

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(6.dp)
            .keyPressGlow(colors = pressColors, progress = { pressProgress.value * glowAlpha })
            .shadow(
                elevation = calculatorButtonElevation(),
                shape = CircleShape,
                clip = false,
            )
            .clip(CircleShape)
            .drawBehind { drawRect(lerp(backgroundColor, pressColors.accent, tintFraction * pressProgress.value)) }
            .then(inputModifier),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * 키를 누른 정도(0~1)를 애니메이션합니다.
 *
 * 누르면 빠르게 켜고 떼면 천천히 끕니다. 짧게 탭해도 빛이 끝까지 켜졌다가 사라지도록 켜는 애니메이션이
 * 끝난 뒤에 끄고, 끄는 도중에 다시 누르면 바로 다시 켭니다.
 */
@Composable
private fun rememberKeyPressProgress(interactionSource: InteractionSource): Animatable<Float, AnimationVector1D> {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(interactionSource) {
        var animation: Job? = null

        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    animation?.cancel()
                    animation = launch { progress.animateTo(1f, tween(PRESS_IN_MILLIS)) }
                }

                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    val pressIn = animation
                    animation = launch {
                        pressIn?.join()
                        progress.animateTo(0f, tween(PRESS_OUT_MILLIS, easing = FastOutSlowInEasing))
                    }
                }
            }
        }
    }

    return progress
}

/** 원 둘레로 번지는 빛. 오른쪽·아래는 [KeyPressColors.accent], 왼쪽·위는 [KeyPressColors.soft]다. */
private fun Modifier.keyPressGlow(colors: KeyPressColors, progress: () -> Float): Modifier = drawBehind {
    val alpha = progress()
    if (alpha <= 0f) return@drawBehind

    drawIntoCanvas { canvas ->
        val paint = Paint()
        Brush.sweepGradient(
            colors = listOf(colors.accent, colors.accent, colors.soft, colors.soft, colors.accent),
            center = center,
        ).applyTo(size, paint, alpha)
        paint.asFrameworkPaint().maskFilter = BlurMaskFilter(GLOW_BLUR_RADIUS.toPx(), BlurMaskFilter.Blur.NORMAL)
        canvas.drawCircle(center, size.minDimension / 2 + GLOW_SPREAD.toPx(), paint)
    }
}

/**
 * 키를 누를 때 번지는 빛과 바탕을 물들이는 색입니다.
 *
 * @property accent 빛의 진한 쪽(오른쪽·아래)이자 바탕을 물들이는 색
 * @property soft 빛의 옅은 쪽(왼쪽·위)
 */
@Immutable
data class KeyPressColors(val accent: Color, val soft: Color) {
    companion object {
        /** 숫자·연산자 키. 앱 강조색 주황에서 옅은 호박색으로 이어진다. */
        val Default = KeyPressColors(accent = Orange, soft = KeyPressGlowSoft)
    }
}

@Composable
private fun calculatorButtonElevation() = if (LocalCalcMoneyDarkTheme.current) 0.dp else 2.dp

// 삼성 계산기에서 잰 값: 켜질 때 약 0.1초, 꺼질 때 약 0.25~0.3초
private const val PRESS_IN_MILLIS = 90
private const val PRESS_OUT_MILLIS = 320

// 빛을 키보다 조금 크게 그린 뒤 흐린다. 키 사이 간격(12dp)으로 번지고, 더 번진 부분은 옆 키에 가려진다.
private val GLOW_SPREAD = 3.dp
private val GLOW_BLUR_RADIUS = 8.dp

// 밝은 배경에서는 같은 빛도 강해 보여서 라이트 모드를 더 옅게 둔다.
private const val DARK_GLOW_ALPHA = 0.9f
private const val LIGHT_GLOW_ALPHA = 0.85f
private const val DARK_TINT_FRACTION = 0.15f
private const val LIGHT_TINT_FRACTION = 0.08f
