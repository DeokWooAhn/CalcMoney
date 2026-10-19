package com.ahn.presentation.ui.screen.setting

import android.content.Context
import android.content.Intent
import com.ahn.presentation.R

/**
 * 설정의 "친구에게 추천하기"가 공유하는 Play 스토어 링크입니다.
 *
 * 공유로 들어온 설치를 Play Console 획득 보고서와 Firebase 첫 실행 캠페인에서 구분하려고 referrer에 UTM을 붙입니다.
 */
internal fun playStoreRecommendUrl(packageName: String): String {
    return "https://play.google.com/store/apps/details?id=$packageName&referrer=$RECOMMEND_REFERRER"
}

/** 시스템 공유 시트로 앱 추천 메시지를 보냅니다. */
internal fun Context.shareAppRecommendation() {
    val sheetTitle = getString(R.string.share_app_sheet_title)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_message, playStoreRecommendUrl(packageName)))
        // 공유 시트 상단 미리보기 제목. 받는 앱으로 가는 메시지(EXTRA_TEXT)에는 들어가지 않는다.
        putExtra(Intent.EXTRA_TITLE, sheetTitle)
    }

    startActivity(Intent.createChooser(sendIntent, sheetTitle))
}

/**
 * URL 인코딩된 `utm_source=app_share`.
 *
 * 링크가 받는 사람의 메시지에 그대로 보이므로 공유 유입을 구분할 최소한만 붙인다.
 */
private const val RECOMMEND_REFERRER = "utm_source%3Dapp_share"
