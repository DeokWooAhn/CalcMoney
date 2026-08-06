import Foundation

/// 패키지 번들에서 지역화 문자열을 찾는다. 소스 언어는 한국어 (Android `values-ko` 대응).
func L(_ keyAndValue: String.LocalizationValue) -> String {
    String(localized: keyAndValue, bundle: .module)
}

/// 변수로 들어오는 키의 지역화 — 카탈로그에 없으면 키 그대로 반환된다.
func LDynamic(_ key: String) -> String {
    String(localized: String.LocalizationValue(key), bundle: .module)
}
