/// 인앱 리뷰 요청에 쓰는 유스케이스 묶음 (Android `ReviewPromptUseCases` 대응)
public struct ReviewPromptUseCases: Sendable {
    public let recordAppOpen: RecordAppOpenUseCase
    public let recordCalculationSuccess: RecordCalculationSuccessUseCase

    public init(recordAppOpen: RecordAppOpenUseCase, recordCalculationSuccess: RecordCalculationSuccessUseCase) {
        self.recordAppOpen = recordAppOpen
        self.recordCalculationSuccess = recordCalculationSuccess
    }
}
