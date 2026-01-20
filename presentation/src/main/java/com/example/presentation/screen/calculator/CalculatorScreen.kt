package com.example.presentation.screen.calculator

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.R

@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("0") }

    val previewResult = remember(input) {
        if (input.isEmpty()) ""
        else {
            val res = calculate(input)
            if (res == "Error") "" else res
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Display Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // 남은 공간을 다 차지함
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween // 위(입력)와 아래(결과)로 벌림
        ) {
            // 1. 입력창 (TopEnd 역할)
            // 텍스트가 길어지면 스크롤되거나 줄어들도록 Box로 감쌉니다.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // 공간의 대부분을 입력창이 씀
                contentAlignment = Alignment.TopEnd // 상단 우측 정렬
            ) {
                Text(
                    text = input,
                    color = Color.White,
                    fontSize = 56.sp, // 메인이라 크게
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End,
                    lineHeight = 60.sp, // 줄간격 확보
                    overflow = TextOverflow.Visible, // 글자가 많으면 다음줄로 넘어가게 할 수도 있음 (선택사항)
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. 결과 미리보기 (BottomEnd 역할)
            // input이 "0"이 아니고, 미리보기 결과가 있을 때만 보여줌
            if (input != "0" && previewResult.isNotEmpty()) {
                Text(
                    text = previewResult,
                    color = Color.Gray, // 회색으로 연하게 (포인트)
                    fontSize = 32.sp,   // 작게
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End, // 우측 정렬
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Button Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: AC, (), Divide
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalculatorIconButton(
                    imageVector = Icons.Default.Refresh,
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFF9E9E9E),
                    contentColor = Color.Black,
                    contentDescription = "Refresh",
                    onClick = {  }
                )
                CalculatorButton(
                    text = "AC",
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFF9E9E9E),
                    textColor = Color.Black,
                    onClick = { input = "0" }
                )
                CalculatorButton(
                    text = "( )",
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFF9E9E9E),
                    textColor = Color.Black,
                    onClick = { input = addParentheses(input) }
                )
                CalculatorButton(
                    text = stringResource(R.string.divide),
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFFFF9500),
                    onClick = { input = addOperator(input, "÷") }
                )
            }

            // Row 2: 7, 8, 9, Multiply
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalculatorButton(
                    text = "7",
                    modifier = Modifier.weight(1f),
                    onClick = { input = addNumber(input, "7") })
                CalculatorButton(
                    text = "8",
                    modifier = Modifier.weight(1f),
                    onClick = { input = addNumber(input, "8") }
                )
                CalculatorButton(
                    text = "9",
                    modifier = Modifier.weight(1f),
                    onClick = { input = addNumber(input, "9") }
                )
                CalculatorButton(
                    text = "×",
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFFFF9500),
                    onClick = { input = addOperator(input, "×") }
                )
            }

            // Row 3: 4, 5, 6, Subtract
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalculatorButton(
                    text = "4",
                    modifier = Modifier.weight(1f),
                    onClick = { input = addNumber(input, "4") }
                )
                CalculatorButton(
                    text = "5",
                    modifier = Modifier.weight(1f),
                    onClick = { input = addNumber(input, "5") }
                )
                CalculatorButton(
                    text = "6",
                    modifier = Modifier.weight(1f),
                    onClick = { input = addNumber(input, "6") }
                )
                CalculatorButton(
                    text = "−",
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFFFF9500),
                    onClick = { input = addOperator(input, "−") }
                )
            }

            // Row 4: 1, 2, 3, Add
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalculatorButton(
                    text = "1",
                    modifier = Modifier.weight(1f),
                    onClick = { input = addNumber(input, "1") }
                )
                CalculatorButton(
                    text = "2",
                    modifier = Modifier.weight(1f),
                    onClick = { input = addNumber(input, "2") }
                )
                CalculatorButton(
                    text = "3",
                    modifier = Modifier.weight(1f),
                    onClick = { input = addNumber(input, "3") }
                )
                CalculatorButton(
                    text = "+",
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFFFF9500),
                    onClick = { input = addOperator(input, "+") }
                )
            }

            // Row 5: 0 (double width), ., =
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalculatorButton(
                    text = ".",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        // 소수점 중복 방지 로직 (현재 입력중인 마지막 숫자에 점이 없는지 체크)
                        val lastNumber = input.split(Regex("[+\\-×÷]")).last()
                        if (!lastNumber.contains(".")) {
                            input += "."
                        }
                    }
                )
                CalculatorButton(
                    text = "0",
                    modifier = Modifier.weight(1f),
                    onClick = { input = addNumber(input, "0") }
                )
                CalculatorButton(
                    text = "⌫",
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFF9E9E9E),
                    textColor = Color.Black,
                    onClick = { input = if (input.length > 1) input.dropLast(1) else "0" }
                )
                CalculatorButton(
                    text = "=",
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFFFF9500),
                    onClick = { input = calculate(input) }
                )
            }
        }
    }
}

private fun addNumber(currentInput: String, number: String): String {
    return if (currentInput == "0") number else currentInput + number
}

private fun addOperator(currentInput: String, operator: String): String {
    if (currentInput.isEmpty() || currentInput == "Error") return "0"

    val lastChar = currentInput.last()

    // 마지막이 이미 연산자라면 새 연산자로 교체
    return if (lastChar.toString().matches(Regex("[+\\-×÷]"))) {
        currentInput.dropLast(1) + operator
    } else {
        currentInput + operator
    }
}

private fun addParentheses(currentInput: String): String {
    val openCount = currentInput.count { it == '(' }
    val closeCount = currentInput.count { it == ')' }
    val lastChar = currentInput.lastOrNull()

    // 1. 닫아야 하는 상황인지 체크
    // (조건: 열린 게 더 많고 + 마지막이 숫자거나 닫는 괄호임)
    if (openCount > closeCount && lastChar != null && (lastChar.isDigit() || lastChar == ')')) {
        return "$currentInput)"
    }

    // 2. 그 외에는 여는 상황
    // (숫자 뒤에 바로 열 때는 곱하기 자동 추가)
    if (lastChar != null && (lastChar.isDigit() || lastChar == ')')) {
        return "$currentInput×("
    }

    // 그냥 열기
    return "$currentInput("
}

@SuppressLint("DefaultLocale")
private fun calculate(expr: String): String {
    return try {
        // 공백이 섞여있을 수 있으므로 제거
        val normalized = expr.replace(" ", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")

        if (normalized.isEmpty()) return "0"

        // 마지막이 연산자로 끝나면 제거 (예: "3+" 상태에서 = 누를 시)
        val finalExpr = if (!normalized.last().isDigit()) normalized.dropLast(1) else normalized

        val result = eval(finalExpr)
        if (result % 1.0 == 0.0) {
            result.toInt().toString()
        } else {
            String.format("%.10f", result).trimEnd('0').trimEnd('.')
        }
    } catch (_: Exception) {
        "Error"
    }
}

private fun eval(expr: String): Double {
    return object : Any() {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < expr.length) expr[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < expr.length) throw RuntimeException("Unexpected: " + ch.toChar())
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x += parseTerm()
                else if (eat('-'.code)) x -= parseTerm()
                else return x
            }
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor()
                else if (eat('/'.code)) x /= parseFactor()
                else return x
            }
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                x = expr.substring(startPos, pos).toDouble()
            } else {
                throw RuntimeException("Unexpected: " + ch.toChar())
            }

            return x
        }
    }.parse()
}

@Composable
fun CalculatorButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF333333),
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(color = backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun CalculatorIconButton(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF333333),
    contentColor: Color = Color.White,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(color = backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(32.dp)
        )
    }
}


@Preview(showBackground = true)
@Composable
fun CalculatorScreenPreview() {
    Surface {
        CalculatorScreen()
    }
}