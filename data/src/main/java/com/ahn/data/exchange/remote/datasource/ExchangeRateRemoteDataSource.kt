package com.ahn.data.exchange.remote.datasource

import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ExchangeRateRemoteDataSource @Inject constructor(private val firestore: FirebaseFirestore) {
    private companion object {
        const val EXCHANGE_RATES_COLLECTION = "exchangeRates"
        const val LATEST_DOCUMENT_ID = "latest"
        const val ERROR_EXCHANGE_RATES_NOT_READY = "환율 서버 데이터가 아직 준비되지 않았습니다."
    }

    /**
     * Firebase 서버 캐시에 저장된 최신 환율 목록을 가져옵니다.
     *
     * 앱은 한국수출입은행 API를 직접 호출하지 않고, Cloud Functions가 갱신한 Firestore 문서를 읽습니다.
     *
     * @return 서버 캐시에 저장된 환율 엔티티 목록입니다.
     */
    suspend fun fetchExchangeRates(): List<ExchangeRateEntity> {
        val snapshot = firestore
            .collection(EXCHANGE_RATES_COLLECTION)
            .document(LATEST_DOCUMENT_ID)
            .get()
            .await()

        val rateDate = snapshot.getString("rateDate").orEmpty()
        val fetchedAt = snapshot.getLong("fetchedAt") ?: 0L
        val rates = snapshot.get("rates") as? List<*> ?: emptyList<Any>()
        val entities = rates.mapNotNull { it.toExchangeRateEntity(fetchedAt, rateDate) }

        if (entities.isEmpty()) {
            throw IllegalStateException(snapshot.getString("message") ?: ERROR_EXCHANGE_RATES_NOT_READY)
        }

        return entities
    }

    private fun Any?.toExchangeRateEntity(
        fetchedAt: Long,
        rateDate: String,
    ): ExchangeRateEntity? {
        val rate = this as? Map<*, *> ?: return null
        val code = rate["code"] as? String ?: return null
        val baseRate = rate["baseRate"].toDoubleOrNull() ?: return null

        return ExchangeRateEntity(
            code = code,
            currencyUnit = rate["currencyUnit"] as? String ?: code,
            currencyName = rate["currencyName"] as? String ?: "Unknown",
            baseRate = baseRate,
            fetchedAt = fetchedAt,
            rateDate = rateDate,
        )
    }

    private fun Any?.toDoubleOrNull(): Double? {
        return when (this) {
            is Number -> toDouble()
            is String -> replace(",", "").toDoubleOrNull()
            else -> null
        }
    }
}
