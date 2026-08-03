import Testing
@testable import Domain

@Suite("ToggleFavoriteCurrencyUseCase")
struct ToggleFavoriteCurrencyUseCaseTests {
    @Test("선택한 통화가 즐겨찾기에 없는 상태에서 토글하면 즐겨찾기에 추가해야 한다")
    func 없는_통화_토글은_추가() async throws {
        let repository = FavoriteCurrencyRepositoryStub(isFavoriteResult: false)
        let useCase = ToggleFavoriteCurrencyUseCase(repository: repository)

        try await useCase("USD")

        #expect(repository.isFavoriteCalls == ["USD"])
        #expect(repository.addedCodes == ["USD"])
        #expect(repository.removedCodes.isEmpty)
    }

    @Test("선택한 통화가 이미 즐겨찾기에 있는 상태에서 토글하면 즐겨찾기에서 제거해야 한다")
    func 있는_통화_토글은_제거() async throws {
        let repository = FavoriteCurrencyRepositoryStub(isFavoriteResult: true)
        let useCase = ToggleFavoriteCurrencyUseCase(repository: repository)

        try await useCase("USD")

        #expect(repository.isFavoriteCalls == ["USD"])
        #expect(repository.removedCodes == ["USD"])
        #expect(repository.addedCodes.isEmpty)
    }
}

/// 즐겨찾기 호출을 기록하는 테스트 스텁 (Android MockK 대응)
private final class FavoriteCurrencyRepositoryStub: FavoriteCurrencyRepository, @unchecked Sendable {
    private let isFavoriteResult: Bool
    private(set) var isFavoriteCalls: [String] = []
    private(set) var addedCodes: [String] = []
    private(set) var removedCodes: [String] = []

    init(isFavoriteResult: Bool) {
        self.isFavoriteResult = isFavoriteResult
    }

    func favoriteCurrencyCodes() -> AsyncStream<[String]> {
        AsyncStream { $0.finish() }
    }

    func addFavorite(_ currencyCode: String) async throws {
        addedCodes.append(currencyCode)
    }

    func removeFavorite(_ currencyCode: String) async throws {
        removedCodes.append(currencyCode)
    }

    func isFavorite(_ currencyCode: String) async throws -> Bool {
        isFavoriteCalls.append(currencyCode)
        return isFavoriteResult
    }
}
