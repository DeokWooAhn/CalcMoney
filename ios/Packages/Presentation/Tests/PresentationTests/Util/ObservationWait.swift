import Observation
import os

/// 조건이 참이 될 때까지 기다린다. 조건 안에서 읽은 Observable 프로퍼티가 바뀔 때마다 다시 확인한다.
///
/// 고정 시간 대기는 CI에서 앱 호스트의 메인 스레드가 오래 잡혀 있으면 깨진다.
/// 조건이 끝내 참이 되지 않으면 스위트의 `.timeLimit`이 테스트를 취소하고, 그때 대기를 풀고 `false`를 반환한다.
@MainActor
@discardableResult
func waitUntil(_ condition: () -> Bool) async -> Bool {
    while !condition() {
        if Task.isCancelled { return false }

        let resumer = OnceResumer()
        await withTaskCancellationHandler {
            await withCheckedContinuation { continuation in
                resumer.setContinuation(continuation)
                withObservationTracking {
                    _ = condition()
                } onChange: {
                    resumer.resume()
                }
            }
        } onCancel: {
            resumer.resume()
        }
    }
    return true
}

/// 관찰 변경 알림과 취소 중 먼저 온 쪽만 continuation을 재개한다.
/// 취소가 continuation을 받기 전에 오면, continuation을 받는 즉시 재개한다.
/// 관찰 알림과 취소 처리기는 MainActor 밖에서도 불리므로 `nonisolated`로 둔다.
private final nonisolated class OnceResumer: Sendable {
    private struct State {
        var continuation: CheckedContinuation<Void, Never>?
        var isResumed = false
    }

    private let state = OSAllocatedUnfairLock(initialState: State())

    func setContinuation(_ continuation: CheckedContinuation<Void, Never>) {
        let alreadyResumed = state.withLock { state in
            if state.isResumed { return true }
            state.continuation = continuation
            return false
        }
        if alreadyResumed { continuation.resume() }
    }

    func resume() {
        let continuation = state.withLock { state -> CheckedContinuation<Void, Never>? in
            guard !state.isResumed else { return nil }
            state.isResumed = true
            defer { state.continuation = nil }
            return state.continuation
        }
        continuation?.resume()
    }
}
