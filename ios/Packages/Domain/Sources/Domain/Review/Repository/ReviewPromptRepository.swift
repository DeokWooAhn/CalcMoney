/// 인앱 리뷰 요청 기록 저장소 (Android `ReviewPromptRepository` 대응)
public protocol ReviewPromptRepository: Sendable {
    /// 저장된 기록을 `transform`으로 고쳐 저장하고, `transform`이 돌려준 값을 반환한다.
    ///
    /// 읽고 고쳐 쓰는 동안 다른 갱신이 끼어들지 않도록 한 번에 처리한다.
    func updateRecord<Result: Sendable>(
        _ transform: @Sendable (inout ReviewPromptRecord) -> Result,
    ) async -> Result
}
