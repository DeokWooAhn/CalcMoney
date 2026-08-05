import Domain

/// 통화 코드별 이름의 지역화 키 (Android `CurrencyNameFormatter` 대응)
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

extension CurrencyInfo {
    /// 지역화된 통화 이름. 매핑에 없으면 서버가 준 이름을 그대로 쓴다.
    var localizedName: String {
        guard let key = currencyNameKeys[code.uppercased()] else { return name }

        return LDynamic(key)
    }
}
