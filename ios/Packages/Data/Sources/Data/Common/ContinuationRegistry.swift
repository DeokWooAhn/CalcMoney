import Foundation

/// `AsyncStream` continuation 등록부.
///
/// 데이터소스들은 `AsyncStream` 생성부에서 등록과 해제를 각각 별도 `Task`로 액터에 보낸다.
///
/// ```swift
/// AsyncStream { continuation in
///     Task { await self.register(id: id, continuation: continuation) }
///     continuation.onTermination = { _ in Task { await self.unregister(id: id) } }
/// }
/// ```
///
/// 두 `Task`의 실행 순서는 보장되지 않아서, 스트림이 곧바로 끝나면 해제가 등록보다 먼저 도착할 수 있다.
/// 그러면 뒤늦게 등록된 continuation을 아무도 지우지 않아 등록부에 영구히 남는다.
/// 이미 해제가 도착한 id를 기억해 두었다가 등록을 거절하는 방식으로 순서 역전을 흡수한다.
struct ContinuationRegistry<Element: Sendable> {
    private var continuations: [UUID: AsyncStream<Element>.Continuation] = [:]
    private var terminatedIDs: Set<UUID> = []

    var count: Int { continuations.count }
    var isEmpty: Bool { continuations.isEmpty }

    /// 등록에 성공하면 true. 해제가 먼저 도착한 id면 continuation을 끝내고 false를 반환한다.
    mutating func register(id: UUID, continuation: AsyncStream<Element>.Continuation) -> Bool {
        if terminatedIDs.remove(id) != nil {
            continuation.finish()

            return false
        }

        continuations[id] = continuation

        return true
    }

    mutating func unregister(id: UUID) {
        // 지울 게 없으면 아직 등록 전이라는 뜻이다. 뒤늦게 등록되지 않도록 표시해 둔다.
        // 표시는 곧 도착할 register가 지우므로 무한히 쌓이지 않는다.
        if continuations.removeValue(forKey: id) == nil {
            terminatedIDs.insert(id)
        }
    }

    func yield(_ element: Element) {
        for continuation in continuations.values {
            continuation.yield(element)
        }
    }
}
