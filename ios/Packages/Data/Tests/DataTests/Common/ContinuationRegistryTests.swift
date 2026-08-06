import Foundation
import Testing
@testable import Data

@Suite("ContinuationRegistry")
struct ContinuationRegistryTests {
    @Test("등록 후 해제하면 등록부가 비워진다")
    func 정상_순서로_등록하고_해제한다() {
        var registry = ContinuationRegistry<Int>()
        let id = UUID()
        let (_, continuation) = AsyncStream<Int>.makeStream()

        let didRegister = registry.register(id: id, continuation: continuation)

        #expect(didRegister)
        #expect(registry.count == 1)

        registry.unregister(id: id)

        #expect(registry.isEmpty)
    }

    /// 등록과 onTermination이 각각 별도 Task로 액터에 들어가므로 순서가 뒤집힐 수 있다.
    /// 이 경우를 흡수하지 않으면 뒤늦게 등록된 continuation을 아무도 지우지 않아 영구히 남는다.
    @Test("해제가 등록보다 먼저 도착하면 등록을 거절한다")
    func 해제가_먼저_도착하면_등록되지_않는다() {
        var registry = ContinuationRegistry<Int>()
        let id = UUID()
        let (_, continuation) = AsyncStream<Int>.makeStream()

        registry.unregister(id: id)

        let didRegister = registry.register(id: id, continuation: continuation)

        #expect(didRegister == false)
        #expect(registry.isEmpty)
    }

    @Test("거절된 continuation은 종료된다")
    func 거절된_continuation은_종료된다() {
        var registry = ContinuationRegistry<Int>()
        let id = UUID()
        let (stream, continuation) = AsyncStream<Int>.makeStream()

        registry.unregister(id: id)
        _ = registry.register(id: id, continuation: continuation)

        // 스트림을 소비해서 확인하면 finish()가 빠졌을 때 테스트가 끝나지 않고 멈춘다.
        // yield의 반환값으로 보면 동기적으로 판별할 수 있다.
        //
        // 다만 스트림이 먼저 해제되면 finish() 여부와 무관하게 terminated가 되므로,
        // 확인이 끝날 때까지 스트림을 살려 둬야 검사가 의미를 갖는다.
        withExtendedLifetime(stream) {
            var isTerminated = false
            if case .terminated = continuation.yield(1) {
                isTerminated = true
            }

            #expect(isTerminated)
        }
    }

    @Test("해제 표시는 뒤이은 등록이 소비해 쌓이지 않는다")
    func 해제_표시는_쌓이지_않는다() {
        var registry = ContinuationRegistry<Int>()
        let id = UUID()

        registry.unregister(id: id)
        let (_, first) = AsyncStream<Int>.makeStream()
        let didRegisterFirst = registry.register(id: id, continuation: first)

        #expect(didRegisterFirst == false)

        // 같은 id로 다시 등록하면(표시가 소비됐으므로) 이번엔 정상 등록돼야 한다.
        let (_, second) = AsyncStream<Int>.makeStream()
        let didRegisterSecond = registry.register(id: id, continuation: second)

        #expect(didRegisterSecond)
        #expect(registry.count == 1)
    }

    @Test("등록된 continuation에만 값을 전달한다")
    func 등록된_continuation에만_전달한다() async {
        var registry = ContinuationRegistry<Int>()
        let liveID = UUID()
        let terminatedID = UUID()
        let (liveStream, liveContinuation) = AsyncStream<Int>.makeStream()
        let (_, terminatedContinuation) = AsyncStream<Int>.makeStream()

        _ = registry.register(id: liveID, continuation: liveContinuation)
        registry.unregister(id: terminatedID)
        _ = registry.register(id: terminatedID, continuation: terminatedContinuation)

        registry.yield(42)
        liveContinuation.finish()

        var received: [Int] = []
        for await value in liveStream {
            received.append(value)
        }

        #expect(received == [42])
        #expect(registry.count == 1)
    }
}
