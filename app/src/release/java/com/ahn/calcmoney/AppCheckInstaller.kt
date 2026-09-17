package com.ahn.calcmoney

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * 릴리스 빌드용 App Check provider.
 *
 * Play Integrity는 Play 스토어에서 설치된 앱만 유효한 토큰을 받는다.
 * 사이드로드한 릴리스 APK로는 검증이 실패하므로 로컬 확인은 디버그 빌드로 한다.
 */
internal fun installAppCheck() {
    FirebaseAppCheck.getInstance()
        .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
}
