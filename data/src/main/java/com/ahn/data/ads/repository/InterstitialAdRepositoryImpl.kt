package com.ahn.data.ads.repository

import com.ahn.data.ads.local.datasource.InterstitialAdDataSource
import com.ahn.domain.ads.model.InterstitialAdRecord
import com.ahn.domain.ads.repository.InterstitialAdRepository
import javax.inject.Inject

class InterstitialAdRepositoryImpl @Inject constructor(
    private val dataSource: InterstitialAdDataSource,
) : InterstitialAdRepository {
    override suspend fun getRecord(): InterstitialAdRecord {
        return dataSource.getRecord()
    }

    override suspend fun updateRecord(transform: (InterstitialAdRecord) -> InterstitialAdRecord): InterstitialAdRecord {
        return dataSource.updateRecord(transform)
    }
}
