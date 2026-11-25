import Observation
import Testing
@testable import Presentation

@Suite("LatestTaskRunner", .timeLimit(.minutes(1)))
struct LatestTaskRunnerTests {
    @Test("새 일을 시작하면 앞 일은 취소되고 최신이 아니게 된다")
    @MainActor
    func 새_일을_시작하면_앞_일은_취소되고_최신이_아니게_된다() async {
        let runner = LatestTaskRunner()
        let first = Job()
        let second = Job()

        runner.restart { await first.run(isLatest: $0) }
        #expect(await waitUntil { first.didStart })
        runner.restart { await second.run(isLatest: $0) }
        await first.release.open()
        #expect(await waitUntil { first.didFinish })

        #expect(first.sawLatest == false)
        #expect(first.wasCancelled == true)

        // 뒤 일을 멈춘 채 두지 않는다
        await second.release.open()
    }

    @Test("cancel 뒤에는 진행 중이던 일이 취소되고 최신이 아니게 된다")
    @MainActor
    func cancel_뒤에는_진행_중이던_일이_취소되고_최신이_아니게_된다() async {
        let runner = LatestTaskRunner()
        let job = Job()

        runner.restart { await job.run(isLatest: $0) }
        #expect(await waitUntil { job.didStart })
        runner.cancel()
        await job.release.open()
        #expect(await waitUntil { job.didFinish })

        #expect(job.sawLatest == false)
        #expect(job.wasCancelled == true)
    }

    @Test("앞 일이 늦게 끝나도 마지막 일은 최신이다")
    @MainActor
    func 앞_일이_늦게_끝나도_마지막_일은_최신이다() async {
        let runner = LatestTaskRunner()
        let first = Job()
        let second = Job()

        runner.restart { await first.run(isLatest: $0) }
        #expect(await waitUntil { first.didStart })
        runner.restart { await second.run(isLatest: $0) }
        #expect(await waitUntil { second.didStart })
        await first.release.open()
        #expect(await waitUntil { first.didFinish })
        await second.release.open()
        #expect(await waitUntil { second.didFinish })

        #expect(second.sawLatest == true)
        #expect(second.wasCancelled == false)
    }

    @Test("러너가 사라지면 진행 중이던 일이 취소된다")
    @MainActor
    func 러너가_사라지면_진행_중이던_일이_취소된다() async {
        var runner: LatestTaskRunner? = LatestTaskRunner()
        let job = Job()

        runner?.restart { await job.run(isLatest: $0) }
        #expect(await waitUntil { job.didStart })
        runner = nil
        await job.release.open()
        #expect(await waitUntil { job.didFinish })

        #expect(job.sawLatest == false)
        #expect(job.wasCancelled == true)
    }
}

/// 러너에 넘기는 일 하나. 시작을 알리고 `release`가 열릴 때까지 기다린 뒤, 그때 본 값을 남기고 끝났음을 알린다.
///
/// `release`는 취소돼도 풀리지 않는다. 취소돼도 끝까지 응답하는 네트워크 요청을 흉내 낸다.
/// 테스트는 시작·끝을 `waitUntil`로 기다린다. 러너가 일을 실행하지 않는 회귀가 생겨도
/// 스위트의 `.timeLimit`이 대기를 풀어 테스트가 멈춰 있지 않고 실패한다.
@MainActor
@Observable
private final class Job {
    @ObservationIgnored let release = TestGate()
    private(set) var didStart = false
    private(set) var didFinish = false
    private(set) var sawLatest: Bool?
    private(set) var wasCancelled: Bool?

    func run(isLatest: () -> Bool) async {
        didStart = true
        await release.wait()
        sawLatest = isLatest()
        wasCancelled = Task.isCancelled
        didFinish = true
    }
}
