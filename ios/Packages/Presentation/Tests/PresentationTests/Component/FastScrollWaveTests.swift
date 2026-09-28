import CoreGraphics
import Testing
@testable import Presentation

@Suite("빠른 이동 인덱스 물결 세기")
struct FastScrollWaveTests {
    private let radius: CGFloat = 60

    @Test("손가락 바로 아래 라벨은 가장 센 1이다")
    func 손가락_바로_아래는_1() {
        #expect(waveInfluence(distance: 0, radius: radius) == 1)
    }

    @Test("물결 반경 끝이나 그 바깥의 라벨은 0이다")
    func 반경_밖은_0() {
        #expect(waveInfluence(distance: radius, radius: radius) == 0)
        #expect(waveInfluence(distance: radius * 2, radius: radius) == 0)
    }

    @Test("손가락에서 멀어질수록 세기가 줄어든다")
    func 멀어질수록_줄어든다() {
        let near = waveInfluence(distance: 10, radius: radius)
        let middle = waveInfluence(distance: 30, radius: radius)
        let far = waveInfluence(distance: 50, radius: radius)

        #expect(near < 1)
        #expect(near > middle)
        #expect(middle > far)
        #expect(far > 0)
    }

    @Test("반경의 절반 거리에서는 세기가 절반이다")
    func 반경_절반은_절반() {
        #expect(abs(waveInfluence(distance: radius / 2, radius: radius) - 0.5) < 0.0001)
    }

    @Test("손가락 위쪽과 아래쪽으로 같은 거리이면 세기가 같다")
    func 위아래_대칭() {
        #expect(waveInfluence(distance: -20, radius: radius) == waveInfluence(distance: 20, radius: radius))
    }

    @Test("반경이 0 이하이면 물결이 없다")
    func 반경이_0_이하면_없다() {
        #expect(waveInfluence(distance: 0, radius: 0) == 0)
        #expect(waveInfluence(distance: 0, radius: -1) == 0)
    }
}

@Suite("빠른 이동 인덱스 위아래 물결")
struct FastScrollRippleTests {
    private let radius: CGFloat = 60

    @Test("물결 중심의 라벨은 밀리거나 흐려지지 않는다")
    func 중심은_0() {
        #expect(waveRipple(distance: 0, radius: radius) == 0)
    }

    @Test("반경의 절반 거리에서 가장 크게 반응한다")
    func 반경_절반에서_최대() {
        #expect(abs(waveRipple(distance: radius / 2, radius: radius) - 1) < 0.0001)
    }

    @Test("반경 끝이나 그 바깥에서는 반응하지 않는다")
    func 반경_밖은_0() {
        #expect(abs(waveRipple(distance: radius, radius: radius)) < 0.0001)
        #expect(waveRipple(distance: radius * 2, radius: radius) == 0)
    }

    @Test("위쪽과 아래쪽으로 같은 거리이면 반응 크기가 같다")
    func 위아래_대칭() {
        #expect(waveRipple(distance: -20, radius: radius) == waveRipple(distance: 20, radius: radius))
    }

    @Test("반경이 0 이하이면 물결이 없다")
    func 반경이_0_이하면_없다() {
        #expect(waveRipple(distance: 0, radius: 0) == 0)
    }
}
