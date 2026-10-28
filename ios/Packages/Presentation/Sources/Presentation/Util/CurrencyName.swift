import Domain
import Foundation

/// 통화 코드별 이름의 지역화 키 (Android `CurrencyNameFormatter` 대응)
///
/// 직접 다듬은 번역이 있는 통화만 둔다. 나머지는 플랫폼의 현지화 이름을 쓴다.
///
/// 값은 한국어 소스 문자열이자 String Catalog의 키다.
private let currencyNameKeys: [String: String] = [
    "KRW": "대한민국 원",
    "USD": "미국 달러",
    "JPY": "일본 엔",
    "EUR": "유로",
    "CNH": "중국 위안",
    "GBP": "영국 파운드",
    "AUD": "호주 달러",
    "CAD": "캐나다 달러",
    "CHF": "스위스 프랑",
    "HKD": "홍콩 달러",
    "AED": "아랍에미리트 디르함",
    "BHD": "바레인 디나르",
    "BND": "브루나이 달러",
    "DKK": "덴마크 크로네",
    "IDR": "인도네시아 루피아",
    "KWD": "쿠웨이트 디나르",
    "MYR": "말레이시아 링깃",
    "NOK": "노르웨이 크로네",
    "NZD": "뉴질랜드 달러",
    "SAR": "사우디아라비아 리얄",
    "SEK": "스웨덴 크로나",
    "SGD": "싱가포르 달러",
    "THB": "태국 바트",
]

/// 플랫폼이 이름을 모를 수 있는 통화의 예비 이름 키 (Android `fallbackCurrencyNameResIds` 대응)
///
/// ISO 4217에 없는 지역 통화와, 최소 지원 OS가 아직 모르는 최근 통화다(iOS 18은 XCG·ZWG를 모른다).
/// 플랫폼 이름이 있으면 그쪽을 먼저 쓴다. 한국어는 서버의 이름과 같다.
private let fallbackCurrencyNameKeys: [String: String] = [
    "FOK": "페로 제도 크로나",
    "GGP": "건지 파운드",
    "IMP": "맨섬 파운드",
    "JEP": "저지 파운드",
    "KID": "키리바시 달러",
    "TVD": "투발루 달러",
    "MRU": "모리타니 우기야",
    "SLE": "시에라리온 리온",
    "STN": "상투메 프린시페 도브라",
    "VES": "베네수엘라 볼리바르",
    "XCG": "카리브 길더",
    "ZWG": "짐바브웨 골드",
]

/// String Catalog가 고른 화면 언어와 같은 로케일. 플랫폼 통화 이름도 이 언어로 맞춘다.
private let displayLocale = Locale(identifier: Bundle.module.preferredLocalizations.first ?? "ko")

extension CurrencyInfo {
    /// 지역화된 통화 이름.
    ///
    /// 직접 다듬은 번역 → 플랫폼 현지화 이름 → 예비 이름 → 서버가 준 이름 순으로 쓴다.
    var localizedName: String {
        localizedName(locale: displayLocale)
    }

    /// `locale` 화면 언어로 본 통화 이름. 플랫폼 이름과 서버 이름을 쓸지가 이 로케일로 정해진다.
    func localizedName(locale: Locale) -> String {
        if let key = currencyNameKeys[code.uppercased()] {
            return LDynamic(key)
        }

        if let name = platformCurrencyName(code, locale: locale) {
            return name
        }

        if let key = fallbackCurrencyNameKeys[code.uppercased()] {
            return LDynamic(key)
        }

        // 서버 이름은 한국어라 한국어 화면에서만 쓴다. 다른 언어에서는 목록에 한국어가 섞이지 않게 코드를 보여 준다.
        return locale.language.languageCode == .korean ? name : displayCode
    }
}

/// 플랫폼에서 통화 이름을 찾는다. 모르는 통화(ISO 4217에 없는 지역 통화 등)면 `nil`.
///
/// 플랫폼은 모르는 통화에 코드를 그대로 돌려주기도 하므로 그 경우도 `nil`로 본다.
func platformCurrencyName(_ code: String, locale: Locale) -> String? {
    guard let name = locale.localizedString(forCurrencyCode: code.uppercased()), !name.isEmpty else { return nil }

    return name.caseInsensitiveCompare(code) == .orderedSame ? nil : name
}
