// swift-tools-version: 6.2
import PackageDescription

let package = Package(
    name: "Presentation",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "Presentation", targets: ["Presentation"]),
    ],
    dependencies: [
        .package(path: "../Domain"),
    ],
    targets: [
        .target(
            name: "Presentation",
            dependencies: [
                .product(name: "Domain", package: "Domain"),
            ],
            swiftSettings: [
                .defaultIsolation(MainActor.self),
            ],
        ),
        .testTarget(
            name: "PresentationTests",
            dependencies: ["Presentation"],
            swiftSettings: [
                .defaultIsolation(MainActor.self),
            ],
        ),
    ],
)
