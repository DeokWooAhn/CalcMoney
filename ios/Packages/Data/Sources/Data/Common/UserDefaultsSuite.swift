import Foundation

extension UserDefaults {
    /// 용도별로 분리된 UserDefaults 스위트를 만든다 (Android의 파일 단위 DataStore 분리 대응).
    public static func suite(named name: String) -> UserDefaults {
        UserDefaults(suiteName: "com.ahn.CalcMoney.\(name)") ?? .standard
    }
}
