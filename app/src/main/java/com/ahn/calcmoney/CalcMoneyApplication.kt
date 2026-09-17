package com.ahn.calcmoney

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CalcMoneyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // FirebaseApp은 google-services 플러그인이 넣는 FirebaseInitProvider가
        // onCreate 이전에 초기화하므로, 여기서 바로 App Check provider를 설치할 수 있다.
        installAppCheck()
    }
}
