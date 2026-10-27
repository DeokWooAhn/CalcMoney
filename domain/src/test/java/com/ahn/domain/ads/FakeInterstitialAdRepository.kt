package com.ahn.domain.ads

import com.ahn.domain.ads.model.InterstitialAdRecord
import com.ahn.domain.ads.repository.InterstitialAdRepository

/** 메모리에 기록을 들고 있는 테스트용 저장소. */
class FakeInterstitialAdRepository(
    var record: InterstitialAdRecord = InterstitialAdRecord(),
) : InterstitialAdRepository {
    override suspend fun getRecord(): InterstitialAdRecord = record

    override suspend fun updateRecord(transform: (InterstitialAdRecord) -> InterstitialAdRecord): InterstitialAdRecord {
        record = transform(record)
        return record
    }
}
