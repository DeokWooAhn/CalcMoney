import Domain
import Foundation

/// 통화 코드 앞 두 글자가 국가 코드가 아닌 예외 (Android `ExchangeRateMapper` 대응)
///
/// ISO 4217 통화 코드는 대부분 앞 두 글자가 ISO 3166 국가 코드라서 규칙으로 국기를 만들고,
/// 규칙이 맞지 않는 코드만 여기에 둔다. 여러 나라가 함께 쓰는 `X`로 시작하는 통화(XAF, XOF, XDR 등)는
/// 국기 하나를 고를 수 없어 `noCountryFlag`를 쓴다.
private let flagCountryCodeOverrides: [String: String] = [
    // EU는 ISO 3166 국가 코드가 아니지만 유럽연합 국기 이모지가 있다.
    "EUR": "EU",
    // 네덜란드령 안틸레스(AN)는 해체돼 국기 이모지가 없다. 현재 통용되는 퀴라소를 쓴다.
    "ANG": "CW",
    // 카리브 길더는 2025년에 ANG를 대체한 후속 통화로 발행처가 같다.
    "XCG": "CW",
]

/// 국가를 특정할 수 없는 통화에 쓰는 중립 아이콘 (Android `NO_COUNTRY_FLAG` 대응)
///
/// 빈 문자열이면 목록에서 국기 자리가 비어 통화 코드와 이름이 다른 줄보다 왼쪽으로 밀린다.
let noCountryFlag = "🌐"

private let isoCountryCodes: Set<String> = Set(Locale.Region.isoRegions.map(\.identifier))

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

/// 세 글자 통화 코드를 해당 국가의 국기 이모지로 변환한다. 국가를 특정할 수 없으면 `noCountryFlag`.
func flagEmoji(for currencyCode: String) -> String {
    guard let countryCode = flagCountryCode(for: currencyCode.uppercased()) else { return noCountryFlag }

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

private func flagCountryCode(for currencyCode: String) -> String? {
    if let override = flagCountryCodeOverrides[currencyCode] { return override }
    guard currencyCode.count == 3, !currencyCode.hasPrefix("X") else { return nil }

    let countryCode = String(currencyCode.prefix(2))

    return isoCountryCodes.contains(countryCode) ? countryCode : nil
}
