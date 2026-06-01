package com.ahn.presentation.ui.screen.calculator

import androidx.compose.animation.AnimatedVisibility as AnimatedVisibilityTopLevel
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.presentation.R
import com.ahn.presentation.ui.component.CalculatorButton
import com.ahn.presentation.ui.component.CalculatorIconButton
import com.ahn.presentation.ui.component.CurrencyPickerDialog
import com.ahn.presentation.ui.component.CustomSnackbarHost
import com.ahn.presentation.ui.component.DeleteCalculatorButton
import com.ahn.presentation.ui.theme.buttonFunction
import com.ahn.presentation.ui.theme.buttonOperator
import com.ahn.presentation.ui.theme.buttonTextSecondary
import com.ahn.presentation.ui.theme.calculatorAccent
import com.ahn.presentation.ui.theme.currencySelectorBorder
import com.ahn.presentation.ui.theme.currencySelectorSurface
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
    viewModel: CalculatorViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is CalculatorContract.SideEffect.ShowSnackBar -> {
                scope.launch {
                    snackBarHostState.showSnackbarImmediately(sideEffect.message.asString(context))
                }
            }
        }
    }

    CalculatorScreen(
        state = state,
        onIntent = viewModel::processIntent,
        onCursorMove = viewModel::updateCursorPosition,
        snackBarHostState = snackBarHostState,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CalculatorScreen(
    state: CalculatorContract.State,
    onIntent: (CalculatorContract.Intent) -> Unit,
    onCursorMove: (Int) -> Unit,
    snackBarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val focusRequester = remember { FocusRequester() }
    var showHistory by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = {
            CustomSnackbarHost(snackbarHostState = snackBarHostState)
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(bottom = 0.dp),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp, 5.dp, 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            CalculatorCurrencySelectorRow(
                state = state,
                onIntent = onIntent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 3.dp),
            )

            CalculatorDisplay(
                state = state,
                focusRequester = focusRequester,
                onCursorMove = onCursorMove,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
            )

            CalculatorKeypadArea(
                showHistory = showHistory,
                histories = state.histories,
                onHistoryClick = { showHistory = !showHistory },
                onClearHistory = { onIntent(CalculatorContract.Intent.ClearHistory) },
                onDismissHistory = { showHistory = false },
                onIntent = onIntent,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }
}

@Composable
private fun CalculatorCurrencySelectorRow(
    state: CalculatorContract.State,
    onIntent: (CalculatorContract.Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalculatorExchangeCurrencySelector(
            label = stringResource(R.string.main_exchange_currency),
            selectedCurrency = state.mainExchangeCurrency,
            availableCurrencies = state.availableCurrencies,
            favoriteCurrencyCodes = state.favoriteCurrencyCodes,
            onToggleFavorite = {
                onIntent(CalculatorContract.Intent.ToggleFavorite(it))
            },
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
                    contentDescription = stringResource(R.string.swap_exchange_currency),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        CalculatorExchangeCurrencySelector(
            label = stringResource(R.string.sub_exchange_currency),
            selectedCurrency = state.selectedExchangeCurrency,
            availableCurrencies = state.availableCurrencies,
            favoriteCurrencyCodes = state.favoriteCurrencyCodes,
            onToggleFavorite = {
                onIntent(CalculatorContract.Intent.ToggleFavorite(it))
            },
            onCurrencySelected = {
                onIntent(CalculatorContract.Intent.SelectExchangeCurrency(it))
            },
            modifier = Modifier.widthIn(min = 84.dp),
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CalculatorDisplay(
    state: CalculatorContract.State,
    focusRequester: FocusRequester,
    onCursorMove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val calculatorAccent = MaterialTheme.colorScheme.calculatorAccent
    val textFieldValue = TextFieldValue(
        text = state.expression,
        selection = TextRange(state.cursorPosition),
    )
    val dynamicFontSize = remember(state.expression.length) {
        when (state.expression.length) {
            in 0..11 -> 42.sp
            in 11..16 -> 35.sp
            else -> 30.sp
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopEnd,
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                InterceptPlatformTextInput(
                    interceptor = { _, _ ->
                        awaitCancellation()
                    },
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
                                calculatorAccent
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            },
                            fontSize = dynamicFontSize,
                            fontWeight = FontWeight.Light,
                            textAlign = TextAlign.End,
                        ),
                        visualTransformation = remember(calculatorAccent) {
                            ThousandSeparatorTransformation(calculatorAccent)
                        },
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                }

                ConvertedAmountText(
                    amount = state.convertedExpressionAmount,
                    currency = state.selectedExchangeCurrency,
                )
            }
        }

        if (state.expression.isNotEmpty() && state.previewResult.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatNumberWithCommas(state.previewResult),
                    color = Color.Gray,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )

                ConvertedAmountText(
                    amount = state.convertedPreviewAmount,
                    currency = state.selectedExchangeCurrency,
                )
            }
        }
    }
}

@Composable
private fun CalculatorKeypadArea(
    showHistory: Boolean,
    histories: List<CalculatorContract.HistoryItem>,
    onHistoryClick: () -> Unit,
    onClearHistory: () -> Unit,
    onDismissHistory: () -> Unit,
    onIntent: (CalculatorContract.Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val buttonGap = 0.dp
        val rowGap = 0.dp
        val buttonSize = ((maxWidth - buttonGap * 3) / 4).coerceAtMost(84.dp)
        val keypadWidth = buttonSize * 4 + buttonGap * 3
        val keypadStartPadding = (maxWidth - keypadWidth) / 2
        val historyWidth = 16.dp + keypadStartPadding + buttonSize * 3 + buttonGap * 3
        val historyHeight = buttonSize * 4 + rowGap * 3

        Box(modifier = Modifier.fillMaxWidth()) {
            CalculatorKeypad(
                rows = calculatorKeyRows,
                buttonSize = buttonSize,
                buttonGap = buttonGap,
                rowGap = rowGap,
                keypadWidth = keypadWidth,
                modifier = Modifier.fillMaxWidth(),
                onHistoryClick = onHistoryClick,
                onIntent = onIntent,
            )

            CalculatorHistoryOverlay(
                visible = showHistory,
                histories = histories,
                onClearHistory = onClearHistory,
                onDismiss = onDismissHistory,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-16).dp, y = buttonSize + rowGap)
                    .width(historyWidth)
                    .height(historyHeight),
            )
        }
    }
}

private sealed interface CalculatorKey {
    data object History : CalculatorKey

    data object Clear : CalculatorKey

    data object Parenthesis : CalculatorKey

    data object Dot : CalculatorKey

    data object Delete : CalculatorKey

    data object Calculate : CalculatorKey

    data class Number(val value: String) : CalculatorKey

    data class Operator(
        val displayText: String,
        val inputValue: String,
    ) : CalculatorKey
}

private val calculatorKeyRows = listOf(
    listOf(
        CalculatorKey.History,
        CalculatorKey.Clear,
        CalculatorKey.Parenthesis,
        CalculatorKey.Operator(displayText = "÷", inputValue = "÷"),
    ),
    listOf(
        CalculatorKey.Number("7"),
        CalculatorKey.Number("8"),
        CalculatorKey.Number("9"),
        CalculatorKey.Operator(displayText = "×", inputValue = "×"),
    ),
    listOf(
        CalculatorKey.Number("4"),
        CalculatorKey.Number("5"),
        CalculatorKey.Number("6"),
        CalculatorKey.Operator(displayText = "−", inputValue = "-"),
    ),
    listOf(
        CalculatorKey.Number("1"),
        CalculatorKey.Number("2"),
        CalculatorKey.Number("3"),
        CalculatorKey.Operator(displayText = "+", inputValue = "+"),
    ),
    listOf(
        CalculatorKey.Dot,
        CalculatorKey.Number("0"),
        CalculatorKey.Delete,
        CalculatorKey.Calculate,
    ),
)

@Composable
private fun CalculatorKeypad(
    rows: List<List<CalculatorKey>>,
    buttonSize: Dp,
    buttonGap: Dp,
    rowGap: Dp,
    keypadWidth: Dp,
    onHistoryClick: () -> Unit,
    onIntent: (CalculatorContract.Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(rowGap),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.width(keypadWidth),
                horizontalArrangement = Arrangement.spacedBy(buttonGap),
            ) {
                row.forEach { key ->
                    CalculatorKeyButton(
                        key = key,
                        modifier = Modifier.size(buttonSize),
                        onHistoryClick = onHistoryClick,
                        onIntent = onIntent,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculatorKeyButton(
    key: CalculatorKey,
    onHistoryClick: () -> Unit,
    onIntent: (CalculatorContract.Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (key) {
        CalculatorKey.History -> CalculatorIconButton(
            imageVector = Icons.Default.AccessTime,
            modifier = modifier,
            backgroundColor = MaterialTheme.colorScheme.buttonFunction,
            contentColor = MaterialTheme.colorScheme.buttonTextSecondary,
            contentDescription = stringResource(R.string.calculator_history),
            onClick = onHistoryClick,
        )

        CalculatorKey.Clear -> CalculatorButton(
            text = "AC",
            modifier = modifier,
            backgroundColor = MaterialTheme.colorScheme.buttonFunction,
            textColor = MaterialTheme.colorScheme.buttonTextSecondary,
            onClick = { onIntent(CalculatorContract.Intent.Clear) },
        )

        CalculatorKey.Parenthesis -> CalculatorButton(
            text = "( )",
            modifier = modifier,
            backgroundColor = MaterialTheme.colorScheme.buttonFunction,
            textColor = MaterialTheme.colorScheme.buttonTextSecondary,
            onClick = {
                onIntent(
                    CalculatorContract.Intent.Input(CalculatorToken.Parenthesis),
                )
            },
        )

        CalculatorKey.Dot -> CalculatorButton(
            text = ".",
            modifier = modifier,
            onClick = {
                onIntent(
                    CalculatorContract.Intent.Input(CalculatorToken.Dot),
                )
            },
        )

        CalculatorKey.Delete -> DeleteCalculatorButton(
            text = "⌫",
            modifier = modifier,
            backgroundColor = MaterialTheme.colorScheme.buttonFunction,
            textColor = MaterialTheme.colorScheme.buttonTextSecondary,
            onDeleteAction = { onIntent(CalculatorContract.Intent.Delete) },
        )

        CalculatorKey.Calculate -> CalculatorButton(
            text = "=",
            modifier = modifier,
            backgroundColor = MaterialTheme.colorScheme.buttonOperator,
            onClick = { onIntent(CalculatorContract.Intent.Calculate) },
        )

        is CalculatorKey.Number -> CalculatorButton(
            text = key.value,
            modifier = modifier,
            onClick = {
                onIntent(
                    CalculatorContract.Intent.Input(CalculatorToken.Number(key.value)),
                )
            },
        )

        is CalculatorKey.Operator -> CalculatorButton(
            text = key.operatorText(),
            modifier = modifier,
            backgroundColor = MaterialTheme.colorScheme.buttonOperator,
            onClick = {
                onIntent(
                    CalculatorContract.Intent.Input(CalculatorToken.Operator(key.inputValue)),
                )
            },
        )
    }
}

@Composable
private fun CalculatorKey.Operator.operatorText(): String {
    return if (inputValue == "÷") stringResource(R.string.divide) else displayText
}

@Composable
private fun CalculatorExchangeCurrencySelector(
    label: String,
    selectedCurrency: CurrencyInfo?,
    availableCurrencies: List<CurrencyInfo>,
    favoriteCurrencyCodes: List<String>,
    onToggleFavorite: (String) -> Unit,
    onCurrencySelected: (CurrencyInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    var favoriteCodesSnapshot by remember { mutableStateOf<List<String>>(emptyList()) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.currencySelectorSurface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.currencySelectorBorder,
        ),
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = availableCurrencies.isNotEmpty()) {
                    favoriteCodesSnapshot = favoriteCurrencyCodes
                    showDialog = true
                }
                .padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedCurrency?.let { "${it.flagEmoji} ${it.code}" }
                    ?: stringResource(R.string.select_currency),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (showDialog) {
        CurrencyPickerDialog(
            currencies = availableCurrencies,
            selectedCurrency = selectedCurrency,
            favoriteCurrencyCodesForSort = favoriteCodesSnapshot,
            favoriteCurrencyCodesForIcon = favoriteCurrencyCodes,
            title = stringResource(R.string.select_currency_with_label, label),
            onDismiss = { showDialog = false },
            onCurrencySelected = { currency ->
                onCurrencySelected(currency)
                showDialog = false
            },
            onToggleFavorite = onToggleFavorite,
        )
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
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibilityTopLevel(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { -it }),
        exit = slideOutHorizontally(targetOffsetX = { -it }),
    ) {
        CalculatorHistoryPanel(
            histories = histories,
            onClearHistory = onClearHistory,
            onDismiss = onDismiss,
            modifier = modifier,
        )
    }
}

/**
 * 계산 기록 목록과 기록 삭제 버튼을 표시하는 패널입니다.
 *
 * 최신 기록이 위에 보이도록 표시하며, 기록 수가 바뀌면 가장 최근 항목으로 스크롤합니다.
 *
 * @param histories 표시할 계산 기록 목록입니다.
 * @param onClearHistory 기록 삭제 버튼을 눌렀을 때 실행할 콜백입니다.
 * @param modifier 패널 외부 Surface에 적용할 Modifier입니다.
 */
@Composable
private fun CalculatorHistoryPanel(
    histories: List<CalculatorContract.HistoryItem>,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    val swipeThreshold = with(LocalDensity.current) { 48.dp.toPx() }
    var horizontalDragAmount by remember { mutableStateOf(0f) }

    LaunchedEffect(histories.size) {
        if (histories.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Surface(
        modifier = modifier.pointerInput(swipeThreshold) {
            detectHorizontalDragGestures(
                onDragStart = {
                    horizontalDragAmount = 0f
                },
                onHorizontalDrag = { change, dragAmount ->
                    horizontalDragAmount += dragAmount
                    if (dragAmount < 0) {
                        change.consume()
                    }
                },
                onDragEnd = {
                    if (horizontalDragAmount < -swipeThreshold) {
                        onDismiss()
                    }
                    horizontalDragAmount = 0f
                },
                onDragCancel = {
                    horizontalDragAmount = 0f
                },
            )
        },
        shape = RoundedCornerShape(
            topStart = 0.dp,
            bottomStart = 0.dp,
            topEnd = 20.dp,
            bottomEnd = 20.dp,
        ),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(15.dp, 10.dp),
        ) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(
                    space = 20.dp,
                    alignment = Alignment.Bottom,
                ),
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
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                        )

                        Text(
                            text = "=${formatNumberWithCommas(item.result)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }

            Button(
                onClick = onClearHistory,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(150.dp),
            ) {
                Text(
                    text = stringResource(R.string.delete_calculator_history),
                    fontSize = 14.sp,
                )
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
                mainExchangeCurrency = CurrencyInfo(
                    "KRW",
                    "KRW",
                    stringResource(R.string.preview_currency_krw),
                    "🇰🇷",
                ),
                selectedExchangeCurrency = CurrencyInfo(
                    "USD",
                    "USD",
                    stringResource(R.string.preview_currency_usd),
                    "🇺🇸",
                ),
            ),
            onCursorMove = { },
            onIntent = { },
        )
    }
}
