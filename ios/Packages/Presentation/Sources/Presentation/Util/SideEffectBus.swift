import Foundation

private let sideEffectBufferSize = 64

/// ViewModel의 일회성 이벤트를 여러 구독자에게 전달하는 버스 (Orbit `postSideEffect` 대응)
///
/// 화면이 사라졌다 다시 나타나도 `stream()`으로 새로 구독할 수 있다.
/// 살아 있는 구독자에게 전달하지 못한 이벤트는 버퍼에 쌓아 두고 다음 구독자에게 전달한다
/// (Android Orbit의 `Channel(Channel.BUFFERED)` 동작과 맞춘 것).
///
/// `onTermination` 정리가 Task로 미뤄지므로 이미 끝난 continuation이 잠시 남아 있을 수 있다.
/// 그래서 개수만 보지 않고 `yield` 결과로 실제 전달 여부를 판단한다.
@MainActor
final class SideEffectBus<Effect: Sendable> {
    private var continuations: [UUID: AsyncStream<Effect>.Continuation] = [:]
    private var pending: [Effect] = []

    func stream() -> AsyncStream<Effect> {
        AsyncStream { continuation in
            let id = UUID()
            continuations[id] = continuation

            for effect in pending {
                continuation.yield(effect)
            }
            pending.removeAll()

            continuation.onTermination = { [weak self] _ in
                Task { @MainActor in
                    self?.continuations[id] = nil
                }
            }
        }
    }

    func send(_ effect: Effect) {
        var delivered = false

        for (id, continuation) in continuations {
            switch continuation.yield(effect) {
            case .terminated:
                continuations[id] = nil

            default:
                delivered = true
            }
        }

        guard !delivered else { return }

        if pending.count >= sideEffectBufferSize {
            pending.removeFirst()
        }
        pending.append(effect)
    }
}
