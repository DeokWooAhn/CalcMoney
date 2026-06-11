package com.ahn.data.exchange.remote.datasource

import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import com.ahn.data.exchange.remote.mapper.toExchangeRateEntities
import com.ahn.domain.exchange.model.ExchangeRateException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

class ExchangeRateRemoteDataSource @Inject constructor(private val firestore: FirebaseFirestore) {
    /**
     * Firebase 서버 캐시에 저장된 최신 환율 목록을 가져옵니다.
     *
     * 앱은 한국수출입은행 API를 직접 호출하지 않고, Cloud Functions가 갱신한 Firestore 문서를 읽습니다.
     *
     * @return 서버 캐시에 저장된 환율 엔티티 목록입니다.
     */
    suspend fun fetchExchangeRates(): List<ExchangeRateEntity> {
        val snapshot = fetchLatestSnapshot().requireExists()
        val rateDate = snapshot.getString("rateDate").orEmpty()
        val fetchedAt = snapshot.requireFetchedAt()
        val entities = snapshot.get("rates").toExchangeRateEntities(fetchedAt, rateDate)

        return entities.ifNotEmptyOrThrow(snapshot)
    }

    private suspend fun fetchLatestSnapshot(): DocumentSnapshot {
        return try {
            firestore
                .collection(EXCHANGE_RATES_COLLECTION)
                .document(LATEST_DOCUMENT_ID)
                .get()
                .await()
        } catch (e: FirebaseFirestoreException) {
            throw e.toExchangeRateException()
        }
    }

    private fun DocumentSnapshot.requireExists(): DocumentSnapshot {
        if (!exists()) throw ExchangeRateException.NotReady()

        return this
    }

    private fun DocumentSnapshot.requireFetchedAt(): Long {
        return getLong("rateFetchedAt")
            ?: getLong("fetchedAt")
            ?: throw ExchangeRateException.TemporarilyUnavailable(
                NoSuchElementException(ERROR_FETCHED_AT_MISSING),
            )
    }

    private fun List<ExchangeRateEntity>.ifNotEmptyOrThrow(snapshot: DocumentSnapshot): List<ExchangeRateEntity> {
        if (isEmpty()) throw snapshot.toEmptyExchangeRatesException()

        return this
    }

    private fun FirebaseFirestoreException.toExchangeRateException(): ExchangeRateException {
        return when (code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            -> ExchangeRateException.NetworkUnavailable(this)

            else -> ExchangeRateException.TemporarilyUnavailable(this)
        }
    }

    private fun DocumentSnapshot.toEmptyExchangeRatesException(): ExchangeRateException {
        val lastError = getString("lastError").orEmpty()

        return if (lastError.isNotReadyError()) {
            ExchangeRateException.NotReady()
        } else {
            ExchangeRateException.TemporarilyUnavailable()
        }
    }

    private fun String.isNotReadyError(): Boolean {
        val normalized = trim().lowercase(Locale.ROOT)

        // Cloud Functions currently stores upstream no-data cases as text in lastError.
        return NOT_READY_ERROR_PATTERNS.any { normalized.contains(it) }
    }

    private companion object {
        const val EXCHANGE_RATES_COLLECTION = "exchangeRates"
        const val LATEST_DOCUMENT_ID = "latest"
        const val ERROR_FETCHED_AT_MISSING = "Exchange rate fetched timestamp is missing."
        val NOT_READY_ERROR_PATTERNS = listOf("아직 고시", "no data")
    }
}
