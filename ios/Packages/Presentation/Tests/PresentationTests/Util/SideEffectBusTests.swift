import Testing
@testable import Presentation

@Suite("SideEffectBus")
struct SideEffectBusTests {
    @Test("구독자가 없는 동안 보낸 이벤트는 다음 구독자에게 전달된다")
    @MainActor
    func 구독자가_없는_동안_보낸_이벤트는_다음_구독자에게_전달된다() async {
        let bus = SideEffectBus<String>()

        bus.send("첫번째")
        bus.send("두번째")

        var received: [String] = []
        for await effect in bus.stream() {
            received.append(effect)
            if received.count == 2 { break }
        }

        #expect(received == ["첫번째", "두번째"])
    }

    @Test("버퍼는 첫 구독자에게 전달된 뒤 비워진다")
    @MainActor
    func 버퍼는_첫_구독자에게_전달된_뒤_비워진다() async {
        let bus = SideEffectBus<String>()

        bus.send("버퍼된 이벤트")

        var first: [String] = []
        for await effect in bus.stream() {
            first.append(effect)
            break
        }
        #expect(first == ["버퍼된 이벤트"])

        let stream = bus.stream()
        bus.send("새 이벤트")

        var second: [String] = []
        for await effect in stream {
            second.append(effect)
            break
        }
        #expect(second == ["새 이벤트"])
    }

    @Test("구독자가 있으면 버퍼를 거치지 않고 바로 전달된다")
    @MainActor
    func 구독자가_있으면_버퍼를_거치지_않고_바로_전달된다() async {
        let bus = SideEffectBus<String>()
        let stream = bus.stream()

        bus.send("즉시")

        var received: [String] = []
        for await effect in stream {
            received.append(effect)
            break
        }

        #expect(received == ["즉시"])
    }
}
