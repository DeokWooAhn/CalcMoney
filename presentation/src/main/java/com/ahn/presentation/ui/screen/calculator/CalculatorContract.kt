package com.ahn.presentation.ui.screen.calculator

interface CalculatorContract {
    // 1. 상태: 순수 String과 Int(커서)로 관리
    data class State(
        val expression: String = "",
        val cursorPosition: Int = 0, // 커서 위치를 별도 관리해야 중간 편집 가능
        val previewResult: String = "",
        val isError: Boolean = false,
        val errorMessage: String? = null
    )

    // 2. 의도: 추상화된 입력
    sealed interface Intent {
        data class Input(val token: CalculatorToken) : Intent
        object Delete : Intent
        object Clear : Intent
        object Calculate : Intent
    }

    // 3. 부수 효과: 일회성 이벤트
    sealed interface SideEffect {
        data class ShowSnackBar(val message: String) : SideEffect
    }
}

// 토큰 정의 (입력 타입 구분)
sealed interface CalculatorToken {
    data class Number(val value: String) : CalculatorToken
    data class Operator(val value: String) : CalculatorToken
    object Dot : CalculatorToken
    object Parenthesis : CalculatorToken // 괄호는 로직이 복잡하므로 별도 토큰
}