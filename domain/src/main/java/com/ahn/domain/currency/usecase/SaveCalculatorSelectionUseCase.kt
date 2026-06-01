package com.ahn.domain.currency.usecase

import com.ahn.domain.currency.repository.CurrencySelectionRepository
import javax.inject.Inject

class SaveCalculatorSelectionUseCase @Inject constructor(private val repository: CurrencySelectionRepository) {
    suspend operator fun invoke(mainCode: String, subCode: String) {
        repository.saveCalculatorSelection(mainCode, subCode)
    }
}
