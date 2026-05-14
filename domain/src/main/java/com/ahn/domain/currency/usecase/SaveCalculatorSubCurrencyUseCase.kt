package com.ahn.domain.currency.usecase

import com.ahn.domain.currency.repository.CurrencySelectionRepository
import javax.inject.Inject

class SaveCalculatorSubCurrencyUseCase @Inject constructor(
    private val repository: CurrencySelectionRepository,
) {
    suspend operator fun invoke(code: String) {
        repository.saveCalculatorSubCurrencyCode(code)
    }
}