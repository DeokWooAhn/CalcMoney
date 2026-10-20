# review-ktx 2.0.2가 공개 라이브러리에 없는 내부 애너테이션을 참조한다. 어떤 play-services-basement 버전에도
# 이 클래스가 없어 R8이 release 빌드에서 Missing class로 멈춘다. 애너테이션이라 실행에는 필요 없다.
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
