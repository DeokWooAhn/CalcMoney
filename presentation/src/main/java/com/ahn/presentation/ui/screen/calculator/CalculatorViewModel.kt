package com.ahn.presentation.ui.screen.calculator

import android.util.Log
import androidx.lifecycle.ViewModel
import com.ahn.domain.ads.usecase.InterstitialAdUseCases
import com.ahn.domain.calculator.model.CalculatorHistory
import com.ahn.domain.calculator.usecase.CalculatorUseCases
import com.ahn.domain.currency.model.CalculatorCurrencySelection
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.domain.currency.usecase.CurrencySelectionUseCases
import com.ahn.domain.exchange.usecase.ExchangeUseCases
import com.ahn.domain.favorite.usecase.FavoriteUseCases
import com.ahn.domain.review.usecase.ReviewPromptUseCases
import com.ahn.presentation.R
import com.ahn.presentation.util.UiText
import com.ahn.presentation.util.deviceCurrencyCode
import com.ahn.presentation.util.toExchangeRateErrorUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container

/**
 * 계산기 화면 ViewModel
 *
 * Orbit은 인텐트를 받은 순서대로 시작만 하고 각자 코루틴으로 돌린다. 한 인텐트가 저장·조회를 기다리는 사이
 * 다음 인텐트가 끼어들므로, 겹쳐도 순서가 지켜지도록 일을 셋으로 나눈다.
 *
 * - **확정**: 계산 결과·통화 선택은 첫 중단 지점 전에 `reduce`로 반영한다. 다음 인텐트는 반영된 상태를 읽는다.
 * - **저장**: 로컬 저장은 [persistenceMutex] 안에서 탭한 순서대로 처리한다.
 * - **조회**: 환율 조회는 기다리지 않고 따로 돌린다. 마지막 요청의 응답만 반영한다([restartRateFetch]).
 *
 * iOS `CalculatorViewModel`도 같은 동작을 지킨다. 두 플랫폼이 같은 시나리오로 검증한다(`CalculatorViewModelOrderingTest`).
 */
@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val calculatorUseCases: CalculatorUseCases,
    private val exchangeUseCases: ExchangeUseCases,
    private val favoriteUseCases: FavoriteUseCases,
    private val currencySelectionUseCases: CurrencySelectionUseCases,
    private val reviewPromptUseCases: ReviewPromptUseCases,
    private val interstitialAdUseCases: InterstitialAdUseCases,
) : ViewModel(), ContainerHost<CalculatorContract.State, CalculatorContract.SideEffect> {
    override val container = container(
        initialState = CalculatorContract.State(),
    ) {
        performLoadCurrencies()
        observeSavedData()
    }

    companion object {
        private const val MAX_NUMBER_LENGTH = 15
        private const val MAX_HISTORY_COUNT = 20
    }

    private val expressionReducer = CalculatorExpressionReducer(
        calculateExpression = calculatorUseCases.calculateExpression,
        convertExchangeAmount = exchangeUseCases.convertExchangeAmount,
        extractRepeatOperation = calculatorUseCases.extractRepeatOperation,
    )

    /**
     * 로컬 저장(기록 추가·삭제, 통화 선택, 즐겨찾기, 리뷰·광고 횟수)을 탭한 순서대로 하나씩 처리한다.
     *
     * 인텐트는 첫 중단 지점까지 이어서 실행되므로, 상태를 확정한 뒤 다른 중단 없이 곧바로 잠금을 요청하면
     * 잠금을 기다리는 순서가 탭 순서와 같다. [Mutex]는 먼저 기다린 쪽에 먼저 잠금을 준다.
     */
    private val persistenceMutex = Mutex()

    /** 가장 최근 환율 조회의 번호. 이 번호와 다른 요청의 응답은 버린다. */
    private val latestRateRequestId = AtomicLong(0L)
    private val rateFetchJob = AtomicReference<Job?>(null)

    fun updateCursorPosition(newCursorPosition: Int) = intent {
        reduce {
            state.copy(cursorPosition = newCursorPosition.coerceIn(0, state.expression.length))
        }
    }

    fun processIntent(intent: CalculatorContract.Intent) {
        when (intent) {
            is CalculatorContract.Intent.Input -> when (intent.token) {
                is CalculatorToken.Number -> handleNumberInput(intent.token.value)
                is CalculatorToken.Operator -> handleOperatorInput(intent.token.value)
                is CalculatorToken.Dot -> handleDotInput()
                is CalculatorToken.Parenthesis -> handleParenthesisInput()
            }

            is CalculatorContract.Intent.SelectExchangeCurrency -> handleSelectExchangeCurrency(intent.currency)

            is CalculatorContract.Intent.SelectMainExchangeCurrency -> handleSelectMainExchangeCurrency(intent.currency)

            is CalculatorContract.Intent.ToggleFavorite -> handleToggleFavorite(intent.currencyCode)

            is CalculatorContract.Intent.Delete -> handleDelete()

            is CalculatorContract.Intent.Clear -> handleClear()

            is CalculatorContract.Intent.Calculate -> handleCalculate()

            CalculatorContract.Intent.SwapExchangeCurrencies -> intent { performSwapExchangeCurrencies() }

            CalculatorContract.Intent.ClearHistory -> handleClearHistory()
        }
    }

    private fun handleToggleFavorite(currencyCode: String) = intent {
        persistenceMutex.withLock {
            val wasFavorite = currencyCode in state.favoriteCurrencyCodes

            runCatching {
                favoriteUseCases.toggleFavoriteCurrency(currencyCode)
            }.onSuccess {
                postSideEffect(
                    CalculatorContract.SideEffect.ShowSnackBar(
                        UiText.StringResource(
                            if (wasFavorite) R.string.favorite_removed else R.string.favorite_added,
                        ),
                    ),
                )
            }.onFailure { e ->
                if (e is CancellationException) throw e
                postSideEffect(
                    CalculatorContract.SideEffect.ShowSnackBar(
                        UiText.StringResource(R.string.favorite_change_failed),
                    ),
                )
            }
        }
    }

    private fun handleNumberInput(number: String) = intent {
        when (val result = expressionReducer.inputNumber(state, number)) {
            CalculatorExpressionResult.MaxNumberLengthExceeded -> {
                postSideEffect(
                    CalculatorContract.SideEffect.ShowSnackBar(
                        UiText.StringResource(
                            R.string.max_number_length_error,
                            listOf(MAX_NUMBER_LENGTH),
                        ),
                    ),
                )
            }

            is CalculatorExpressionResult.Updated -> reduce { result.state }
        }
    }

    private fun handleOperatorInput(operator: String) = intent {
        expressionReducer.inputOperator(state, operator)?.let { nextState ->
            reduce { nextState }
        }
    }

    private fun handleDotInput() = intent {
        expressionReducer.inputDot(state)?.let { nextState ->
            reduce { nextState }
        }
    }

    private fun handleParenthesisInput() = intent {
        reduce { expressionReducer.inputParenthesis(state) }
    }

    private fun handleDelete() = intent {
        expressionReducer.delete(state)?.let { nextState ->
            reduce { nextState }
        }
    }

    private fun handleClear() = intent {
        reduce {
            state.copy(
                expression = "",
                cursorPosition = 0,
                previewResult = "",
                convertedExpressionAmount = "",
                convertedPreviewAmount = "",
                repeatOperation = null,
                isCalculatedResult = false,
                isError = false,
                errorMessage = null,
            )
        }
    }

    /**
     * 현재 수식을 계산해 결과를 바로 보여 주고, 기록 저장은 그 뒤에 저장 순서대로 한다.
     *
     * 결과를 저장보다 먼저 확정하므로 "="를 연달아 눌러도 다음 계산이 이전 결과를 읽어 반복 연산으로 이어진다.
     * 환율 조회는 기다리지 않는다.
     */
    private fun handleCalculate() = intent {
        // reduce는 StateFlow.update라 다른 변경과 겹치면 블록을 다시 실행한다. 블록 안에서는 계산만 하고,
        // 저장·이벤트는 블록 밖에서 마지막 실행(실제로 반영된 실행)이 남긴 결과로 처리한다.
        var outcome: CalculationOutcome = CalculationOutcome.Ignored
        reduce {
            outcome = expressionReducer.calculate(state)
            when (val calculated = outcome) {
                CalculationOutcome.Ignored -> state
                is CalculationOutcome.Failed -> calculated.state
                is CalculationOutcome.Succeeded -> calculated.state
            }
        }

        when (val committed = outcome) {
            CalculationOutcome.Ignored -> Unit

            is CalculationOutcome.Failed -> postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar(
                    UiText.StringResource(R.string.invalid_expression),
                ),
            )

            is CalculationOutcome.Succeeded -> persistenceMutex.withLock { saveCalculation(committed.history) }
        }
    }

    /**
     * 계산 기록을 저장하고 계산 성공을 셉니다. [persistenceMutex] 안에서 부릅니다.
     *
     * 기록 목록에는 저장에 성공한 뒤에만 넣습니다. 저장하는 동안 목록이 그대로였을 때만 저장 전 목록에 새 기록을 붙이고,
     * 그사이 저장소의 기록 흐름이 목록을 바꿨으면 흐름을 따릅니다. 흐름이 새 기록을 먼저 내보냈을 때 중복되지 않고,
     * 늦게 도착한 기존 기록을 덮어쓰지도 않습니다. 기록 저장에 실패해도 계산 성공은 셉니다.
     */
    private suspend fun Syntax<CalculatorContract.State, CalculatorContract.SideEffect>.saveCalculation(
        history: CalculatorContract.HistoryItem,
    ) {
        val historiesBeforeSave = state.histories
        val isHistorySaved = try {
            calculatorUseCases.addHistory(CalculatorHistory(expression = history.expression, result = history.result))
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("CalculatorViewModel", "Failed to save calculator history.", e)
            postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar(
                    UiText.StringResource(R.string.calculator_history_save_failed),
                ),
            )
            false
        }

        if (isHistorySaved) {
            reduce {
                // 저장하는 동안 기록 흐름(observeSavedData)이 목록을 바꿨다면 그 목록이 저장소를 따른 것이다.
                // 덮어쓰면 그사이 들어온 기존 기록이 빠지므로 그대로 둔다. 저장소는 저장이 끝나면 새 목록을 내보낸다.
                if (state.histories == historiesBeforeSave) {
                    state.copy(histories = (historiesBeforeSave + history).takeLast(MAX_HISTORY_COUNT))
                } else {
                    state
                }
            }
        }

        recordCalculationSuccess()
    }

    /**
     * 계산 성공을 인앱 리뷰·전면 광고 기록에 세고, 리뷰를 요청할 때가 되면 [CalculatorContract.SideEffect.RequestReview]를 보냅니다.
     *
     * 결과를 화면에 반영한 뒤에 호출해 작업을 하나 끝낸 순간에 창이 뜨게 합니다. 전면 광고는 계산기를 떠날 때
     * [com.ahn.presentation.main.MainViewModel]이 정하므로 여기서는 횟수만 셉니다.
     */
    private suspend fun Syntax<CalculatorContract.State, CalculatorContract.SideEffect>.recordCalculationSuccess() {
        val shouldRequestReview = try {
            reviewPromptUseCases.recordCalculationSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("CalculatorViewModel", "Failed to record calculation for review prompt.", e)
            false
        }

        if (shouldRequestReview) {
            postSideEffect(CalculatorContract.SideEffect.RequestReview)
        }

        try {
            interstitialAdUseCases.recordCalculation()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("CalculatorViewModel", "Failed to record calculation for interstitial ad.", e)
        }
    }

    private suspend fun Syntax<CalculatorContract.State, CalculatorContract.SideEffect>.performLoadCurrencies() {
        try {
            val currencies = exchangeUseCases.getSupportedCurrencies()
            val deviceCurrencyCode = deviceCurrencyCode()
            val savedSelection = currencySelectionUseCases.getCalculatorSelectionOrNull()

            reduce {
                val mainCurrency = resolveCalculatorMainCurrency(
                    currencies = currencies,
                    currentCurrency = state.mainExchangeCurrency,
                    savedSelection = savedSelection,
                    deviceCurrencyCode = deviceCurrencyCode,
                )
                val subCurrency = resolveCalculatorSubCurrency(
                    currencies = currencies,
                    currentCurrency = state.selectedExchangeCurrency,
                    savedSelection = savedSelection,
                    mainCurrency = mainCurrency,
                )

                state.copy(
                    availableCurrencies = currencies,
                    mainExchangeCurrency = mainCurrency,
                    selectedExchangeCurrency = subCurrency,
                )
            }

            restartRateFetch(state.mainExchangeCurrency, state.selectedExchangeCurrency)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar(
                    e.toExchangeRateErrorUiText(),
                ),
            )

            Log.e(
                "CalculatorViewModel",
                "Failed to load currency list.",
                e,
            )
        }
    }

    /**
     * 통화쌍의 환율 조회를 새로 시작한다. 진행 중인 조회는 취소하고 요청 번호를 올린다.
     *
     * 조회를 기다리지 않고 돌아오므로 계산·저장이 네트워크 응답에 묶이지 않는다. 취소가 늦어 이전 응답이 도착해도
     * 요청 번호가 달라 반영하지 않는다. 통화쌍이 같아도(USD → EUR → USD) 번호로 구별한다.
     */
    private fun restartRateFetch(from: CurrencyInfo?, to: CurrencyInfo?) {
        val requestId = latestRateRequestId.incrementAndGet()
        val nextJob = if (from != null && to != null) {
            intent { performFetchExchangeRate(requestId, from, to) }
        } else {
            null
        }

        rateFetchJob.getAndSet(nextJob)?.cancel()
    }

    private suspend fun Syntax<CalculatorContract.State, CalculatorContract.SideEffect>.performFetchExchangeRate(
        requestId: Long,
        from: CurrencyInfo,
        to: CurrencyInfo,
    ) {
        try {
            val rate = if (from.code == to.code) {
                1.0
            } else {
                exchangeUseCases.getExchangeRate(from = from.code, to = to.code)
            }

            reduce {
                if (state.isLatestRateRequest(requestId, latestRateRequestId.get(), from, to)) {
                    expressionReducer.run { state.copy(exchangeRate = rate).withConvertedAmounts() }
                } else {
                    state
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (state.isLatestRateRequest(requestId, latestRateRequestId.get(), from, to)) {
                postSideEffect(
                    CalculatorContract.SideEffect.ShowSnackBar(
                        e.toExchangeRateErrorUiText(),
                    ),
                )
            }
        }
    }

    private suspend fun Syntax<
        CalculatorContract.State,
        CalculatorContract.SideEffect,
    >.performSwapExchangeCurrencies() {
        val from = state.mainExchangeCurrency ?: return
        val to = state.selectedExchangeCurrency ?: return

        reduce {
            state.copy(
                mainExchangeCurrency = to,
                selectedExchangeCurrency = from,
                exchangeRate = 0.0,
                convertedExpressionAmount = "",
                convertedPreviewAmount = "",
            )
        }
        restartRateFetch(to, from)

        persistenceMutex.withLock {
            saveCalculatorSelectionBestEffort {
                currencySelectionUseCases.saveCalculatorSelection(
                    mainCode = to.code,
                    subCode = from.code,
                )
            }
        }
    }

    private fun handleSelectMainExchangeCurrency(currency: CurrencyInfo) = intent {
        if (currency.code == state.mainExchangeCurrency?.code) return@intent

        reduce {
            state.copy(
                mainExchangeCurrency = currency,
                exchangeRate = 0.0,
                convertedExpressionAmount = "",
                convertedPreviewAmount = "",
            )
        }
        restartRateFetch(state.mainExchangeCurrency, state.selectedExchangeCurrency)

        persistenceMutex.withLock {
            saveCalculatorSelectionBestEffort {
                currencySelectionUseCases.saveCalculatorMainCurrency(currency.code)
            }
        }
    }

    private fun handleSelectExchangeCurrency(currency: CurrencyInfo) = intent {
        if (currency.code == state.selectedExchangeCurrency?.code) return@intent

        reduce {
            state.copy(
                selectedExchangeCurrency = currency,
                exchangeRate = 0.0,
                convertedExpressionAmount = "",
                convertedPreviewAmount = "",
            )
        }
        restartRateFetch(state.mainExchangeCurrency, state.selectedExchangeCurrency)

        persistenceMutex.withLock {
            saveCalculatorSelectionBestEffort {
                currencySelectionUseCases.saveCalculatorSubCurrency(currency.code)
            }
        }
    }

    private fun handleClearHistory() = intent {
        persistenceMutex.withLock {
            runCatching {
                calculatorUseCases.clearHistory()
            }.onSuccess {
                reduce { state.copy(histories = emptyList()) }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                Log.e("CalculatorViewModel", "Failed to clear calculator history.", e)
                postSideEffect(
                    CalculatorContract.SideEffect.ShowSnackBar(
                        UiText.StringResource(R.string.calculator_history_delete_failed),
                    ),
                )
            }
        }
    }

    private fun observeSavedData() = intent {
        combine(
            calculatorUseCases.getHistory(),
            favoriteUseCases.getFavoriteCurrencies(),
        ) { histories, favoriteCodes ->
            histories to favoriteCodes
        }.collect { (histories, favoriteCodes) ->
            reduce {
                state.copy(
                    histories = histories.map {
                        CalculatorContract.HistoryItem(
                            expression = it.expression,
                            result = it.result,
                        )
                    },
                    favoriteCurrencyCodes = favoriteCodes,
                )
            }
        }
    }
}

/** 응답이 가장 최근 요청의 것이고, 그사이 통화쌍도 바뀌지 않았는지 본다. */
private fun CalculatorContract.State.isLatestRateRequest(
    requestId: Long,
    latestRequestId: Long,
    from: CurrencyInfo,
    to: CurrencyInfo,
): Boolean {
    return requestId == latestRequestId &&
        mainExchangeCurrency?.code == from.code &&
        selectedExchangeCurrency?.code == to.code
}

private suspend fun CurrencySelectionUseCases.getCalculatorSelectionOrNull(): CalculatorCurrencySelection? {
    return try {
        getCalculatorSelection()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("CalculatorViewModel", "Failed to load saved calculator currency selection.", e)
        null
    }
}

private suspend fun saveCalculatorSelectionBestEffort(action: suspend () -> Unit) {
    try {
        action()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("CalculatorViewModel", "Failed to save calculator currency selection.", e)
    }
}

private fun resolveCalculatorMainCurrency(
    currencies: List<CurrencyInfo>,
    currentCurrency: CurrencyInfo?,
    savedSelection: CalculatorCurrencySelection?,
    deviceCurrencyCode: String,
): CurrencyInfo? {
    return currencies.find { it.code == currentCurrency?.code }
        ?: currencies.find { it.code == savedSelection?.mainCode }
        ?: currencies.find { it.code == deviceCurrencyCode }
        ?: currencies.find { it.code == "KRW" }
        ?: currencies.firstOrNull()
}

private fun resolveCalculatorSubCurrency(
    currencies: List<CurrencyInfo>,
    currentCurrency: CurrencyInfo?,
    savedSelection: CalculatorCurrencySelection?,
    mainCurrency: CurrencyInfo?,
): CurrencyInfo? {
    val mainCurrencyCode = mainCurrency?.code

    return currencies.find { it.code == currentCurrency?.code && it.code != mainCurrencyCode }
        ?: currencies.find { it.code == savedSelection?.subCode && it.code != mainCurrencyCode }
        ?: currencies.find { it.code == "USD" && it.code != mainCurrencyCode }
        ?: currencies.firstOrNull { it.code != mainCurrencyCode }
}
