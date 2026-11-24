import Foundation

/// 새 일을 시작하면 앞 일을 취소하고, 앞 일의 결과는 반영하지 않게 하는 도우미
///
/// 취소는 협조적이라 앞 일이 끝까지 돌아올 수 있다(Firestore 요청은 취소돼도 끝까지 응답한다).
/// 그래서 일마다 요청 번호를 붙이고, 번호가 가장 최근이 아니면 `isLatest()`가 `false`를 돌려준다.
/// 같은 입력으로 다시 시작해도(통화쌍 USD → EUR → USD) 번호로 구별한다.
@MainActor
final class LatestTaskRunner {
    private var latestRequestID = 0
    private var task: Task<Void, Never>?

    deinit {
        task?.cancel()
    }

    /// 진행 중인 일을 취소하고 `operation`을 새로 시작한다. 기다리지 않고 돌아온다.
    ///
    /// `operation`이 받는 `isLatest`는 그 일 안에서 부른다. 일이 취소되지 않았고 아직 가장 최근 일이면 `true`다.
    /// 결과를 반영하기 직전에 확인한다.
    func restart(_ operation: @escaping @MainActor (_ isLatest: @escaping @MainActor () -> Bool) async -> Void) {
        cancel()
        let requestID = latestRequestID

        let isLatest: @MainActor () -> Bool = { [weak self] in
            !Task.isCancelled && self?.latestRequestID == requestID
        }
        task = Task {
            await operation(isLatest)
        }
    }

    /// 진행 중인 일을 취소하고 요청 번호를 올린다. 그 일의 `isLatest()`는 이제 `false`다.
    func cancel() {
        latestRequestID += 1
        task?.cancel()
        task = nil
    }
}
