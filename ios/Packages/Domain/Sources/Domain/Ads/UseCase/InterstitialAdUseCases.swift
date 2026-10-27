import Foundation

/// 계산 성공을 전면 광고 기록에 센다. 처음 성공한 계산이면 그 시각도 남겨 유예 기간을 센다.
/// (Android `RecordInterstitialAdCalculationUseCase` 대응)
public struct RecordInterstitialAdCalculationUseCase: Sendable {
    private let repository: any InterstitialAdRepository
    private let now: @Sendable () -> Date

    public init(
        repository: any InterstitialAdRepository,
        now: @escaping @Sendable () -> Date = { Date() },
    ) {
        self.repository = repository
        self.now = now
    }

    public func callAsFunction() async {
        let now = now()

        await repository.updateRecord { record in
            record.firstCalculatedAt = record.firstCalculatedAt ?? now
            record.calculationCountSinceLastAd += 1
        }
    }
}

/// 지금 전면 광고를 보여 줄 차례인지 알려 준다. 기록은 바꾸지 않는다. (Android `ShouldShowInterstitialAdUseCase` 대응)
///
/// 광고가 실제로 떴을 때만 `RecordInterstitialAdShownUseCase`로 기록한다. 광고를 아직 받아 오지 못해
/// 못 띄웠으면 조건이 그대로 남아 다음 기회에 다시 띄운다.
public struct ShouldShowInterstitialAdUseCase: Sendable {
    private let repository: any InterstitialAdRepository
    private let calendar: Calendar
    private let now: @Sendable () -> Date

    public init(
        repository: any InterstitialAdRepository,
        calendar: Calendar = .current,
        now: @escaping @Sendable () -> Date = { Date() },
    ) {
        self.repository = repository
        self.calendar = calendar
        self.now = now
    }

    public func callAsFunction() async -> Bool {
        await InterstitialAdPolicy.shouldShow(repository.record(), now: now(), calendar: calendar)
    }
}

/// 전면 광고를 보여 줬다고 기록한다. 계산 횟수를 처음부터 다시 세고, 오늘 보여 준 횟수를 늘린다.
/// (Android `RecordInterstitialAdShownUseCase` 대응)
public struct RecordInterstitialAdShownUseCase: Sendable {
    private let repository: any InterstitialAdRepository
    private let calendar: Calendar
    private let now: @Sendable () -> Date

    public init(
        repository: any InterstitialAdRepository,
        calendar: Calendar = .current,
        now: @escaping @Sendable () -> Date = { Date() },
    ) {
        self.repository = repository
        self.calendar = calendar
        self.now = now
    }

    public func callAsFunction() async {
        let now = now()
        let calendar = calendar

        await repository.updateRecord { record in
            record.shownCountOfDay = record.shownCount(onSameDayAs: now, calendar: calendar) + 1
            record.calculationCountSinceLastAd = 0
            record.lastShownAt = now
        }
    }
}

/// 전면 광고에 쓰는 유스케이스 묶음 (Android `InterstitialAdUseCases` 대응)
public struct InterstitialAdUseCases: Sendable {
    public let recordCalculation: RecordInterstitialAdCalculationUseCase
    public let shouldShow: ShouldShowInterstitialAdUseCase
    public let recordShown: RecordInterstitialAdShownUseCase

    public init(
        recordCalculation: RecordInterstitialAdCalculationUseCase,
        shouldShow: ShouldShowInterstitialAdUseCase,
        recordShown: RecordInterstitialAdShownUseCase,
    ) {
        self.recordCalculation = recordCalculation
        self.shouldShow = shouldShow
        self.recordShown = recordShown
    }
}
