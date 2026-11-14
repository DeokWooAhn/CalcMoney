# 고정 환율 Maestro 테스트

- 목표: 서버 응답 없이 USD→KRW 환산과 AUD 즐겨찾기의 탭 간 반영을 실제 앱에서 검증한다.
- 범위: 디버그 고정 환율 준비, 두 플랫폼 금액 식별자, 공통 smoke Flow, 실행 문서. 기존 계산기 수정과 실제 환율 release Flow는 유지한다.
- 데이터: USD 1단위=1,000원, AUD 1단위=500원. 계산·즐겨찾기 저장은 실제 구현을 사용한다.
- 검증: Android 빌드·관련 테스트·detekt, iOS 린트·빌드, 양 플랫폼에서 새 Flow를 두 번 연속 실행한다. 기존 smoke도 확인한다.
- 완료: 12 USD=12000.00 KRW, 즐겨찾기 추가 후 AUD 카드에 24.00 AUD와 1 USD=2 AUD가 표시되고 CI smoke에 포함된다.
- 검증 결과: 연결된 실기기가 없어 Android API 34 에뮬레이터와 iPhone 17 Pro(iOS 26.5) 시뮬레이터를 사용했다. 새 Flow는 양 플랫폼에서 두 번 연속 통과했고, 전체 smoke도 각각 4/4 통과했다. 최종 Flow의 AUD 카드 금액 24.00 검증도 양 플랫폼 전체 smoke에서 통과했다.
- 추가 검사: Android debug 빌드·presentation/data 단위 테스트·detekt·Konsist·Kover, iOS 앱 빌드·SwiftLint·SwiftFormat 검사를 통과했다. Android는 고정 모드 다음 일반 실행에서 테스트 표식 제거도 확인했다.
- 남은 확인: GitHub Actions 실행은 푸시 후 확인한다. 기존 Android·iOS CI의 `smoke` 태그 선택에 새 Flow가 포함되며 워크플로 변경은 필요하지 않다.
