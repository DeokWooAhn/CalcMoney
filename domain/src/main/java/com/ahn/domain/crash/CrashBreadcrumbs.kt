package com.ahn.domain.crash

/**
 * 크래시가 났을 때 직전 상황을 알 수 있도록 단서를 남긴다 (iOS `CrashBreadcrumbs` 대응).
 *
 * 스택 트레이스만으로는 어느 화면에서 무엇을 하던 중이었는지 알 수 없다. 여기 남긴 값은 다음 크래시
 * 리포트에 함께 실린다. 계산식·금액처럼 사용자가 입력한 값은 남기지 않는다.
 */
interface CrashBreadcrumbs {
    /** 지금 보고 있는 화면. 크래시 리포트에 키 [SCREEN_KEY]로 실린다. */
    fun setScreen(screen: String)

    /** 크래시 직전 행동을 한 줄 남긴다. */
    fun log(message: String)

    companion object {
        const val SCREEN_KEY = "screen"
    }
}
