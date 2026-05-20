package com.ahn.data.exchange.repository

import com.ahn.data.exchange.local.datasource.ExchangeRateLocalDataSource
import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import com.ahn.data.exchange.remote.datasource.ExchangeRateRemoteDataSource
import com.ahn.data.exchange.remote.dto.ExchangeRateResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.doubles.shouldBeExactly
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import java.io.IOException

class ExchangeRateRepositoryImplTest : DescribeSpec({
    lateinit var remoteDataSource: ExchangeRateRemoteDataSource
    lateinit var localDataSource: ExchangeRateLocalDataSource
    lateinit var repository: ExchangeRateRepositoryImpl

    beforeTest {
        remoteDataSource = mockk()
        localDataSource = mockk()
        repository = ExchangeRateRepositoryImpl(
            remoteDataSource = remoteDataSource,
            localDataSource = localDataSource,
        )
    }

    describe("환율 Room 캐시") {
        context("신선한 Room 캐시가 있으면") {
            it("원격 데이터를 요청하지 않고 캐시된 환율을 반환한다") {
                runTest {
                    coEvery { localDataSource.getCachedRates() } returns listOf(
                        usdEntity(baseRate = 1300.0),
                    )
                    coEvery { localDataSource.getLatestFetchedAt() } returns System.currentTimeMillis()

                    val rate = repository.getExchangeRate(from = "USD", to = "KRW")

                    rate shouldBeExactly 1300.0
                    coVerify(exactly = 0) { remoteDataSource.fetchExchangeRates(any()) }
                    coVerify(exactly = 0) { localDataSource.replaceRates(any()) }
                }
            }
        }

        context("Room 캐시가 만료되면") {
            it("원격에서 환율을 새로 가져와 로컬 캐시를 교체한다") {
                runTest {
                    coEvery { localDataSource.getCachedRates() } returns listOf(
                        usdEntity(baseRate = 1300.0),
                    )
                    coEvery { localDataSource.getLatestFetchedAt() } returns oldFetchedAt()
                    coEvery { remoteDataSource.fetchExchangeRates(any()) } returns listOf(
                        usdResponse(baseRate = "1,400.0"),
                    )
                    coEvery { localDataSource.replaceRates(any()) } just Runs

                    val rate = repository.getExchangeRate(from = "USD", to = "KRW")

                    rate shouldBeExactly 1400.0
                    coVerify {
                        localDataSource.replaceRates(
                            match { rates ->
                                rates.single().code == "USD" && rates.single().baseRate == 1400.0
                            },
                        )
                    }
                }
            }
        }

        context("캐시 만료 후 원격 요청이 실패하면") {
            it("취소 예외는 기존 캐시로 대체하지 않고 그대로 던진다") {
                runTest {
                    coEvery { localDataSource.getCachedRates() } returns listOf(
                        usdEntity(baseRate = 1300.0),
                    )
                    coEvery { localDataSource.getLatestFetchedAt() } returns oldFetchedAt()
                    coEvery { remoteDataSource.fetchExchangeRates(any()) } throws CancellationException("cancelled")

                    shouldThrow<CancellationException> {
                        repository.getExchangeRate(from = "USD", to = "KRW")
                    }
                    coVerify(exactly = 0) { localDataSource.replaceRates(any()) }
                }
            }

            it("기존 캐시가 있을 때 만료된 Room 캐시로 대체한다") {
                runTest {
                    coEvery { localDataSource.getCachedRates() } returns listOf(
                        usdEntity(baseRate = 1300.0),
                    )
                    coEvery { localDataSource.getLatestFetchedAt() } returns oldFetchedAt()
                    coEvery { remoteDataSource.fetchExchangeRates(any()) } throws IOException("network error")

                    val rate = repository.getExchangeRate(from = "USD", to = "KRW")

                    rate shouldBeExactly 1300.0
                    coVerify(exactly = 0) { localDataSource.replaceRates(any()) }
                }
            }

            it("기존 캐시가 없을 때 원격 오류를 던진다") {
                coEvery { localDataSource.getCachedRates() } returns emptyList()
                coEvery { localDataSource.getLatestFetchedAt() } returns 0L
                coEvery { remoteDataSource.fetchExchangeRates(any()) } throws IOException("network error")

                shouldThrow<IOException> {
                    runTest {
                        repository.getExchangeRate(from = "USD", to = "KRW")
                    }
                }
            }
        }
    }
})

private fun usdEntity(baseRate: Double): ExchangeRateEntity {
    return ExchangeRateEntity(
        code = "USD",
        currencyUnit = "USD",
        currencyName = "US Dollar",
        baseRate = baseRate,
        fetchedAt = System.currentTimeMillis(),
    )
}

private fun usdResponse(baseRate: String): ExchangeRateResponse {
    return ExchangeRateResponse(
        result = 1,
        currencyUnit = "USD",
        currencyName = "US Dollar",
        baseRate = baseRate,
    )
}

private fun oldFetchedAt(): Long {
    return System.currentTimeMillis() - (2 * 60 * 60 * 1000L)
}
