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

        let received = await effects(from: bus.stream(), count: 2)

        #expect(received == ["첫번째", "두번째"])
    }

    @Test("버퍼는 첫 구독자에게 전달된 뒤 비워진다")
    @MainActor
    func 버퍼는_첫_구독자에게_전달된_뒤_비워진다() async {
        let bus = SideEffectBus<String>()

        bus.send("버퍼된 이벤트")
        let first = await effects(from: bus.stream(), count: 1)
        #expect(first == ["버퍼된 이벤트"])

        let stream = bus.stream()
        bus.send("새 이벤트")

        #expect(await effects(from: stream, count: 1) == ["새 이벤트"])
    }

    @Test("구독자가 있으면 버퍼를 거치지 않고 바로 전달된다")
    @MainActor
    func 구독자가_있으면_버퍼를_거치지_않고_바로_전달된다() async {
        let bus = SideEffectBus<String>()
        let stream = bus.stream()

        bus.send("즉시")

        #expect(await effects(from: stream, count: 1) == ["즉시"])
    }

    @Test("이미 끝난 구독자만 남아 있으면 이벤트를 버려서는 안 된다")
    @MainActor
    func 이미_끝난_구독자만_남아_있으면_이벤트를_버려서는_안_된다() async {
        let bus = SideEffectBus<String>()

        // onTermination 정리는 Task로 미뤄지므로, 아래 send 시점에는 이미 끝난
        // continuation이 아직 딕셔너리에 남아 있다.
        _ = bus.stream()
        bus.send("탭 전환 직후 도착한 이벤트")

        let received = await effects(from: bus.stream(), count: 1)

        #expect(received == ["탭 전환 직후 도착한 이벤트"])
    }

    /// 이벤트가 유실되면 무한 대기 대신 빈 배열로 실패하도록 시간 제한을 둔다.
    private func effects(
        from stream: AsyncStream<String>,
        count: Int,
        timeout: Duration = .seconds(1),
    ) async -> [String] {
        await withTaskGroup(of: [String]?.self) { group in
            group.addTask {
                var collected: [String] = []
                for await effect in stream {
                    collected.append(effect)
                    if collected.count == count { break }
                }
                return collected
            }
            group.addTask {
                try? await Task.sleep(for: timeout)
                return nil
            }

            var result: [String] = []
            for await collected in group {
                if let collected {
                    result = collected
                }
                break
            }
            group.cancelAll()

            return result
        }
    }
}
