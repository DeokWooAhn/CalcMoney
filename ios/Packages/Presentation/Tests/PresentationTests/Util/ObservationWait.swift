import Observation

/// 조건이 참이 될 때까지 기다린다. 조건 안에서 읽은 Observable 프로퍼티가 바뀔 때마다 다시 확인한다.
///
/// 고정 시간 대기는 CI에서 앱 호스트의 메인 스레드가 오래 잡혀 있으면 깨진다.
/// 조건이 끝내 참이 되지 않으면 스위트의 `.timeLimit`에 걸려 실패한다.
@MainActor
func waitUntil(_ condition: () -> Bool) async {
    while !condition() {
        await withCheckedContinuation { continuation in
            withObservationTracking {
                _ = condition()
            } onChange: {
                continuation.resume()
            }
        }
    }
}
