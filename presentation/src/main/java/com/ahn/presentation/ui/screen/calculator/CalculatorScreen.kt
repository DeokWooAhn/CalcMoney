package com.ahn.presentation.ui.screen.calculator


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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahn.presentation.R
import com.ahn.presentation.ui.component.CalculatorButton
import com.ahn.presentation.ui.component.CalculatorIconButton
import com.ahn.presentation.ui.component.CustomSnackbarHost
import com.ahn.presentation.ui.component.DeleteCalculatorButton
import com.ahn.presentation.util.ThousandSeparatorTransformation
import com.ahn.presentation.util.formatNumberWithCommas
import com.ahn.presentation.util.showSnackbarImmediately
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CalculatorRoute(
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel.sideEffect) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is CalculatorContract.SideEffect.ShowSnackBar -> {
                    scope.launch {
                        snackbarHostState.showSnackbarImmediately(sideEffect.message)
                    }
                }
            }
        }
    }

    CalculatorScreen(
        state = state,
        onIntent = viewModel::processIntent,
        onCursorMove = viewModel::updateCursorPosition,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CalculatorScreen(
    state: CalculatorContract.State,
    onIntent: (CalculatorContract.Intent) -> Unit,
    onCursorMove: (Int) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {

    val focusRequester = remember { FocusRequester() }

    val textFieldValue = TextFieldValue(
        text = state.expression,
        selection = TextRange(state.cursorPosition)
    )

    val dynamicFontSize = remember(state.expression.length) {
        when (state.expression.length) {
            in 0..11 -> 42.sp
            in 11..16 -> 35.sp
            else -> 30.sp
        }
    }

    Scaffold(
        snackbarHost = {
            CustomSnackbarHost(snackbarHostState = snackbarHostState)
        },
        containerColor = Color.Black,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                // 텍스트가 길어지면 스크롤되거나 줄어들도록 Box로 감쌈.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.TopEnd
                ) {
                    InterceptPlatformTextInput(
                        interceptor = { _, _ ->
                            awaitCancellation()
                        }
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                if (newValue.text == state.expression) {
                                    onCursorMove(newValue.selection.start)
                                }
                            },
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
                if (state.expression.isNotEmpty() && state.previewResult.isNotEmpty()) {
                    Text(
                        text = formatNumberWithCommas(state.previewResult),
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
                        onClick = { onIntent(CalculatorContract.Intent.Clear) }
                    )
                    CalculatorButton(
                        text = "( )",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF9E9E9E),
                        textColor = Color.Black,
                        onClick = {
                            onIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Parenthesis
                                )
                            )
                        }
                    )
                    CalculatorButton(
                        text = stringResource(R.string.divide),
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFF9500),
                        onClick = {
                            onIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Operator("÷")
                                )
                            )
                        }
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
                        onClick = {
                            onIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Number("7")
                                )
                            )
                        }
                    )
                    CalculatorButton(
                        text = "8",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Number("8")
                                )
                            )
                        }
                    )
                    CalculatorButton(
                        text = "9",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Number("9")
                                )
                            )
                        }
                    )
                    CalculatorButton(
                        text = "×",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFF9500),
                        onClick = {
                            onIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Operator("×")
                                )
                            )
                        }
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
                        onClick = {
                            onIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Number("4")
                                )
                            )
                        }
                    )
                    CalculatorButton(
                        text = "5",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Number("5")
                                )
                            )
                        }
                    )
                    CalculatorButton(
                        text = "6",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Number("6")
                                )
                            )
                        }
                    )
                    CalculatorButton(
                        text = "−",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFF9500),
                        onClick = {
                            onIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Operator("-")
                                )
                            )
                        }
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
                        onClick = {
                            onIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Number("1")
                                )
                            )
                        }
                    )
                    CalculatorButton(
                        text = "2",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Number("2")
                                )
                            )
                        }
                    )
                    CalculatorButton(
                        text = "3",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Number("3")
                                )
                            )
                        }
                    )
                    CalculatorButton(
                        text = "+",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFF9500),
                        onClick = {
                            onIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Operator("+")
                                )
                            )
                        }
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
                            onIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Dot
                                )
                            )
                        }
                    )
                    CalculatorButton(
                        text = "0",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Number("0")
                                )
                            )
                        }
                    )
                    DeleteCalculatorButton(
                        text = "⌫",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF9E9E9E),
                        textColor = Color.Black,
                        onDeleteAction = { onIntent(CalculatorContract.Intent.Delete) }
                    )
                    CalculatorButton(
                        text = "=",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFF9500),
                        onClick = { onIntent(CalculatorContract.Intent.Calculate) }
                    )
                }
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
        CalculatorScreen(
            state = CalculatorContract.State(
                expression = "100+200",
                cursorPosition = 2,
                previewResult = "300"
            ),
            onCursorMove = { },
            onIntent = { }
        )
    }
}