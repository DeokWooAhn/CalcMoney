/// 통화 정보 (Android `CurrencyInfo` 대응)
public struct CurrencyInfo: Equatable, Hashable, Sendable {
    public let code: String
    public let displayCode: String
    public let name: String
    public let flagEmoji: String

    public init(code: String, displayCode: String, name: String, flagEmoji: String) {
        self.code = code
        self.displayCode = displayCode
        self.name = name
        self.flagEmoji = flagEmoji
    }
}
