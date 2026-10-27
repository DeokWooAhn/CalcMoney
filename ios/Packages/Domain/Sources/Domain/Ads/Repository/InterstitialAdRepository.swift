/// 전면 광고 기록 저장소 (Android `InterstitialAdRepository` 대응)
public protocol InterstitialAdRepository: Sendable {
    func record() async -> InterstitialAdRecord

    /// 저장된 기록을 `transform`으로 고쳐 저장한다.
    ///
    /// 읽고 고쳐 쓰는 동안 다른 갱신이 끼어들지 않도록 한 번에 처리한다.
    func updateRecord(_ transform: @Sendable (inout InterstitialAdRecord) -> Void) async
}
