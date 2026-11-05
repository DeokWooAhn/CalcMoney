package com.ahn.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty

/**
 * `.claude/skills/mvi-screen/SKILL.md`에 글로 적힌 구조 규칙을 테스트로 고정한다.
 *
 * 레이어 의존 방향(presentation은 data를 모른다)은 Gradle 모듈 경계가 컴파일 단계에서 막으므로 여기서 보지 않는다.
 * 예외는 아래 목록에 이유와 함께 둔다. 예외가 규칙을 지키게 되거나 이름이 바뀌면 목록을 고치라고 테스트가 실패한다.
 *
 * Konsist는 빈 목록에 대한 단언을 통과시킨다. 그래서 검사 대상이 비지 않았는지와 예외 이름이 실제 클래스인지를 따로 본다.
 */
class ArchitectureTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerRoot

    val scope = Konsist.scopeFromProduction()
    val viewModels = scope.classes().withNameEndingWith("ViewModel")
    val useCases = scope.classes().withNameEndingWith("UseCase")
    val repositoryImpls = scope.classes().withNameEndingWith("RepositoryImpl")

    Given("검사 대상") {
        Then("ViewModel·UseCase·Repository 구현을 찾는다") {
            viewModels.shouldNotBeEmpty()
            useCases.shouldNotBeEmpty()
            repositoryImpls.shouldNotBeEmpty()
        }

        Then("예외 목록의 이름은 모두 실제 클래스다") {
            (NON_ORBIT_VIEW_MODELS.keys - viewModels.map { it.name }.toSet()).shouldBeEmpty()
            (NON_INVOKE_USE_CASES.keys - useCases.map { it.name }.toSet()).shouldBeEmpty()
        }
    }

    Given("ViewModel") {
        Then("모두 @HiltViewModel이다") {
            viewModels.assertTrue { it.hasAnnotationWithName("dagger.hilt.android.lifecycle.HiltViewModel") }
        }

        Then("예외 목록 밖의 ViewModel은 Orbit ContainerHost를 구현한다") {
            viewModels
                .filterNot { it.name in NON_ORBIT_VIEW_MODELS }
                .assertTrue(additionalMessage = "새 화면은 Orbit MVI로 만든다. 정석은 ExchangeViewModel") {
                    it.implementsContainerHost()
                }
        }

        Then("예외 목록의 ViewModel이 Orbit으로 바뀌었으면 목록에서 뺀다") {
            viewModels
                .filter { it.name in NON_ORBIT_VIEW_MODELS }
                .assertFalse(additionalMessage = "ArchitectureTest의 NON_ORBIT_VIEW_MODELS에서 뺀다") {
                    it.implementsContainerHost()
                }
        }

        Then("Orbit ViewModel은 같은 패키지의 Contract에 State·Intent·SideEffect를 둔다") {
            viewModels
                .filter { it.implementsContainerHost() }
                .assertTrue(
                    additionalMessage = "<Feature>Contract에 data class State, sealed interface Intent·SideEffect",
                ) { vm ->
                    val contractName = vm.name.removeSuffix("ViewModel") + "Contract"
                    val contract = scope.interfaces().firstOrNull {
                        it.name == contractName && it.packagee?.name == vm.packagee?.name
                    }
                    contract != null &&
                        contract.classes().any { it.name == "State" && it.hasDataModifier } &&
                        contract.interfaces().any { it.name == "Intent" && it.hasSealedModifier } &&
                        contract.interfaces().any { it.name == "SideEffect" && it.hasSealedModifier }
                }
        }
    }

    Given("UseCase") {
        Then("UseCase와 UseCase 홀더는 domain의 usecase 패키지에 있다") {
            (useCases + scope.classes().withNameEndingWith("UseCases"))
                .assertTrue { it.resideInPackage("com.ahn.domain..usecase..") }
        }

        Then("예외 목록 밖의 UseCase는 operator fun invoke를 가진다") {
            useCases
                .filterNot { it.name in NON_INVOKE_USE_CASES }
                .assertTrue(additionalMessage = "UseCase는 단일 책임이고 operator fun invoke로 부른다") {
                    it.hasOperatorInvoke()
                }
        }

        Then("예외 목록의 UseCase가 invoke를 갖게 됐으면 목록에서 뺀다") {
            useCases
                .filter { it.name in NON_INVOKE_USE_CASES }
                .assertFalse(additionalMessage = "ArchitectureTest의 NON_INVOKE_USE_CASES에서 뺀다") {
                    it.hasOperatorInvoke()
                }
        }
    }

    Given("Repository 구현") {
        Then("data의 repository 패키지에 있고 domain의 같은 이름 인터페이스를 구현한다") {
            repositoryImpls
                .assertTrue { impl ->
                    impl.resideInPackage("com.ahn.data..repository..") &&
                        impl.parents().any { it.name == impl.name.removeSuffix("Impl") }
                }
        }
    }
})

/** Orbit ContainerHost를 쓰지 않는 ViewModel과 그 이유. 새 화면은 여기에 더하지 않는다. */
private val NON_ORBIT_VIEW_MODELS = mapOf(
    "MainViewModel" to "Activity 범위에서 테마·인앱 리뷰·전면 광고·크래시 단서를 다룬다. 화면의 Intent·SideEffect가 없다",
    "FavoriteViewModel" to "공유 ExchangeViewModel의 상태를 받아 즐겨찾기 환율 목록만 계산하는 보조 ViewModel이다. " +
        "자체 Intent·SideEffect가 없어 FavoriteContract도 State만 둔다",
)

/** operator fun invoke 대신 이름 있는 함수를 쓰는 UseCase와 그 이유. */
private val NON_INVOKE_USE_CASES = mapOf(
    "ConvertExchangeAmountUseCase" to "계산식 환산(convertExpression)과 단일 금액 환산(convertSingleAmount) 두 공개 함수를 " +
        "가진다. 나눌지는 따로 정한다",
)

private fun com.lemonappdev.konsist.api.declaration.KoClassDeclaration.implementsContainerHost() =
    parents().any { it.name.startsWith("ContainerHost") }

private fun com.lemonappdev.konsist.api.declaration.KoClassDeclaration.hasOperatorInvoke() =
    functions().any { it.name == "invoke" && it.hasOperatorModifier }
