import Domain

/// 통화 코드 → 국가 코드 매핑 (국기 이모지용, Android `ExchangeRateMapper` 대응)
private let currencyCountryCodes: [String: String] = [
    "KRW": "KR",
    "USD": "US",
    "JPY": "JP",
    "EUR": "EU",
    "CNH": "CN",
    "GBP": "GB",
    "AUD": "AU",
    "CAD": "CA",
    "CHF": "CH",
    "HKD": "HK",
    "AED": "AE",
    "BHD": "BH",
    "BND": "BN",
    "DKK": "DK",
    "IDR": "ID",
    "KWD": "KW",
    "MYR": "MY",
    "NOK": "NO",
    "NZD": "NZ",
    "SAR": "SA",
    "SEK": "SE",
    "SGD": "SG",
    "THB": "TH",
]

extension [ExchangeRateData] {
    /// 환율 목록에서 지정한 통화 코드의 기준 환율을 찾는다. 기준 통화 `KRW`는 1.0으로 처리한다.
    func rate(of code: String) -> Double? {
        if code == "KRW" { return 1.0 }

        return first { $0.code == code }?.baseRate
    }
}

extension ExchangeRateData {
    func toCurrencyInfo() -> CurrencyInfo {
        CurrencyInfo(
            code: code,
            displayCode: code,
            name: currencyName,
            flagEmoji: flagEmoji(for: code),
        )
    }
}

func krwCurrencyInfo() -> CurrencyInfo {
    CurrencyInfo(
        code: "KRW",
        displayCode: "KRW",
        name: "한국 원",
        flagEmoji: flagEmoji(for: "KRW"),
    )
}

/// 세 글자 통화 코드를 해당 국가의 국기 이모지로 변환한다. 지원하지 않으면 빈 문자열.
func flagEmoji(for currencyCode: String) -> String {
    guard let countryCode = currencyCountryCodes[currencyCode] else { return "" }

    return countryCode
        .uppercased()
        .unicodeScalars
        .compactMap { scalar -> String? in
            guard let flagScalar = Unicode.Scalar(0x1F1E6 + scalar.value - Unicode.Scalar("A").value) else {
                return nil
            }

            return String(flagScalar)
        }
        .joined()
}
