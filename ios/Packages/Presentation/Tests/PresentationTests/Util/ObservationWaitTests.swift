import Observation
import Testing

@MainActor
@Observable
private final class Counter {
    var value = 0
}

@MainActor
@Suite("waitUntil", .timeLimit(.minutes(1)))
struct ObservationWaitTests {
    @Test("조건이 이미 참이면 기다리지 않고 true를 반환한다")
    func 조건이_이미_참이면_곧바로_true를_반환한다() async {
        let counter = Counter()
        counter.value = 1

        let isSatisfied = await waitUntil { counter.value > 0 }

        #expect(isSatisfied)
    }

    @Test("관찰하는 값이 바뀌어 조건이 참이 되면 true를 반환한다")
    func 값이_바뀌어_조건이_참이_되면_true를_반환한다() async {
        let counter = Counter()
        let waiting = Task { await waitUntil { counter.value >= 2 } }

        // MainActor 작업은 들어온 순서대로 실행되므로, 이 뒤에 넣은 빈 작업이 끝나면 대기가 이미 시작돼 있다.
        await Task { @MainActor in }.value
        counter.value = 1
        counter.value = 2

        #expect(await waiting.value)
    }

    @Test("대기 중에 취소되면 멈추지 않고 false를 반환하며, 이어진 값 변경이 continuation을 다시 재개하지 않는다")
    func 대기_중_취소되면_false를_반환한다() async {
        let counter = Counter()
        let waiting = Task { await waitUntil { counter.value > 0 } }

        await Task { @MainActor in }.value
        waiting.cancel()

        #expect(await waiting.value == false)

        // 취소로 이미 재개된 뒤 남아 있던 관찰 변경 알림이 와도 두 번 재개되면 안 된다(두 번 재개되면 런타임이 중단시킨다).
        counter.value = 1
        await Task { @MainActor in }.value
    }

    @Test("대기를 시작하기 전에 취소돼 있으면 곧바로 false를 반환한다")
    func 시작_전에_취소돼_있으면_false를_반환한다() async {
        let counter = Counter()
        let waiting = Task { await waitUntil { counter.value > 0 } }
        waiting.cancel()

        #expect(await waiting.value == false)
    }
}
