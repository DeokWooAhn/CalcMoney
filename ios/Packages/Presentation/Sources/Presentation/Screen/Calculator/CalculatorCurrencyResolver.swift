import Domain

// 계산기 화면의 기준·보조 통화를 정한다. `CalculatorViewModel`이 통화 목록을 불러올 때 쓴다.

/// 통화 목록과 화면에 있던 통화, 저장된 선택, 기기 지역 통화로 기준·보조 통화를 함께 정한다.
/// 보조 통화는 정해진 기준 통화와 겹치지 않는다.
func resolveCalculatorCurrencies(
    currencies: [CurrencyInfo],
    currentMain: CurrencyInfo?,
    currentSub: CurrencyInfo?,
    savedSelection: CalculatorCurrencySelection?,
    deviceCurrencyCode: String,
) -> (main: CurrencyInfo?, sub: CurrencyInfo?) {
    let mainCurrency = resolveCalculatorMainCurrency(
        currencies: currencies,
        currentCurrency: currentMain,
        savedSelection: savedSelection,
        deviceCurrencyCode: deviceCurrencyCode,
    )
    let subCurrency = resolveCalculatorSubCurrency(
        currencies: currencies,
        currentCurrency: currentSub,
        savedSelection: savedSelection,
        mainCurrency: mainCurrency,
    )
    return (mainCurrency, subCurrency)
}

/// 화면에 있던 통화 → 저장된 통화 → 기기 지역 통화 → 원화 → 목록 첫 통화 순으로 기준 통화를 정한다.
func resolveCalculatorMainCurrency(
    currencies: [CurrencyInfo],
    currentCurrency: CurrencyInfo?,
    savedSelection: CalculatorCurrencySelection?,
    deviceCurrencyCode: String,
) -> CurrencyInfo? {
    currencies.first { $0.code == currentCurrency?.code }
        ?? currencies.first { $0.code == savedSelection?.mainCode }
        ?? currencies.first { $0.code == deviceCurrencyCode }
        ?? currencies.first { $0.code == "KRW" }
        ?? currencies.first
}

/// 화면에 있던 통화 → 저장된 통화 → 미국 달러 → 목록 첫 통화 순으로, 기준 통화와 겹치지 않는 보조 통화를 정한다.
func resolveCalculatorSubCurrency(
    currencies: [CurrencyInfo],
    currentCurrency: CurrencyInfo?,
    savedSelection: CalculatorCurrencySelection?,
    mainCurrency: CurrencyInfo?,
) -> CurrencyInfo? {
    let mainCurrencyCode = mainCurrency?.code

    return currencies.first { $0.code == currentCurrency?.code && $0.code != mainCurrencyCode }
        ?? currencies.first { $0.code == savedSelection?.subCode && $0.code != mainCurrencyCode }
        ?? currencies.first { $0.code == "USD" && $0.code != mainCurrencyCode }
        ?? currencies.first { $0.code != mainCurrencyCode }
}
