package com.ahn.presentation.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat

class ThousandSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val formattedText = formatWithThousandSeparatorAndBreaks(originalText)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var transformedOffset = 0
                var originalOffset = 0

                while (originalOffset < offset && transformedOffset < formattedText.length) {
                    val transChar = formattedText[transformedOffset]

                    // Zero-width space나 쉼표는 원본에 없으므로 건너뜀
                    if (transChar == '\u200B' || transChar == ',') {
                        transformedOffset++
                        continue
                    }

                    originalOffset++
                    transformedOffset++
                }

                return transformedOffset.coerceAtMost(formattedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                var originalOffset = 0
                var transformedOffset = 0

                while (transformedOffset < offset && transformedOffset < formattedText.length) {
                    val transChar = formattedText[transformedOffset]

                    // Zero-width space나 쉼표는 원본에 없으므로 건너뜀
                    if (transChar == '\u200B' || transChar == ',') {
                        transformedOffset++
                        continue
                    }

                    originalOffset++
                    transformedOffset++
                }

                return originalOffset.coerceAtMost(originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}

// 천 단위 쉼표 + 연산자 앞 줄바꿈 힌트 추가
private fun formatWithThousandSeparatorAndBreaks(text: String): String {
    if (text.isEmpty()) return text

    val result = StringBuilder()
    val currentNumber = StringBuilder()

    for (char in text) {
        if (char.isDigit() || char == '.') {
            currentNumber.append(char)
        } else {
            // 숫자가 끝났으면 포맷팅해서 추가
            if (currentNumber.isNotEmpty()) {
                result.append(formatNumber(currentNumber.toString()))
                currentNumber.clear()
            }

            // 연산자 앞에 Zero-Width Space 추가 (줄바꿈 힌트)
            // +, -, ×, ÷ 앞에서 줄바꿈 가능하도록
            if (char in listOf('+', '-', '×', '÷')) {
                result.append("\u200B")  // 줄바꿈 가능 위치 힌트
            }

            result.append(char)
        }
    }

    // 마지막 숫자 처리
    if (currentNumber.isNotEmpty()) {
        result.append(formatNumber(currentNumber.toString()))
    }

    return result.toString()
}

private fun formatNumber(numberString: String): String {
    return try {
        // 소수점 처리
        if (numberString.contains(".")) {
            val parts = numberString.split(".")
            val intPart = parts[0].toLongOrNull()?.let {
                DecimalFormat("#,###").format(it)
            } ?: parts[0]
            if (parts.size > 1) "$intPart.${parts[1]}" else intPart
        } else {
            numberString.toLongOrNull()?.let {
                DecimalFormat("#,###").format(it)
            } ?: numberString
        }
    } catch (_: Exception) {
        numberString
    }
}

fun formatNumberWithCommas(text: String): String {
    if (text.isEmpty() || text == "Error") return text

    // 숫자 부분만 추출해서 포맷팅
    val result = StringBuilder()
    val currentNumber = StringBuilder()

    for (char in text) {
        if (char.isDigit() || char == '.') {
            currentNumber.append(char)
        } else {
            // 숫자가 끝났으면 포맷팅해서 추가
            if (currentNumber.isNotEmpty()) {
                result.append(formatNumber(currentNumber.toString()))
                currentNumber.clear()
            }
            // 연산자나 다른 문자 추가
            result.append(char)
        }
    }

    // 마지막 숫자 처리
    if (currentNumber.isNotEmpty()) {
        result.append(formatNumber(currentNumber.toString()))
    }

    return result.toString()
}