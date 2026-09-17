package com.ahn.calcmoney

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * 디버그 빌드용 App Check provider.
 *
 * 에뮬레이터와 Play 서명이 없는 로컬 빌드에서는 Play Integrity가 동작하지 않으므로
 * debug provider를 쓴다. 이 provider가 만든 토큰은 Firebase 콘솔의 App Check 디버그 토큰
 * 목록에 등록된 것만 유효하다.
 */
internal fun installAppCheck() {
    FirebaseAppCheck.getInstance()
        .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
}
