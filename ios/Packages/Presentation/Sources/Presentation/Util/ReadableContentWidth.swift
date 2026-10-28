import SwiftUI

/// iPad처럼 넓은 화면에서 입력칸·목록이 화면 폭 전체로 늘어나지 않게 하는 최대 폭.
///
/// iPhone은 화면이 이보다 좁아 아무 변화가 없다.
enum ReadableContentWidth {
    static let maxWidth: CGFloat = 640
}

extension View {
    /// 내용 폭을 `ReadableContentWidth.maxWidth` 이하로 줄이고 남는 폭의 가운데에 둔다.
    func readableContentWidth() -> some View {
        frame(maxWidth: ReadableContentWidth.maxWidth)
            .frame(maxWidth: .infinity)
    }
}
