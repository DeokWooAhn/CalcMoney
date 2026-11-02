package com.ahn.data.crash

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify

class FirebaseCrashBreadcrumbsTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerRoot

    val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
    val breadcrumbs = FirebaseCrashBreadcrumbs(crashlytics)

    beforeEach {
        clearMocks(crashlytics)
    }

    Given("Crashlytics로 크래시 단서를 남길 때") {
        When("지금 화면을 남기면") {
            Then("화면 키를 바꾸고 화면 이동 로그도 남겨야 한다") {
                breadcrumbs.setScreen("exchange")

                verify(exactly = 1) { crashlytics.setCustomKey("screen", "exchange") }
                verify(exactly = 1) { crashlytics.log("screen: exchange") }
            }
        }

        When("직전 행동을 남기면") {
            Then("같은 문장을 Crashlytics 로그로 남겨야 한다") {
                breadcrumbs.log("ads: interstitial shown")

                verify(exactly = 1) { crashlytics.log("ads: interstitial shown") }
            }
        }
    }
})
