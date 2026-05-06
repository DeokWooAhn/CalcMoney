package com.ahn.presentation.ui.screen.calculator

import androidx.compose.animation.AnimatedVisibility as AnimatedVisibilityTopLevel
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahn.domain.model.CurrencyInfo
import com.ahn.presentation.R
import com.ahn.presentation.ui.component.CalculatorButton
import com.ahn.presentation.ui.component.CalculatorIconButton
import com.ahn.presentation.ui.component.CustomSnackbarHost
import com.ahn.presentation.ui.component.DeleteCalculatorButton
import com.ahn.presentation.ui.theme.buttonFunction
import com.ahn.presentation.ui.theme.buttonOperator
import com.ahn.presentation.ui.theme.buttonTextSecondary
import com.ahn.presentation.util.ThousandSeparatorTransformation
import com.ahn.presentation.util.formatNumberWithCommas
import com.ahn.presentation.util.showSnackbarImmediately
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun CalculatorRoute(
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val state by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is CalculatorContract.SideEffect.ShowSnackBar -> {
                scope.launch {
                    snackbarHostState.showSnackbarImmediately(sideEffect.message)
                }
            }
        }
    }

    CalculatorScreen(
        state = state,
        onIntent = viewModel::processIntent,
        onCursorMove = viewModel::updateCursorPosition,
        snackBarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CalculatorScreen(
    state: CalculatorContract.State,
    onIntent: (CalculatorContract.Intent) -> Unit,
    onCursorMove: (Int) -> Unit,
    snackBarHostState: SnackbarHostState = remember { SnackbarHostState() }
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

    var showHistory by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = {
            CustomSnackbarHost(snackbarHostState = snackBarHostState)
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp, 5.dp, 16.dp, 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CalculatorExchangeCurrencySelector(
                    label = "메인 환율",
                    selectedCurrency = state.mainExchangeCurrency,
                    availableCurrencies = state.availableCurrencies,
                    onCurrencySelected = {
                        onIntent(CalculatorContract.Intent.SelectMainExchangeCurrency(it))
                    },
                    modifier = Modifier.widthIn(min = 84.dp),
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    IconButton(
                        onClick = {
                            onIntent(CalculatorContract.Intent.SwapExchangeCurrencies)
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "환율 통화 교환",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                CalculatorExchangeCurrencySelector(
                    label = "보조 환율",
                    selectedCurrency = state.selectedExchangeCurrency,
                    availableCurrencies = state.availableCurrencies,
                    onCurrencySelected = {
                        onIntent(CalculatorContract.Intent.SelectExchangeCurrency(it))
                    },
                    modifier = Modifier.widthIn(min = 84.dp),
                )
            }

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
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.fillMaxWidth(),
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
                                    color = if (state.isCalculatedResult) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onBackground
                                    },
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

                        ConvertedAmountText(
                            amount = state.convertedExpressionAmount,
                            currency = state.selectedExchangeCurrency,
                        )
                    }
                }

                // 2. 결과 미리보기
                if (state.expression.isNotEmpty() && state.previewResult.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatNumberWithCommas(state.previewResult),
                            color = Color.Gray,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        ConvertedAmountText(
                            amount = state.convertedPreviewAmount,
                            currency = state.selectedExchangeCurrency,
                        )
                    }
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
                        imageVector = Icons.Default.AccessTime,
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = Color.Black,
                        contentDescription = "calculator history",
                        onClick = { showHistory = !showHistory }
                    )
                    CalculatorButton(
                        text = "AC",
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.buttonFunction,
                        textColor = MaterialTheme.colorScheme.buttonTextSecondary,
                        onClick = { onIntent(CalculatorContract.Intent.Clear) }
                    )
                    CalculatorButton(
                        text = "( )",
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.buttonFunction,
                        textColor = MaterialTheme.colorScheme.buttonTextSecondary,
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
                        backgroundColor = MaterialTheme.colorScheme.buttonOperator,
                        onClick = {
                            onIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Operator("÷")
                                )
                            )
                        }
                    )
                }

                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val buttonGap = 12.dp
                    val rowGap = 6.dp
                    val buttonSize = (maxWidth - buttonGap * 3) / 4
                    val historyWidth = buttonSize * 3 + buttonGap * 2
                    val historyHeight = buttonSize * 4 + rowGap * 3

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(historyHeight)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
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
                                    backgroundColor = MaterialTheme.colorScheme.buttonOperator,
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
                                    backgroundColor = MaterialTheme.colorScheme.buttonOperator,
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
                                    backgroundColor = MaterialTheme.colorScheme.buttonOperator,
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
                                    backgroundColor = MaterialTheme.colorScheme.buttonFunction,      // ✅ 변경
                                    textColor = MaterialTheme.colorScheme.buttonTextSecondary,
                                    onDeleteAction = { onIntent(CalculatorContract.Intent.Delete) }
                                )
                                CalculatorButton(
                                    text = "=",
                                    modifier = Modifier.weight(1f),
                                    backgroundColor = MaterialTheme.colorScheme.buttonOperator,
                                    onClick = { onIntent(CalculatorContract.Intent.Calculate) }
                                )
                            }
                        }

                        CalculatorHistoryOverlay(
                            visible = showHistory,
                            histories = state.histories,
                            onClearHistory = { onIntent(CalculatorContract.Intent.ClearHistory) },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .width(historyWidth)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }
}

@Composable
private fun CalculatorExchangeCurrencySelector(
    label: String,
    selectedCurrency: CurrencyInfo?,
    availableCurrencies: List<CurrencyInfo>,
    onCurrencySelected: (CurrencyInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = availableCurrencies.isNotEmpty()) { showDialog = true }
                .padding(horizontal = 15.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedCurrency?.let { "${it.flagEmoji} ${it.code}" }
                    ?: "통화 선택",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
            ) {
                Column {
                    Text(
                        text = "$label 선택",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(20.dp),
                    )

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                    LazyColumn {
                        items(availableCurrencies, key = { it.code }) { currency ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCurrencySelected(currency)
                                        showDialog = false
                                    }
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(text = currency.flagEmoji, fontSize = 24.sp)

                                Column {
                                    Text(
                                        text = currency.displayCode,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = currency.name,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConvertedAmountText(
    amount: String,
    currency: CurrencyInfo?,
) {
    if (amount.isEmpty() || currency == null) return

    Text(
        text = "≈ ${formatNumberWithCommas(amount)}",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CalculatorHistoryOverlay(
    visible: Boolean,
    histories: List<CalculatorContract.HistoryItem>,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibilityTopLevel(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
    ) {
        CalculatorHistoryPanel(
            histories = histories,
            onClearHistory = onClearHistory,
            modifier = modifier
        )
    }
}

@Composable
private fun CalculatorHistoryPanel(
    histories: List<CalculatorContract.HistoryItem>,
    onClearHistory: () -> Unit,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(histories.size) {
        if (histories.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(
                    items = histories.asReversed(),
                ) { item ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = formatNumberWithCommas(item.expression),
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                        )

                        Text(
                            text = "=${formatNumberWithCommas(item.result)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }

            Button(
                onClick = onClearHistory,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "계산 기록 삭제")
            }
        }
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
                previewResult = "300",
                mainExchangeCurrency = CurrencyInfo("KRW", "KRW", "대한민국 원", "🇰🇷"),
                selectedExchangeCurrency = CurrencyInfo("USD", "USD", "미국 달러", "🇺🇸"),
            ),
            onCursorMove = { },
            onIntent = { }
        )
    }
}
