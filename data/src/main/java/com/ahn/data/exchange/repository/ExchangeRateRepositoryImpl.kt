package com.ahn.data.exchange.repository

import com.ahn.data.exchange.local.datasource.ExchangeRateLocalDataSource
import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import com.ahn.data.exchange.mapper.krwCurrencyInfo
import com.ahn.data.exchange.mapper.rateOf
import com.ahn.data.exchange.mapper.toCurrencyInfo
import com.ahn.data.exchange.mapper.toEntity
import com.ahn.data.exchange.remote.datasource.ExchangeRateRemoteDataSource
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.domain.exchange.repository.ExchangeRateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExchangeRateRepositoryImpl internal constructor(
    private val remoteDataSource: ExchangeRateRemoteDataSource,
    private val localDataSource: ExchangeRateLocalDataSource,
    private val clock: Clock,
) : ExchangeRateRepository {

    @Inject
    constructor(
        remoteDataSource: ExchangeRateRemoteDataSource,
        localDataSource: ExchangeRateLocalDataSource,
    ) : this(
        remoteDataSource = remoteDataSource,
        localDataSource = localDataSource,
        clock = Clock.systemDefaultZone(),
    )

    companion object {
        private const val CACHE_TTL_MS = 60 * 60 * 1000L
        private const val MAX_FALLBACK_DAYS = 7
        private const val ERROR_EXCHANGE_RATE_NOT_FOUND = "Exchange rate data not found"
        private const val ERROR_EXCHANGE_RATE_NOT_ANNOUNCED =
            "오늘 환율이 아직 고시되지 않았습니다. 잠시 후 다시 시도해 주세요."
        private val searchDateFormatter = DateTimeFormatter.BASIC_ISO_DATE
    }

    private val refreshMutex = Mutex()

    /**
     * 현재 사용할 환율 목록을 가져옵니다.
     *
     * 캐시가 유효하면 로컬 데이터를 사용하고, 캐시가 만료되었으면 원격 데이터를 가져와 캐시를 갱신합니다.
     * 동시에 여러 갱신 작업이 실행되지 않도록 [refreshMutex]로 보호합니다.
     *
     * @return 유효한 캐시 또는 새로 가져와 저장한 환율 엔티티 목록입니다.
     * @throws IllegalStateException 원격 응답에 유효한 환율이 없고 사용할 캐시도 없을 때 발생합니다.
     * @throws Exception 원격 조회 또는 로컬 저장에 실패했고 사용할 캐시도 없을 때 원래 예외를 다시 던집니다.
     */
    private suspend fun fetchRatesIfNeeded(): List<ExchangeRateEntity> {
        return refreshMutex.withLock {
            val now = clock.millis()
            val cached = localDataSource.getCachedRates()
            val fetchedAt = localDataSource.getLatestFetchedAt()

            if (cached.isNotEmpty() && now - fetchedAt < CACHE_TTL_MS) {
                return@withLock cached
            }

            runCatching {
                val valid = fetchValidRates(now)
                localDataSource.replaceRates(valid)
                valid
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                cached.ifEmpty { throw error }
            }
        }
    }

    /**
     * 기준 통화에서 대상 통화로 변환할 환율을 계산합니다.
     *
     * @param from 기준 통화 코드입니다.
     * @param to 대상 통화 코드입니다.
     * @return `from` 1단위를 `to` 단위로 변환하기 위한 배율입니다.
     * @throws IllegalStateException 현재 환율 목록에서 둘 중 하나의 통화 코드를 찾을 수 없을 때 발생합니다.
     */
    override suspend fun getExchangeRate(from: String, to: String): Double {
        if (from == to) return 1.0

        val rates = fetchRatesIfNeeded()
        val fromRate = rates.rateOf(from) ?: throw IllegalStateException(ERROR_EXCHANGE_RATE_NOT_FOUND)
        val toRate = rates.rateOf(to) ?: throw IllegalStateException(ERROR_EXCHANGE_RATE_NOT_FOUND)

        return fromRate / toRate
    }

    override suspend fun getLatestRateDate(): String {
        return localDataSource.getLatestRateDate()
    }

    /**
     * 지원 통화 목록을 반환합니다.
     *
     * 기준 통화인 KRW를 항상 포함하고, 현재 환율 목록에서 얻은 통화를 더한 뒤 통화 코드 기준으로 중복을 제거합니다.
     */
    override suspend fun getSupportedCurrencies(): List<CurrencyInfo> {
        val rates = fetchRatesIfNeeded()

        return (listOf(krwCurrencyInfo()) + rates.map { it.toCurrencyInfo() })
            .distinctBy { it.code }
    }

    /**
     * 환율 API 결과 코드를 사용자에게 보여줄 오류 메시지로 변환합니다.
     *
     * @param resultCode 환율 응답의 결과 코드입니다. 값이 없으면 `null`입니다.
     * @return 코드 2, 3, 4에 해당하는 메시지입니다. 그 외에는 기본 환율 없음 메시지를 반환합니다.
     */
    private fun apiErrorMessage(resultCode: Int?): String {
        return when (resultCode) {
            2 -> "Invalid exchange rate API request (result=2)"
            3 -> "Invalid exchange rate API authKey (result=3)"
            4 -> "Exchange rate API daily request limit exceeded (result=4)"
            else -> ERROR_EXCHANGE_RATE_NOT_ANNOUNCED
        }
    }

    private suspend fun fetchValidRates(fetchedAt: Long): List<ExchangeRateEntity> {
        var lastResultCode: Int? = null

        for (searchDate in exchangeRateSearchDates()) {
            val responses = remoteDataSource.fetchExchangeRates(searchDate)
            lastResultCode = responses.firstOrNull()?.result

            if (lastResultCode in 2..4) {
                throw IllegalStateException(apiErrorMessage(lastResultCode))
            }

            val valid = responses.mapNotNull { it.toEntity(fetchedAt, searchDate.toRateDate()) }
            if (valid.isNotEmpty()) return valid
        }

        throw IllegalStateException(apiErrorMessage(lastResultCode))
    }

    private fun exchangeRateSearchDates(): List<String> {
        val today = LocalDate.now(clock)
        val previousBusinessDates = generateSequence(today.minusDays(1)) { it.minusDays(1) }
            .filterNot { it.dayOfWeek == DayOfWeek.SATURDAY || it.dayOfWeek == DayOfWeek.SUNDAY }
            .take(MAX_FALLBACK_DAYS)
            .map { it.format(searchDateFormatter) }
            .toList()

        return listOf("") + previousBusinessDates
    }

    private fun String.toRateDate(): String {
        return if (isBlank()) {
            LocalDate.now(clock).format(searchDateFormatter)
        } else {
            this
        }
    }
}
