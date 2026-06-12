package com.ahn.presentation.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlin.math.roundToInt

@Composable
fun AdMobBanner(
    @StringRes adUnitIdResId: Int,
    canRequestAds: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val adUnitId = stringResource(adUnitIdResId)

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (LocalInspectionMode.current) {
            AdBannerPreviewPlaceholder(modifier = Modifier.fillMaxWidth())
            return@BoxWithConstraints
        }

        if (!canRequestAds) return@BoxWithConstraints

        val adWidth = maxWidth.value.roundToInt()
        if (adWidth <= 0) return@BoxWithConstraints

        val adSize = remember(context, adWidth) {
            AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, adWidth)
        }
        val adView = remember(context, adUnitId, adSize) {
            AdView(context).apply {
                this.adUnitId = adUnitId
                setAdSize(adSize)
                loadAd(AdRequest.Builder().build())
            }
        }

        DisposableEffect(adView) {
            onDispose {
                adView.destroy()
            }
        }

        AndroidView(
            factory = { adView },
            modifier = Modifier
                .fillMaxWidth()
                .height(adSize.height.dp),
        )
    }
}

@Composable
private fun AdBannerPreviewPlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Ad banner",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
