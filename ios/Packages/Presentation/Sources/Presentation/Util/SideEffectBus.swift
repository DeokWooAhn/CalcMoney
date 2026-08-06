import Foundation

/// ViewModel의 일회성 이벤트를 여러 구독자에게 전달하는 버스 (Orbit `postSideEffect` 대응)
///
/// 화면이 사라졌다 다시 나타나도 `stream()`으로 새로 구독할 수 있다.
@MainActor
final class SideEffectBus<Effect: Sendable> {
    private var continuations: [UUID: AsyncStream<Effect>.Continuation] = [:]

    func stream() -> AsyncStream<Effect> {
        AsyncStream { continuation in
            let id = UUID()
            continuations[id] = continuation
            continuation.onTermination = { [weak self] _ in
                Task { @MainActor in
                    self?.continuations[id] = nil
                }
            }
        }
    }

    func send(_ effect: Effect) {
        for continuation in continuations.values {
            continuation.yield(effect)
        }
    }
}
