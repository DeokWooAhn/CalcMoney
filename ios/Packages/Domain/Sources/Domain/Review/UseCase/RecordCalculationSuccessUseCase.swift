import Foundation

/// 계산 성공을 기록하고, 지금 리뷰를 요청해야 하는지 알려 준다 (Android `RecordCalculationSuccessUseCase` 대응)
///
/// 요청해야 하면 요청 시각도 함께 남긴다. 창이 실제로 떴는지는 OS가 알려 주지 않으므로
/// 요청을 보낸 시점을 기준으로 다음 요청 간격을 센다.
public struct RecordCalculationSuccessUseCase: Sendable {
    private let repository: any ReviewPromptRepository
    private let now: @Sendable () -> Date

    public init(
        repository: any ReviewPromptRepository,
        now: @escaping @Sendable () -> Date = { Date() },
    ) {
        self.repository = repository
        self.now = now
    }

    /// 지금 리뷰를 요청해야 하면 true를 반환한다.
    public func callAsFunction() async -> Bool {
        let now = now()

        return await repository.updateRecord { record in
            record.calculationSuccessCount += 1

            guard ReviewPromptPolicy.shouldRequest(record, now: now) else { return false }

            record.lastRequestedAt = now
            return true
        }
    }
}
