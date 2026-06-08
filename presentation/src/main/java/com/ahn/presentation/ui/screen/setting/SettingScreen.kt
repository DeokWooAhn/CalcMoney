package com.ahn.presentation.ui.screen.setting

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahn.domain.setting.model.ThemeMode
import com.ahn.presentation.R
import com.ahn.presentation.main.MainViewModel
import com.ahn.presentation.ui.component.AdMobBanner
import com.ahn.presentation.ui.component.CustomSnackbarHost
import com.ahn.presentation.ui.screen.exchange.ExchangeContract
import com.ahn.presentation.ui.screen.exchange.ExchangeViewModel
import com.ahn.presentation.ui.theme.CalcMoneyTheme
import com.ahn.presentation.util.formatExchangeRateDate
import com.ahn.presentation.util.formatExchangeRateFetchedAt
import com.ahn.presentation.util.showSnackbarImmediately
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun SettingRoute(
    exchangeViewModel: ExchangeViewModel,
    mainViewModel: MainViewModel,
) {
    val exchangeState by exchangeViewModel.collectAsState()
    val themeMode by mainViewModel.themeMode.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val appInfo = remember(context) { context.getAppInfo() }
    val exchangeRateDateText = remember(exchangeState.exchangeRateDate) {
        exchangeState.exchangeRateDate
            .takeIf { it.isNotBlank() }
            ?.let(::formatExchangeRateDate)
    }
    val exchangeRateFetchedAtText = remember(exchangeState.exchangeRateFetchedAt) {
        formatExchangeRateFetchedAt(exchangeState.exchangeRateFetchedAt)
    }

    exchangeViewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is ExchangeContract.SideEffect.ShowSnackBar -> {
                scope.launch {
                    snackbarHostState.showSnackbarImmediately(sideEffect.message.asString(context))
                }
            }
        }
    }

    SettingScreen(
        appInfo = appInfo,
        themeMode = themeMode,
        exchangeRateDateText = exchangeRateDateText,
        exchangeRateFetchedAtText = exchangeRateFetchedAtText,
        isExchangeRateLoading = exchangeState.isLoading,
        snackbarHostState = snackbarHostState,
        onThemeModeSelected = mainViewModel::saveThemeMode,
        onRefreshExchangeRate = {
            exchangeViewModel.processIntent(ExchangeContract.Intent.RefreshExchangeRates)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    modifier: Modifier = Modifier,
    appInfo: AppInfo = AppInfo(),
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    exchangeRateDateText: String? = null,
    exchangeRateFetchedAtText: String? = null,
    isExchangeRateLoading: Boolean = false,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onThemeModeSelected: (ThemeMode) -> Unit = {},
    onRefreshExchangeRate: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = {
            CustomSnackbarHost(snackbarHostState = snackbarHostState)
        },
        bottomBar = {
            AdMobBanner(adUnitIdResId = R.string.admob_settings_banner_id)
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.bottom_nav_settings),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        SettingContent(
            appInfo = appInfo,
            themeMode = themeMode,
            paddingValues = paddingValues,
            exchangeRateDateText = exchangeRateDateText,
            exchangeRateFetchedAtText = exchangeRateFetchedAtText,
            isExchangeRateLoading = isExchangeRateLoading,
            onThemeModeSelected = onThemeModeSelected,
            onRefreshExchangeRate = onRefreshExchangeRate,
        )
    }
}

@Composable
private fun SettingContent(
    appInfo: AppInfo,
    themeMode: ThemeMode,
    paddingValues: PaddingValues,
    exchangeRateDateText: String?,
    exchangeRateFetchedAtText: String?,
    isExchangeRateLoading: Boolean,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onRefreshExchangeRate: () -> Unit,
) {
    val emptyValue = stringResource(R.string.setting_exchange_rate_date_empty)
    val loadingValue = stringResource(R.string.setting_loading)
    val exchangeRateDateValue = exchangeRateDateText ?: emptyValue
    val lastUpdatedValue =
        exchangeRateFetchedAtText ?: if (isExchangeRateLoading) loadingValue else emptyValue
    var isThemeExpanded by remember { mutableStateOf(false) }
    var isRateInfoExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        SettingSection(title = stringResource(R.string.setting_section_display)) {
            ThemeSettingCard(
                title = stringResource(R.string.setting_theme),
                summary = themeMode.label(),
                selectedThemeMode = themeMode,
                expanded = isThemeExpanded,
                onExpandedChange = { isThemeExpanded = !isThemeExpanded },
                onThemeModeSelected = onThemeModeSelected,
            )
        }

        SettingSection(title = stringResource(R.string.setting_section_currency)) {
            RefreshSettingCard(
                title = stringResource(R.string.setting_exchange_rate_refresh),
                isLoading = isExchangeRateLoading,
                onClick = onRefreshExchangeRate,
            )
            ExchangeRateInfoCard(
                title = stringResource(R.string.setting_exchange_rate_detail),
                summary = exchangeRateDateValue,
                rateDate = exchangeRateDateValue,
                dataSource = stringResource(R.string.setting_exchange_rate_source_value),
                lastUpdated = lastUpdatedValue,
                notice = stringResource(R.string.setting_exchange_rate_notice_value),
                expanded = isRateInfoExpanded,
                onExpandedChange = { isRateInfoExpanded = !isRateInfoExpanded },
            )
        }

        SettingSection(title = stringResource(R.string.setting_section_app_info)) {
            AppInfoCard(
                title = stringResource(R.string.setting_app_version),
                versionName = appInfo.versionName,
            )
        }
    }
}

@Composable
private fun ThemeMode.label(): String {
    return when (this) {
        ThemeMode.SYSTEM -> stringResource(R.string.setting_theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.setting_theme_light)
        ThemeMode.DARK -> stringResource(R.string.setting_theme_dark)
    }
}

@Composable
private fun SettingSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Column {
        content()
    }
}

@Composable
private fun SettingCard(
    title: String,
    summary: String? = null,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
    expandedContent: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            SettingCardHeader(
                title = title,
                summary = summary,
                expanded = expanded,
                onClick = onExpandedChange,
                trailingContent = trailingContent,
            )

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                ) {
                    expandedContent()
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SettingCardHeader(
    title: String,
    summary: String?,
    expanded: Boolean,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)?,
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "settingCardArrowRotation",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        summary?.let {
            Text(
                text = it,
                modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (trailingContent != null) {
            trailingContent()
        } else {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer(rotationZ = arrowRotation),
            )
        }
    }
}

@Composable
private fun ThemeSettingCard(
    title: String,
    summary: String,
    selectedThemeMode: ThemeMode,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
) {
    SettingCard(
        title = title,
        summary = summary,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeModeOption(
                text = stringResource(R.string.setting_theme_system),
                selected = selectedThemeMode == ThemeMode.SYSTEM,
                onClick = { onThemeModeSelected(ThemeMode.SYSTEM) },
                modifier = Modifier.weight(1f),
            )
            ThemeModeOption(
                text = stringResource(R.string.setting_theme_light),
                selected = selectedThemeMode == ThemeMode.LIGHT,
                onClick = { onThemeModeSelected(ThemeMode.LIGHT) },
                modifier = Modifier.weight(1f),
            )
            ThemeModeOption(
                text = stringResource(R.string.setting_theme_dark),
                selected = selectedThemeMode == ThemeMode.DARK,
                onClick = { onThemeModeSelected(ThemeMode.DARK) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ThemeModeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .wrapContentHeight(Alignment.CenterVertically),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RefreshSettingCard(
    title: String,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            RefreshIconButton(
                isLoading = isLoading,
                onClick = onClick,
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun RefreshIconButton(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refreshIconTransition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "refreshIconRotation",
    )

    IconButton(
        enabled = !isLoading,
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = stringResource(R.string.setting_exchange_rate_refresh),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer(rotationZ = if (isLoading) rotation else 0f),
        )
    }
}

@Composable
private fun ExchangeRateInfoCard(
    title: String,
    summary: String,
    rateDate: String,
    dataSource: String,
    lastUpdated: String,
    notice: String,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
) {
    SettingCard(
        title = title,
        summary = summary,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        SettingInfoRow(
            label = stringResource(R.string.setting_exchange_rate_date_label),
            value = rateDate,
        )
        SettingInfoRow(
            label = stringResource(R.string.setting_exchange_rate_source_label),
            value = dataSource,
        )
        SettingInfoRow(
            label = stringResource(R.string.setting_exchange_rate_last_updated_label),
            value = lastUpdated,
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = notice,
            fontSize = 10.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppInfoCard(
    title: String,
    versionName: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = versionName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SettingInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingScreenPreview() {
    CalcMoneyTheme {
        SettingScreen(
            appInfo = AppInfo(
                versionName = "1.0",
            ),
            themeMode = ThemeMode.SYSTEM,
            exchangeRateDateText = "2026.05.29",
            exchangeRateFetchedAtText = "2026.06.02 14:32",
        )
    }
}

data class AppInfo(
    val versionName: String = "",
)

private fun Context.getAppInfo(): AppInfo {
    val packageInfo = getPackageInfoCompat()
    val versionName = packageInfo.versionName.orEmpty()

    return AppInfo(
        versionName = versionName,
    )
}

private fun Context.getPackageInfoCompat(): PackageInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
}
