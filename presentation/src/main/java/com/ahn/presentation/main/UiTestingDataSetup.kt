package com.ahn.presentation.main

/** 디버그 UI 테스트가 화면을 만들기 전에 고정 환율 데이터를 준비하는 조립 지점. */
fun interface UiTestingDataSetup {
    suspend fun prepareRates(useFixedRates: Boolean)
}
