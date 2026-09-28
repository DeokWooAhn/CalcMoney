package com.ahn.presentation.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.presentation.R
import com.ahn.presentation.util.localizedName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 스크롤과 인덱스 조작이 멈춘 뒤 인덱스를 숨기기까지 기다리는 시간입니다. */
private const val INDEX_HIDE_DELAY_MS = 1_500L

@Composable
fun CurrencyPickerDialog(
    currencies: List<CurrencyInfo>,
    selectedCurrency: CurrencyInfo?,
    favoriteCurrencyCodesForSort: List<String>,
    favoriteCurrencyCodesForIcon: List<String>,
    title: String,
    onDismiss: () -> Unit,
    onCurrencySelected: (CurrencyInfo) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val pickerList = remember(currencies, favoriteCurrencyCodesForSort) {
        buildCurrencyPickerList(currencies, favoriteCurrencyCodesForSort)
    }
    val favoriteCurrencyCodeSet = remember(favoriteCurrencyCodesForIcon) {
        favoriteCurrencyCodesForIcon.toSet()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(20.dp),
                )

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                CurrencyPickerListWithIndex(
                    pickerList = pickerList,
                    selectedCurrency = selectedCurrency,
                    favoriteCurrencyCodeSet = favoriteCurrencyCodeSet,
                    onCurrencySelected = onCurrencySelected,
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }
    }
}

@Composable
private fun CurrencyPickerListWithIndex(
    pickerList: CurrencyPickerList,
    selectedCurrency: CurrencyInfo?,
    favoriteCurrencyCodeSet: Set<String>,
    onCurrencySelected: (CurrencyInfo) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isTouchingIndex by remember { mutableStateOf(false) }
    // 미리보기가 사라지는 동안에도 마지막 라벨을 보여주도록 손을 떼도 지우지 않는다.
    var previewLabel by remember { mutableStateOf<String?>(null) }
    val isIndexVisible = rememberIndexVisibility(listState = listState, isTouchingIndex = isTouchingIndex)
    val indexLabels = remember(pickerList) { pickerList.indexPositions.keys.toList() }

    Box {
        LazyColumn(state = listState) {
            items(pickerList.currencies, key = { it.code }) { currency ->
                CurrencyPickerItem(
                    currency = currency,
                    isSelected = currency == selectedCurrency,
                    isFavorite = currency.code in favoriteCurrencyCodeSet,
                    onClick = { onCurrencySelected(currency) },
                    onToggleFavorite = { onToggleFavorite(currency.code) },
                )
            }
        }

        AnimatedVisibility(
            visible = isIndexVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .matchParentSize()
                .padding(vertical = 8.dp),
        ) {
            FastScrollIndex(
                labels = indexLabels,
                onLabelSelected = { label ->
                    previewLabel = label
                    val position = pickerList.indexPositions[label] ?: return@FastScrollIndex
                    coroutineScope.launch { listState.scrollToItem(position) }
                },
                onTouchingChange = { isTouchingIndex = it },
                modifier = Modifier.fillMaxHeight(),
            )
        }

        // 인덱스 라벨은 손가락에 가려지므로, 누르는 동안 현재 라벨을 목록 가운데에 크게 띄운다.
        AnimatedVisibility(
            visible = isTouchingIndex && previewLabel != null,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            previewLabel?.let { FastScrollIndexPreview(label = it) }
        }
    }
}

/**
 * 목록을 스크롤하거나 인덱스를 누르는 동안 인덱스를 보여주고, 둘 다 멈추면 잠시 뒤 숨깁니다.
 */
@Composable
private fun rememberIndexVisibility(listState: LazyListState, isTouchingIndex: Boolean): Boolean {
    var isVisible by remember { mutableStateOf(false) }
    val isActive = listState.isScrollInProgress || isTouchingIndex

    LaunchedEffect(isActive) {
        if (isActive) {
            isVisible = true
        } else {
            // 다시 스크롤이 시작되면 isActive가 바뀌며 이 대기는 취소된다.
            delay(INDEX_HIDE_DELAY_MS)
            isVisible = false
        }
    }

    return isVisible
}

@Composable
private fun CurrencyPickerItem(
    currency: CurrencyInfo,
    isSelected: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val currencyName = currency.localizedName()
    val selectCurrencyLabel = stringResource(
        R.string.select_currency_accessibility,
        currency.displayCode,
        currencyName,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = selectCurrencyLabel,
                onClick = onClick,
            )
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    Color.Transparent
                },
            )
            // 오른쪽은 빠른 이동 인덱스 폭만큼 비워 두어 스크롤 직후에도 하트 버튼이 가려지지 않게 한다.
            .padding(start = 20.dp, end = FastScrollIndexWidth, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = currency.flagEmoji,
            fontSize = 24.sp,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currency.displayCode,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = currencyName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = if (isFavorite) {
                    Icons.Default.Favorite
                } else {
                    Icons.Default.FavoriteBorder
                },
                contentDescription = if (isFavorite) {
                    stringResource(R.string.remove_favorite)
                } else {
                    stringResource(R.string.add_favorite)
                },
                tint = if (isFavorite) {
                    Color.Red
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
