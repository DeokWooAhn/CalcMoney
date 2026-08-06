import Foundation

/// 즐겨찾기 통화 코드를 UserDefaults에 저장하는 데이터소스 (Android `FavoriteCurrencyDataSource` 대응)
public actor FavoriteCurrencyDataSource {
    private static let favoriteCodesKey = "favorite_codes"

    private let userDefaults: UserDefaults
    private var registry = ContinuationRegistry<[String]>()

    public init(userDefaults: UserDefaults = .suite(named: "favorite_currencies")) {
        self.userDefaults = userDefaults
    }

    public nonisolated func favoriteCodes() -> AsyncStream<[String]> {
        AsyncStream { continuation in
            let id = UUID()

            Task { await self.register(id: id, continuation: continuation) }

            continuation.onTermination = { _ in
                Task { await self.unregister(id: id) }
            }
        }
    }

    public func addFavorite(_ code: String) {
        let current = currentCodes()

        if !current.contains(code) {
            userDefaults.set((current + [code]).joined(separator: ","), forKey: Self.favoriteCodesKey)
            broadcast()
        }
    }

    public func removeFavorite(_ code: String) {
        let current = currentCodes()
        userDefaults.set(current.filter { $0 != code }.joined(separator: ","), forKey: Self.favoriteCodesKey)
        broadcast()
    }

    public func isFavorite(_ code: String) -> Bool {
        currentCodes().contains(code)
    }

    private func currentCodes() -> [String] {
        let raw = userDefaults.string(forKey: Self.favoriteCodesKey) ?? ""

        return raw.isEmpty ? [] : raw.components(separatedBy: ",")
    }

    private func register(id: UUID, continuation: AsyncStream<[String]>.Continuation) {
        guard registry.register(id: id, continuation: continuation) else { return }

        continuation.yield(currentCodes())
    }

    private func unregister(id: UUID) {
        registry.unregister(id: id)
    }

    private func broadcast() {
        registry.yield(currentCodes())
    }
}
