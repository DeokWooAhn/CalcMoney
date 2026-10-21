import Domain
import SwiftUI

/// 통화 선택 시트 (Android `CurrencyPickerSheet` 대응)
///
/// 맨 위 검색창이 비어 있으면 즐겨찾기를 맨 위에 둔 전체 목록과 빠른 이동 인덱스를, 검색어가 있으면 맞는 통화만 보여준다.
///
/// 정렬용 즐겨찾기 스냅샷과 아이콘용 실시간 즐겨찾기를 분리해,
/// 시트가 열려 있는 동안 하트를 눌러도 목록 순서가 튀지 않게 한다.
struct CurrencyPickerSheet: View {
    let currencies: [CurrencyInfo]
    let selectedCurrency: CurrencyInfo?
    let favoriteCurrencyCodesForSort: [String]
    let favoriteCurrencyCodesForIcon: [String]
    let title: String
    let onCurrencySelected: (CurrencyInfo) -> Void
    let onToggleFavorite: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var query = ""
    @State private var isScrolling = false
    @State private var isTouchingIndex = false
    /// 미리보기가 사라지는 동안에도 마지막 라벨을 보여주도록 손을 떼도 지우지 않는다.
    @State private var previewLabel: String?
    @State private var isIndexVisible = false

    private static let englishLocale = Locale(identifier: "en")

    /// 스크롤과 인덱스 조작이 멈춘 뒤 인덱스를 숨기기까지 기다리는 시간
    private static let indexHideDelay: Duration = .milliseconds(1500)

    /// iOS 17은 스크롤 상태를 알 수 없어 인덱스를 항상 보여준다.
    private static var detectsScrolling: Bool {
        if #available(iOS 18.0, *) {
            true
        } else {
            false
        }
    }

    private var isSearching: Bool {
        !query.trimmingCharacters(in: .whitespaces).isEmpty
    }

    private var pickerList: CurrencyPickerList {
        guard isSearching else {
            return CurrencyPickerList(currencies: currencies, favoriteCodesForSort: favoriteCurrencyCodesForSort)
        }

        let results = CurrencyPickerList.search(
            currencies,
            query: query,
            favoriteCodesForSort: favoriteCurrencyCodesForSort,
            // 서버 이름은 한국어라, 화면 언어와 관계없이 "dollar"로도 찾을 수 있게 영어 이름을 더한다.
            namesOf: { currency in
                [currency.localizedName, currency.name, platformCurrencyName(currency.code, locale: Self.englishLocale)]
                    .compactMap(\.self)
            },
        )
        return CurrencyPickerList(searchResults: results)
    }

    var body: some View {
        let pickerList = pickerList

        NavigationStack {
            ScrollViewReader { proxy in
                List(pickerList.currencies, id: \.code) { currency in
                    CurrencyPickerRow(
                        currency: currency,
                        isSelected: currency == selectedCurrency,
                        isFavorite: favoriteCurrencyCodesForIcon.contains(currency.code),
                        onSelect: {
                            onCurrencySelected(currency)
                            dismiss()
                        },
                        onToggleFavorite: { onToggleFavorite(currency.code) },
                    )
                }
                .listStyle(.plain)
                .scrollDismissesKeyboard(.immediately)
                .safeAreaInset(edge: .top, spacing: 0) {
                    CurrencySearchField(text: $query)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(.background)
                }
                .overlay {
                    if isSearching, pickerList.currencies.isEmpty {
                        Text(L("검색 결과가 없습니다."))
                            .foregroundStyle(.secondary)
                    }
                }
                // 검색어가 바뀌어 목록이 달라지면 맨 위 결과부터 보여준다.
                .onChange(of: query) {
                    if let first = self.pickerList.currencies.first {
                        proxy.scrollTo(first.code, anchor: .top)
                    }
                }
                .modifier(ScrollActivityModifier(isScrolling: $isScrolling))
                .overlay(alignment: .trailing) {
                    // 검색 결과에는 인덱스가 없다.
                    if !pickerList.indexEntries.isEmpty, isIndexVisible || !Self.detectsScrolling {
                        FastScrollIndex(
                            labels: pickerList.indexEntries.map(\.label),
                            onLabelSelected: { label in
                                previewLabel = label
                                guard let entry = pickerList.indexEntries.first(where: { $0.label == label }) else {
                                    return
                                }
                                proxy.scrollTo(entry.firstCode, anchor: .top)
                            },
                            onTouchingChange: { isTouchingIndex = $0 },
                        )
                        .padding(.vertical, 8)
                        .transition(.opacity)
                    }
                }
                .animation(.easeInOut(duration: 0.2), value: isIndexVisible)
                // 인덱스 라벨은 손가락에 가려지므로, 누르는 동안 현재 라벨을 목록 가운데에 크게 띄운다.
                .overlay {
                    if isTouchingIndex, let previewLabel {
                        FastScrollIndexPreview(label: previewLabel)
                            .transition(.opacity.combined(with: .scale(scale: 0.92)))
                            .allowsHitTesting(false)
                    }
                }
                .animation(.easeOut(duration: 0.15), value: isTouchingIndex)
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 14, weight: .semibold))
                    }
                    .accessibilityLabel(L("닫기"))
                    .accessibilityIdentifier("currency.picker.close")
                }
            }
        }
        // 인덱스를 끄는 동안 시트가 아래로 끌려 닫히지 않게 한다.
        .interactiveDismissDisabled(isTouchingIndex)
        .task(id: isScrolling || isTouchingIndex) {
            await updateIndexVisibility(isActive: isScrolling || isTouchingIndex)
        }
    }

    /// 목록을 스크롤하거나 인덱스를 누르는 동안 인덱스를 보여주고, 둘 다 멈추면 잠시 뒤 숨긴다.
    ///
    /// 다시 스크롤이 시작되면 `task(id:)`가 이 대기를 취소한다.
    private func updateIndexVisibility(isActive: Bool) async {
        if isActive {
            isIndexVisible = true
            return
        }

        do {
            try await Task.sleep(for: Self.indexHideDelay)
        } catch {
            return
        }
        isIndexVisible = false
    }
}

/// 통화 검색창. `.searchable`은 입력란에 테스트 id를 붙일 수 없어 직접 만든다 (Android `CurrencySearchField` 대응).
private struct CurrencySearchField: View {
    @Binding var text: String

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)

            TextField(L("나라, 통화 이름, 코드로 검색"), text: $text)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.search)
                .accessibilityIdentifier("currency.search")

            if !text.isEmpty {
                Button {
                    text = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                        // .plain 스타일은 여백을 더하지 않아 아이콘 크기가 그대로 탭 영역이 된다. 최소 44pt로 넓힌다.
                        .frame(width: 44, height: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(L("검색어 지우기"))
            }
        }
        // 지우기 버튼의 44pt 탭 영역이 오른쪽 여백을 대신한다.
        .padding(.leading, 10)
        .padding(.trailing, text.isEmpty ? 10 : 0)
        // 지우기 버튼이 나타나도 높이가 바뀌지 않게 버튼 탭 영역과 같은 높이로 고정한다.
        .frame(height: 44)
        .background(Color(.tertiarySystemFill), in: RoundedRectangle(cornerRadius: 10))
    }
}

/// iOS 18부터 제공되는 스크롤 단계로 스크롤 중인지 알린다. iOS 17에서는 아무것도 하지 않는다.
private struct ScrollActivityModifier: ViewModifier {
    @Binding var isScrolling: Bool

    func body(content: Content) -> some View {
        if #available(iOS 18.0, *) {
            content.onScrollPhaseChange { _, newPhase in
                isScrolling = newPhase.isScrolling
            }
        } else {
            content
        }
    }
}

private struct CurrencyPickerRow: View {
    let currency: CurrencyInfo
    let isSelected: Bool
    let isFavorite: Bool
    let onSelect: () -> Void
    let onToggleFavorite: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Text(currency.flagEmoji)
                .font(.system(size: 24))

            VStack(alignment: .leading, spacing: 2) {
                Text(currency.displayCode)
                    .font(.system(size: 16, weight: .semibold))

                Text(currency.localizedName)
                    .font(.system(size: 14))
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Button(action: onToggleFavorite) {
                Image(systemName: isFavorite ? "heart.fill" : "heart")
                    .font(.system(size: 18))
                    .foregroundStyle(isFavorite ? .red : .secondary)
                    // .plain 스타일은 여백을 더하지 않아 아이콘 크기가 그대로 탭 영역이 된다.
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(isFavorite ? L("즐겨찾기 해제") : L("즐겨찾기 추가"))
            .accessibilityIdentifier("favorite.toggle.\(currency.code)")
        }
        // 행 기본 여백에 더해 빠른 이동 인덱스 폭만큼 비워, 스크롤 직후에도 하트 버튼이 가려지지 않게 한다.
        .padding(.trailing, fastScrollIndexWidth - 16)
        .contentShape(Rectangle())
        .onTapGesture(perform: onSelect)
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("currency.row.\(currency.code)")
        .listRowBackground(isSelected ? AppColors.surface : Color.clear)
    }
}
