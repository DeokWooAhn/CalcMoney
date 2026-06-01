package com.ahn.domain.currency.usecase

import com.ahn.domain.currency.repository.CurrencySelectionRepository
import javax.inject.Inject

class SaveExchangeSelectionUseCase @Inject constructor(private val repository: CurrencySelectionRepository) {
    suspend operator fun invoke(fromCode: String, toCode: String) {
        repository.saveExchangeSelection(fromCode, toCode)
    }
}
