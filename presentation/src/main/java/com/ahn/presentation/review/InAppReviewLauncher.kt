package com.ahn.presentation.review

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Google Play 인앱 리뷰 카드를 띄웁니다.
 *
 * 카드를 실제로 보여 줄지는 Play가 정합니다. 할당량을 넘었거나 Play 스토어에서 설치하지 않은 빌드(사이드로드한 debug 등)면
 * 호출이 성공해도 아무것도 뜨지 않고, 앱은 그 결과를 알 수 없습니다. 그래서 실패해도 로그만 남깁니다.
 */
class InAppReviewLauncher(private val activity: ComponentActivity) {
    private val reviewManager = ReviewManagerFactory.create(activity)

    fun launch() {
        activity.lifecycleScope.launch {
            try {
                val reviewInfo = reviewManager.requestReview()
                reviewManager.launchReview(activity, reviewInfo)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch in-app review.", e)
            }
        }
    }

    private companion object {
        const val TAG = "InAppReviewLauncher"
    }
}
