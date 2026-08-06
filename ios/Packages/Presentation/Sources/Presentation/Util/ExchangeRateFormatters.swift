import Foundation

/// 환율 기준일·갱신 시각 포맷터 (Android `ExchangeRateFormatters` 대응)
enum ExchangeRateFormatters {
    /// "20260804" → "2026.08.04". 8자리가 아니면 원본 그대로 반환한다.
    static func formatRateDate(_ rateDate: String) -> String {
        guard rateDate.count == 8 else { return rateDate }

        let chars = Array(rateDate)

        return "\(String(chars[0..<4])).\(String(chars[4..<6])).\(String(chars[6..<8]))"
    }

    /// epoch 밀리초 → "yyyy.MM.dd HH:mm". 0 이하이면 nil.
    static func formatFetchedAt(_ fetchedAt: Int) -> String? {
        guard fetchedAt > 0 else { return nil }

        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy.MM.dd HH:mm"
        formatter.locale = Locale(identifier: "ko_KR")

        return formatter.string(from: Date(timeIntervalSince1970: Double(fetchedAt) / 1000))
    }
}
