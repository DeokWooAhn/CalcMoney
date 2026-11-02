package com.ahn.data.crash

import com.ahn.domain.crash.CrashBreadcrumbs
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject

/** Firebase Crashlytics의 커스텀 키와 로그로 크래시 단서를 남긴다. */
class FirebaseCrashBreadcrumbs @Inject constructor(
    private val crashlytics: FirebaseCrashlytics,
) : CrashBreadcrumbs {
    override fun setScreen(screen: String) {
        crashlytics.setCustomKey(CrashBreadcrumbs.SCREEN_KEY, screen)
        crashlytics.log("screen: $screen")
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }
}
