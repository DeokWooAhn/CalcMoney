import Domain
import Foundation

/// UserDefaults에 계산 기록을 저장하는 데이터소스 (Android `CalculatorHistoryDataSource` 대응)
///
/// Android DataStore와 같은 인코딩(구분자 기반 문자열, 최대 20개)을 사용한다.
public actor CalculatorHistoryDataSource {
    private static let historiesKey = "histories"
    private static let itemSeparator = "\u{1E}"
    private static let fieldSeparator = "\u{1F}"
    private static let maxHistoryCount = 20

    private let userDefaults: UserDefaults
    private var continuations: [UUID: AsyncStream<[CalculatorHistory]>.Continuation] = [:]

    public init(userDefaults: UserDefaults = .suite(named: "calculator_history")) {
        self.userDefaults = userDefaults
    }

    public nonisolated func histories() -> AsyncStream<[CalculatorHistory]> {
        AsyncStream { continuation in
            let id = UUID()

            Task { await self.register(id: id, continuation: continuation) }

            continuation.onTermination = { _ in
                Task { await self.unregister(id: id) }
            }
        }
    }

    public func addHistory(_ history: CalculatorHistory) {
        let current = decodeHistories(userDefaults.string(forKey: Self.historiesKey) ?? "")
        userDefaults.set(encodeHistories(current + [history]), forKey: Self.historiesKey)
        broadcast()
    }

    public func clearHistories() {
        userDefaults.set("", forKey: Self.historiesKey)
        broadcast()
    }

    private func currentHistories() -> [CalculatorHistory] {
        decodeHistories(userDefaults.string(forKey: Self.historiesKey) ?? "")
    }

    private func encodeHistories(_ histories: [CalculatorHistory]) -> String {
        histories.suffix(Self.maxHistoryCount)
            .map { "\($0.expression)\(Self.fieldSeparator)\($0.result)" }
            .joined(separator: Self.itemSeparator)
    }

    private func decodeHistories(_ raw: String) -> [CalculatorHistory] {
        if raw.isEmpty { return [] }

        return raw.components(separatedBy: Self.itemSeparator).compactMap { item in
            let parts = item.split(separator: Self.fieldSeparator, maxSplits: 1, omittingEmptySubsequences: false)
            guard parts.count == 2 else { return nil }

            return CalculatorHistory(expression: String(parts[0]), result: String(parts[1]))
        }
    }

    private func register(id: UUID, continuation: AsyncStream<[CalculatorHistory]>.Continuation) {
        continuations[id] = continuation
        continuation.yield(currentHistories())
    }

    private func unregister(id: UUID) {
        continuations[id] = nil
    }

    private func broadcast() {
        let histories = currentHistories()

        for continuation in continuations.values {
            continuation.yield(histories)
        }
    }
}
