import Foundation
import Testing

/// 새 화면이 시스템 글자 크기를 따르지 않는 고정 크기 글꼴을 다시 들이지 않게 막는다.
@Suite("고정 글자 크기 금지")
struct FixedFontSizeGuardTests {
    /// 고정 크기를 써도 되는 파일. 수정자 자신과, 라벨 27개가 세로로 들어가야 하는 빠른 이동 인덱스다.
    private static let allowedFileNames: Set = ["ScaledFont.swift", "FastScrollIndex.swift"]

    /// 이 파일(`Tests/PresentationTests/Util/`)에서 `Sources/Presentation/`을 찾는다.
    private static var sourcesDirectory: URL {
        URL(filePath: #filePath)
            .deletingLastPathComponent() // Util
            .deletingLastPathComponent() // PresentationTests
            .deletingLastPathComponent() // Tests
            .deletingLastPathComponent() // Presentation 패키지
            .appending(path: "Sources/Presentation")
    }

    @Test("고정 크기 글꼴은 수정자 파일과 빠른 이동 인덱스에만 있다")
    func 고정_크기_글꼴은_허용된_파일에만_있다() throws {
        let swiftFiles = Self.swiftFiles(in: Self.sourcesDirectory)
        // 소스를 못 읽었는데 위반이 없다고 통과하지 않게, 읽은 파일이 있는지부터 본다.
        try #require(swiftFiles.count > 10)

        let violations = try swiftFiles
            .filter { !Self.allowedFileNames.contains($0.lastPathComponent) }
            .filter { try String(contentsOf: $0, encoding: .utf8).contains(".system(size:") }
            .map(\.lastPathComponent)

        #expect(violations.isEmpty, "고정 크기 대신 scaledFont(size:)를 쓴다: \(violations)")
    }

    @Test("검사가 실제로 고정 크기를 찾는다")
    func 검사가_허용된_파일의_고정_크기를_찾는다() throws {
        let fastScrollIndex = Self.sourcesDirectory.appending(path: "Component/FastScrollIndex.swift")

        #expect(try String(contentsOf: fastScrollIndex, encoding: .utf8).contains(".system(size:"))
    }

    private static func swiftFiles(in directory: URL) -> [URL] {
        guard let enumerator = FileManager.default.enumerator(at: directory, includingPropertiesForKeys: nil) else {
            return []
        }
        return enumerator.compactMap { $0 as? URL }.filter { $0.pathExtension == "swift" }
    }
}
