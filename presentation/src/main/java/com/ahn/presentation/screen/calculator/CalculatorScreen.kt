package com.ahn.presentation.screen.calculator

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahn.presentation.R
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.Locale

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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentToast by remember { mutableStateOf<Toast?>(null) }
    val focusRequester = remember { FocusRequester() }

    var inputState by remember {
        mutableStateOf(
            TextFieldValue(
                text = "",
                selection = TextRange(0)
            )
        )
    }

    val previewResult = remember(inputState.text) {
        val text = inputState.text

        if (text.isEmpty()) ""
        else {
            val lastChar = text.last()
            val hasOperator = text.any { it in listOf('+', '-', '×', '÷') }
            val isJustNegativeNumber =
                text.startsWith("-") && text.count { it in listOf('+', '-', '×', '÷') } == 1

            if (!hasOperator || lastChar in listOf('+', '-', '×', '÷') || isJustNegativeNumber) ""
            else {
                val res = calculate(text)
                if (res == "Error") "" else res
            }
        }
    }

    val dynamicFontSize = remember(inputState.text.length) {
        when (inputState.text.length) {
            in 0..11 -> 42.sp
            in 11..16 -> 35.sp
            else -> 30.sp
        }
    }

    fun showToast(message: String) {
        currentToast?.cancel()
        val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        toast.show()
        currentToast = toast
    }

    fun canInsertNumber(currentText: String, cursorIndex: Int): Boolean {
        // 전체 텍스트를 구분자로 쪼갰을 때 커서가 위치한 세그먼트 찾기
        // (간단하게 구현하기 위해 커서 앞쪽의 마지막 구분자 이후부터 커서 뒤쪽 첫 구분자 전까지의 길이를 잰다)

        // 1. 커서 앞부분 탐색
        val textBeforeCursor = currentText.take(cursorIndex)
        val lastSeparatorIndexBefore = textBeforeCursor.indexOfLast { it in "+-×÷\n()" }
        val startOfNumber = if (lastSeparatorIndexBefore == -1) 0 else lastSeparatorIndexBefore + 1

        // 2. 커서 뒷부분 탐색
        val textAfterCursor = currentText.drop(cursorIndex)
        val firstSeparatorIndexAfter = textAfterCursor.indexOfFirst { it in "+-×÷\n()" }
        val endOfNumber =
            if (firstSeparatorIndexAfter == -1) currentText.length else cursorIndex + firstSeparatorIndexAfter

        // 3. 현재 숫자 블록 추출 및 길이 체크
        val currentNumberPart = currentText.substring(startOfNumber, endOfNumber)

        // 소수점(.)과 E 표기법 문자는 글자 수 제한에서 유연하게 처리할 수 있으나, 여기선 순수 숫자 길이만 체크하거나 전체 길이 체크
        // 여기서는 E, -, . 을 포함한 전체 길이로 체크합니다.
        return currentNumberPart.length < 15
    }

    fun insert(textToInsert: String) {
        val currentText = inputState.text
        val selection = inputState.selection
        val operators = listOf("+", "-", "×", "÷")

        if (textToInsert.all { it.isDigit() || it == '.' }) {
            if (!canInsertNumber(currentText, selection.min)) {
                showToast("숫자는 최대 15자리까지 입력 가능합니다.")
                return
            }
        }

        if (currentText.isEmpty()) {
            if (textToInsert in operators) return
            if (textToInsert == ".") {
                inputState = TextFieldValue(text = "0.", selection = TextRange(2))
                return
            }

            inputState = TextFieldValue(
                text = textToInsert,
                selection = TextRange(textToInsert.length)
            )

            return
        }

        if (currentText == "0" && textToInsert !in operators && textToInsert != ".") {
            inputState = TextFieldValue(
                text = textToInsert,
                selection = TextRange(textToInsert.length)
            )
            return
        }

        if (textToInsert in operators) {
            val cursor = selection.min

            // 맨 앞에 연산자가 오려고 할 때 (예: 커서가 0에 있음) 입력 막기 (선택 사항)
            if (cursor == 0) return

            // 커서 바로 앞 글자가 연산자인지 확인
            if (cursor > 0) {
                val charBefore = currentText[cursor - 1]
                if (charBefore.toString() in operators) {
                    // 이전 연산자를 지우고 새로운 연산자로 교체 (예: 1+ 상태에서 - 입력 시 1-로 변경)
                    val newText = StringBuilder(currentText)
                        .replace(cursor - 1, cursor, textToInsert)
                        .toString()

                    inputState = TextFieldValue(
                        text = newText,
                        selection = TextRange(cursor) // 커서 위치 유지
                    )
                    return
                }
            }
        }

        val newText = StringBuilder(currentText)
            .replace(selection.min, selection.max, textToInsert)
            .toString()

        val newCursorPosition = selection.min + textToInsert.length
        inputState = TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPosition)
        )
    }

    fun delete() {
        val currentText = inputState.text
        val selection = inputState.selection

        if (currentText.isEmpty()) {
            return
        }

        val cursor = selection.min
        // 커서가 맨 앞에 있으면 지울게 없음 (단, 드래그 선택 상태면 지움)
        if (cursor == 0 && selection.collapsed) return

        val newText: String
        val newCursorPos: Int

        if (!selection.collapsed) {
            // 1. 드래그 선택된 영역 삭제
            newText = currentText.removeRange(selection.min, selection.max)
            newCursorPos = selection.min
        } else {
            // 2. 커서 앞 한 글자 삭제
            newText = currentText.removeRange(cursor - 1, cursor)
            newCursorPos = cursor - 1
        }

        // 다 지웠으면 "0"으로
        inputState = if (newText.isEmpty()) {
            TextFieldValue("", selection = TextRange(0))
        } else {
            TextFieldValue(text = newText, selection = TextRange(newCursorPos))
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
            // 1. 입력창
            // 텍스트가 길어지면 스크롤되거나 줄어들도록 Box로 감쌉니다.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // 공간의 대부분을 입력창이 씀
                contentAlignment = Alignment.TopEnd
            ) {
                InterceptPlatformTextInput(
                    interceptor = { _, _ ->
                        awaitCancellation()
                    }
                ) {
                    BasicTextField(
                        value = inputState,
                        onValueChange = { newValue -> inputState = newValue },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = dynamicFontSize,
                            fontWeight = FontWeight.Light,
                            textAlign = TextAlign.End,
                        ),
                        visualTransformation = remember { ThousandSeparatorTransformation() },
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
            }

            // 2. 결과 미리보기
            // input이 "0"이 아니고, 미리보기 결과가 있을 때만 보여줌
            if (inputState.text != "0" && previewResult.isNotEmpty()) {
                Text(
                    text = formatNumberWithCommas(previewResult),
                    color = Color.Gray,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
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
                    onClick = { }
                )
                CalculatorButton(
                    text = "AC",
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFF9E9E9E),
                    textColor = Color.Black,
                    onClick = { inputState = TextFieldValue(text = "", selection = TextRange(0)) }
                )
                CalculatorButton(
                    text = "( )",
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFF9E9E9E),
                    textColor = Color.Black,
                    onClick = {
                        val textToInsert = getParenthesisToInsert(inputState)
                        insert(textToInsert)
                    }
                )
                CalculatorButton(
                    text = stringResource(R.string.divide),
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFFFF9500),
                    onClick = { insert("÷") }
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
                    onClick = { insert("7") }
                )
                CalculatorButton(
                    text = "8",
                    modifier = Modifier.weight(1f),
                    onClick = { insert("8") }
                )
                CalculatorButton(
                    text = "9",
                    modifier = Modifier.weight(1f),
                    onClick = { insert("9") }
                )
                CalculatorButton(
                    text = "×",
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFFFF9500),
                    onClick = { insert("×") }
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
                    onClick = { insert("4") }
                )
                CalculatorButton(
                    text = "5",
                    modifier = Modifier.weight(1f),
                    onClick = { insert("5") }
                )
                CalculatorButton(
                    text = "6",
                    modifier = Modifier.weight(1f),
                    onClick = { insert("6") }
                )
                CalculatorButton(
                    text = "−",
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFFFF9500),
                    onClick = { insert("-") }
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
                    onClick = { insert("1") }
                )
                CalculatorButton(
                    text = "2",
                    modifier = Modifier.weight(1f),
                    onClick = { insert("2") }
                )
                CalculatorButton(
                    text = "3",
                    modifier = Modifier.weight(1f),
                    onClick = { insert("3") }
                )
                CalculatorButton(
                    text = "+",
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFFFF9500),
                    onClick = { insert("+") }
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
                    onClick = { insert(".") }
                )
                CalculatorButton(
                    text = "0",
                    modifier = Modifier.weight(1f),
                    onClick = { insert("0") }
                )
                DeleteCalculatorButton(
                    text = "⌫",
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFF9E9E9E),
                    textColor = Color.Black,
                    onDeleteAction = { delete() }
                )
                CalculatorButton(
                    text = "=",
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFFFF9500),
                    onClick = {
                        val result = calculate(inputState.text)
                        inputState = TextFieldValue(result, selection = TextRange(result.length))
                    }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }
}

private fun getParenthesisToInsert(state: TextFieldValue): String {
    val text = state.text
    val cursor = state.selection.min // 현재 커서 위치

    // 전체 괄호 개수 카운트
    val openCount = text.count { it == '(' }
    val closeCount = text.count { it == ')' }

    // 커서 바로 앞의 글자 확인 (커서가 맨 앞이면 null)
    val charBeforeCursor = if (cursor > 0) text.getOrNull(cursor - 1) else null

    // 1. 닫아야 하는 상황인지 체크
    // (조건: 열린 게 더 많고 + 커서 앞이 숫자거나 닫는 괄호임)
    if (openCount > closeCount && charBeforeCursor != null && (charBeforeCursor.isDigit() || charBeforeCursor == ')')) {
        return ")"
    }

    // 2. 그 외에는 여는 상황
    // (커서 앞이 숫자거나 닫는 괄호면 곱하기 자동 추가)
    if (charBeforeCursor != null && (charBeforeCursor.isDigit() || charBeforeCursor == ')')) {
        return "×("
    }

    // 3. 그냥 열기
    return "("
}

private fun calculate(expression: String): String {
    return try {
        val normalized = expression
            .replace("\n", "")
            .replace(" ", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")

        if (normalized.isEmpty()) return "0"

        // 마지막이 연산자면 제거
        val finalExpression = if (normalized.last().toString().matches(Regex("[+\\-*/]"))) {
            normalized.dropLast(1)
        } else {
            normalized
        }

        val result = eval(finalExpression)

        // 15자리를 넘어가거나(10^15), 너무 작으면(10^-10) E 표기법 사용
        val absResult = kotlin.math.abs(result)
        if (absResult != 0.0 && (absResult >= 1e15 || absResult < 1e-10)) {
            // 예: 1.2345678900E15 (소수점 10자리)
            String.format(Locale.US, "%.10E", result).replace(",", ".")
        } else if (result % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", result)
        } else {
            String.format(Locale.US, "%.10f", result).trimEnd('0').trimEnd('.')
        }
    } catch (e: Exception) {
        Log.e(e.javaClass.simpleName, "Calculation error for expression: $expression", e)
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

@Composable
fun DeleteCalculatorButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF333333),
    textColor: Color = Color.White,
    onDeleteAction: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(color = if (isPressed) backgroundColor.copy(alpha = 0.7f) else backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val job = scope.launch {
                            onDeleteAction()
                            delay(500)
                            while (isActive) {
                                onDeleteAction()
                                delay(100)
                            }
                        }
                        tryAwaitRelease()

                        job.cancel()
                        isPressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
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