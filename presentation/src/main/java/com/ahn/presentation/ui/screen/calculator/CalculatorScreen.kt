package com.ahn.presentation.ui.screen.calculator

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahn.domain.usecase.CalculatorEngine.calculate
import com.ahn.presentation.R
import com.ahn.presentation.ui.component.CalculatorButton
import com.ahn.presentation.ui.component.CalculatorIconButton
import com.ahn.presentation.ui.component.DeleteCalculatorButton
import com.ahn.presentation.util.ThousandSeparatorTransformation
import com.ahn.presentation.util.formatNumberWithCommas
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay

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

@Preview(showBackground = true)
@Composable
fun CalculatorScreenPreview() {
    Surface {
        CalculatorScreen()
    }
}