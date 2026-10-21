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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.presentation.R
import com.ahn.presentation.util.localizedName
import com.ahn.presentation.util.platformCurrencyName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/** 스크롤과 인덱스 조작이 멈춘 뒤 인덱스를 숨기기까지 기다리는 시간입니다. */
private const val INDEX_HIDE_DELAY_MS = 1_500L

/** 검색창 바탕에 까는 글자색의 불투명도입니다. iOS `tertiarySystemFill`과 비슷한 짙기입니다. */
private const val SEARCH_FIELD_CONTAINER_ALPHA = 0.07f

/**
 * 통화 선택 시트입니다 (iOS `CurrencyPickerSheet` 대응).
 *
 * 맨 위 검색창이 비어 있으면 즐겨찾기를 맨 위에 둔 전체 목록과 빠른 이동 인덱스를, 검색어가 있으면 맞는 통화만 보여줍니다.
 * 키보드가 목록을 가리지 않도록 화면 가운데 대화상자 대신 거의 전체 높이의 바텀시트를 씁니다.
 *
 * 정렬용 즐겨찾기 스냅샷과 아이콘용 실시간 즐겨찾기를 분리해, 시트가 열려 있는 동안 하트를 눌러도 목록 순서가 튀지 않게 합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerSheet(
    currencies: List<CurrencyInfo>,
    selectedCurrency: CurrencyInfo?,
    favoriteCurrencyCodesForSort: List<String>,
    favoriteCurrencyCodesForIcon: List<String>,
    title: String,
    onDismiss: () -> Unit,
    onCurrencySelected: (CurrencyInfo) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val pickerList = rememberCurrencyPickerList(currencies, favoriteCurrencyCodesForSort, query)
    val favoriteCurrencyCodeSet = remember(favoriteCurrencyCodesForIcon) {
        favoriteCurrencyCodesForIcon.toSet()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // 내용이 전체 높이를 채워도 시트가 상태 표시줄 아래에서 멈추게 해, iOS 시트처럼 위쪽에 뒤 화면이 조금 보이게 한다.
        modifier = Modifier
            .statusBarsPadding()
            .padding(top = 8.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                // 바텀시트는 별도 창이라 MainScreen에서 켠 testTagsAsResourceId가 적용되지 않는다.
                .semantics { testTagsAsResourceId = true },
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            )

            CurrencySearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            HorizontalDivider(
                color = Color.Gray.copy(alpha = 0.2f),
                modifier = Modifier.padding(top = 12.dp),
            )

            if (pickerList.currencies.isEmpty() && query.isNotBlank()) {
                Text(
                    text = stringResource(R.string.currency_search_no_result),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    textAlign = TextAlign.Center,
                )
            } else {
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

/**
 * 검색어가 없으면 전체 목록과 인덱스를, 있으면 검색 결과만 담고 인덱스는 비운 목록을 만듭니다.
 */
@Composable
private fun rememberCurrencyPickerList(
    currencies: List<CurrencyInfo>,
    favoriteCodesForSort: List<String>,
    query: String,
): CurrencyPickerList {
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    // 검색어를 칠 때마다 통화 160여 개의 이름을 다시 만들지 않도록 한 번만 계산한다.
    // 서버 이름은 한국어라, 화면 언어와 관계없이 "dollar"로도 찾을 수 있게 영어 이름을 더한다.
    val searchNames = remember(currencies, resources, locale) {
        currencies.associate { currency ->
            currency.code to listOfNotNull(
                currency.localizedName(resources, locale),
                currency.name,
                platformCurrencyName(currency.code, Locale.ENGLISH),
            )
        }
    }

    return remember(currencies, favoriteCodesForSort, query, searchNames) {
        if (query.isBlank()) {
            buildCurrencyPickerList(currencies, favoriteCodesForSort)
        } else {
            CurrencyPickerList(
                currencies = searchCurrencies(currencies, query, favoriteCodesForSort) {
                    searchNames[it.code].orEmpty()
                },
                indexPositions = emptyMap(),
            )
        }
    }
}

@Composable
private fun CurrencySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    // 시트 바탕과 비슷한 회색은 라이트 모드에서 묻힌다. 글자색을 옅게 깔아 두 모드 모두 바탕보다 한 단계 짙게 보이게 한다.
    val containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = SEARCH_FIELD_CONTAINER_ALPHA)

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag("currency.search"),
        // 좁은 화면이나 큰 글꼴에서 안내 문구가 두 줄이 되면 검색창이 커지므로 한 줄로 자른다.
        placeholder = {
            Text(
                text = stringResource(R.string.currency_search_hint),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.currency_search_clear),
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
    )
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
    val keyboardController = LocalSoftwareKeyboardController.current

    // 검색어가 바뀌어 목록이 달라지면 맨 위 결과부터 보여준다.
    LaunchedEffect(pickerList) { listState.scrollToItem(0) }
    // 목록을 넘겨 보기 시작하면 키보드를 내려 목록을 넓게 보여준다 (iOS scrollDismissesKeyboard 대응).
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) keyboardController?.hide()
    }

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

        // 검색 결과에는 인덱스가 없다.
        AnimatedVisibility(
            visible = isIndexVisible && indexLabels.isNotEmpty(),
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
            .testTag("currency.row.${currency.code}")
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
            modifier = Modifier
                .size(48.dp)
                .testTag("favorite.toggle.${currency.code}"),
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
