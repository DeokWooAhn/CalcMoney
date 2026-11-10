// swift-tools-version: 6.2
import PackageDescription

let package = Package(
    name: "Data",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "Data", targets: ["Data"]),
    ],
    dependencies: [
        .package(path: "../Domain"),
        // 12.19.3 미만은 Firestore가 내장한 신뢰 목록에 유출된 eMudhra 중간 인증 기관의 루트가 남아 있다
        // (Firebase 보안 공지, 2026-10). 이보다 낮은 버전으로 내려가지 않게 최소 버전을 둔다.
        .package(url: "https://github.com/firebase/firebase-ios-sdk.git", from: "12.19.3"),
    ],
    targets: [
        .target(
            name: "Data",
            dependencies: [
                .product(name: "Domain", package: "Domain"),
                .product(name: "FirebaseAppCheck", package: "firebase-ios-sdk"),
                .product(name: "FirebaseFirestore", package: "firebase-ios-sdk"),
                .product(name: "FirebaseAnalytics", package: "firebase-ios-sdk"),
                .product(name: "FirebaseCrashlytics", package: "firebase-ios-sdk"),
            ],
        ),
        .testTarget(name: "DataTests", dependencies: ["Data"]),
    ],
)
