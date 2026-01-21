package com.ahn.presentation.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.ahn.domain.usecase.CalculatorEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val calculatorEngine: CalculatorEngine
) : ViewModel() {

    private fun insert(textToInsert: String) {
        val currentText = inputState.text
        val selection = inputState.selection
        val operators = listOf("+", "-", "×", "÷")

        if (textToInsert.all { it.isDigit() || it == '.' }) {
            if (!canInsertNumber(currentText, selection.min)) {
                showToast("숫자는 최대 15자리까지 입력 가능합니다.")
                return
            }
        }

        if (currentText.isEmpty()) {
            if (textToInsert in operators) return
            if (textToInsert == ".") {
                inputState = TextFieldValue(text = "0.", selection = TextRange(2))
                return
            }

            inputState = TextFieldValue(
                text = textToInsert,
                selection = TextRange(textToInsert.length)
            )

            return
        }

        if (currentText == "0" && textToInsert !in operators && textToInsert != ".") {
            inputState = TextFieldValue(
                text = textToInsert,
                selection = TextRange(textToInsert.length)
            )
            return
        }

        if (textToInsert in operators) {
            val cursor = selection.min

            // 맨 앞에 연산자가 오려고 할 때 (예: 커서가 0에 있음) 입력 막기 (선택 사항)
            if (cursor == 0) return

            // 커서 바로 앞 글자가 연산자인지 확인
            if (cursor > 0) {
                val charBefore = currentText[cursor - 1]
                if (charBefore.toString() in operators) {
                    // 이전 연산자를 지우고 새로운 연산자로 교체 (예: 1+ 상태에서 - 입력 시 1-로 변경)
                    val newText = StringBuilder(currentText)
                        .replace(cursor - 1, cursor, textToInsert)
                        .toString()

                    inputState = TextFieldValue(
                        text = newText,
                        selection = TextRange(cursor) // 커서 위치 유지
                    )
                    return
                }
            }
        }

        val newText = StringBuilder(currentText)
            .replace(selection.min, selection.max, textToInsert)
            .toString()

        val newCursorPosition = selection.min + textToInsert.length
        inputState = TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPosition)
        )
    }

    private fun delete() {
        val currentText = inputState.text
        val selection = inputState.selection

        if (currentText.isEmpty()) {
            return
        }

        val cursor = selection.min
        // 커서가 맨 앞에 있으면 지울게 없음 (단, 드래그 선택 상태면 지움)
        if (cursor == 0 && selection.collapsed) return

        val newText: String
        val newCursorPos: Int

        if (!selection.collapsed) {
            // 1. 드래그 선택된 영역 삭제
            newText = currentText.removeRange(selection.min, selection.max)
            newCursorPos = selection.min
        } else {
            // 2. 커서 앞 한 글자 삭제
            newText = currentText.removeRange(cursor - 1, cursor)
            newCursorPos = cursor - 1
        }

        // 다 지웠으면 "0"으로
        inputState = if (newText.isEmpty()) {
            TextFieldValue("", selection = TextRange(0))
        } else {
            TextFieldValue(text = newText, selection = TextRange(newCursorPos))
        }
    }

    private fun canInsertNumber(currentText: String, cursorIndex: Int): Boolean {
        // 전체 텍스트를 구분자로 쪼갰을 때 커서가 위치한 세그먼트 찾기
        // (간단하게 구현하기 위해 커서 앞쪽의 마지막 구분자 이후부터 커서 뒤쪽 첫 구분자 전까지의 길이를 잰다)

        // 1. 커서 앞부분 탐색
        val textBeforeCursor = currentText.take(cursorIndex)
        val lastSeparatorIndexBefore = textBeforeCursor.indexOfLast { it in "+-×÷\n()" }
        val startOfNumber = if (lastSeparatorIndexBefore == -1) 0 else lastSeparatorIndexBefore + 1

        // 2. 커서 뒷부분 탐색
        val textAfterCursor = currentText.drop(cursorIndex)
        val firstSeparatorIndexAfter = textAfterCursor.indexOfFirst { it in "+-×÷\n()" }
        val endOfNumber =
            if (firstSeparatorIndexAfter == -1) currentText.length else cursorIndex + firstSeparatorIndexAfter

        // 3. 현재 숫자 블록 추출 및 길이 체크
        val currentNumberPart = currentText.substring(startOfNumber, endOfNumber)

        // 소수점(.)과 E 표기법 문자는 글자 수 제한에서 유연하게 처리할 수 있으나, 여기선 순수 숫자 길이만 체크하거나 전체 길이 체크
        // 여기서는 E, -, . 을 포함한 전체 길이로 체크합니다.
        return currentNumberPart.length < 15
    }

    private fun getParenthesisToInsert(state: TextFieldValue): String {
        val text = state.text
        val cursor = state.selection.min // 현재 커서 위치

        // 전체 괄호 개수 카운트
        val openCount = text.count { it == '(' }
        val closeCount = text.count { it == ')' }

        // 커서 바로 앞의 글자 확인 (커서가 맨 앞이면 null)
        val charBeforeCursor = if (cursor > 0) text.getOrNull(cursor - 1) else null

        // 1. 닫아야 하는 상황인지 체크
        // (조건: 열린 게 더 많고 + 커서 앞이 숫자거나 닫는 괄호임)
        if (openCount > closeCount && charBeforeCursor != null && (charBeforeCursor.isDigit() || charBeforeCursor == ')')) {
            return ")"
        }

        // 2. 그 외에는 여는 상황
        // (커서 앞이 숫자거나 닫는 괄호면 곱하기 자동 추가)
        if (charBeforeCursor != null && (charBeforeCursor.isDigit() || charBeforeCursor == ')')) {
            return "×("
        }

        // 3. 그냥 열기
        return "("
    }
}