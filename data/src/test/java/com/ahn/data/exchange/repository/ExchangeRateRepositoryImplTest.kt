package com.ahn.data.exchange.repository

import com.ahn.data.exchange.local.datasource.ExchangeRateLocalDataSource
import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import com.ahn.data.exchange.remote.datasource.ExchangeRateRemoteDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeExactly
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

private val testClock: Clock = Clock.fixed(
    Instant.parse("2026-06-01T00:30:00Z"),
    ZoneId.of("Asia/Seoul"),
)

class ExchangeRateRepositoryImplTest :
    DescribeSpec({
        lateinit var remoteDataSource: ExchangeRateRemoteDataSource
        lateinit var localDataSource: ExchangeRateLocalDataSource
        lateinit var repository: ExchangeRateRepositoryImpl

        beforeTest {
            remoteDataSource = mockk()
            localDataSource = mockk()
            repository = ExchangeRateRepositoryImpl(
                remoteDataSource = remoteDataSource,
                localDataSource = localDataSource,
                clock = testClock,
            )
        }

        describe("환율 Room 캐시") {
            context("신선한 Room 캐시가 있으면") {
                it("Firebase 서버 캐시를 요청하지 않고 캐시된 환율을 반환한다") {
                    runTest {
                        coEvery { localDataSource.getCachedRates() } returns listOf(
                            usdEntity(baseRate = 1300.0, fetchedAt = testClock.millis()),
                        )
                        coEvery { localDataSource.getLatestFetchedAt() } returns testClock.millis()

                        val rate = repository.getExchangeRate(from = "USD", to = "KRW")

                        rate shouldBeExactly 1300.0
                        coVerify(exactly = 0) { remoteDataSource.fetchExchangeRates() }
                        coVerify(exactly = 0) { localDataSource.replaceRates(any()) }
                    }
                }
            }

            context("Room 캐시가 만료되면") {
                it("Firebase 서버 캐시에서 환율을 가져와 로컬 캐시를 교체한다") {
                    runTest {
                        val remoteRates = listOf(
                            usdEntity(baseRate = 1400.0, fetchedAt = testClock.millis()),
                        )
                        coEvery { localDataSource.getCachedRates() } returns listOf(
                            usdEntity(baseRate = 1300.0, fetchedAt = oldFetchedAt()),
                        )
                        coEvery { localDataSource.getLatestFetchedAt() } returns oldFetchedAt()
                        coEvery { remoteDataSource.fetchExchangeRates() } returns remoteRates
                        coEvery { localDataSource.replaceRates(any()) } just Runs

                        val rate = repository.getExchangeRate(from = "USD", to = "KRW")

                        rate shouldBeExactly 1400.0
                        coVerify { localDataSource.replaceRates(remoteRates) }
                    }
                }

                it("지원 통화 목록에 KRW와 서버 캐시 통화를 포함한다") {
                    runTest {
                        coEvery { localDataSource.getCachedRates() } returns emptyList()
                        coEvery { localDataSource.getLatestFetchedAt() } returns 0L
                        coEvery { remoteDataSource.fetchExchangeRates() } returns listOf(
                            usdEntity(baseRate = 1400.0, fetchedAt = testClock.millis()),
                        )
                        coEvery { localDataSource.replaceRates(any()) } just Runs

                        val currencies = repository.getSupportedCurrencies().map { it.code }

                        currencies.shouldContainExactly("KRW", "USD")
                    }
                }
            }

            context("캐시 만료 후 Firebase 서버 캐시 요청이 실패하면") {
                it("기존 캐시가 있을 때 만료된 Room 캐시를 사용한다") {
                    runTest {
                        coEvery { localDataSource.getCachedRates() } returns listOf(
                            usdEntity(baseRate = 1300.0, fetchedAt = oldFetchedAt()),
                        )
                        coEvery { localDataSource.getLatestFetchedAt() } returns oldFetchedAt()
                        coEvery { remoteDataSource.fetchExchangeRates() } throws IOException("network error")

                        val rate = repository.getExchangeRate(from = "USD", to = "KRW")

                        rate shouldBeExactly 1300.0
                        coVerify(exactly = 0) { localDataSource.replaceRates(any()) }
                    }
                }

                it("기존 캐시가 없을 때 원격 오류를 던진다") {
                    runTest {
                        coEvery { localDataSource.getCachedRates() } returns emptyList()
                        coEvery { localDataSource.getLatestFetchedAt() } returns 0L
                        coEvery { remoteDataSource.fetchExchangeRates() } throws IOException("network error")

                        shouldThrow<IOException> {
                            repository.getExchangeRate(from = "USD", to = "KRW")
                        }
                    }
                }

                it("취소 예외는 기존 캐시로 대체하지 않고 그대로 던진다") {
                    runTest {
                        coEvery { localDataSource.getCachedRates() } returns listOf(
                            usdEntity(baseRate = 1300.0, fetchedAt = oldFetchedAt()),
                        )
                        coEvery { localDataSource.getLatestFetchedAt() } returns oldFetchedAt()
                        coEvery { remoteDataSource.fetchExchangeRates() } throws CancellationException("cancelled")

                        shouldThrow<CancellationException> {
                            repository.getExchangeRate(from = "USD", to = "KRW")
                        }
                        coVerify(exactly = 0) { localDataSource.replaceRates(any()) }
                    }
                }
            }

            context("환율 새로고침을 요청하면") {
                it("Firebase 서버 캐시에서 최신 환율을 가져와 로컬 캐시를 교체한다") {
                    runTest {
                        val remoteRates = listOf(
                            usdEntity(baseRate = 1400.0, fetchedAt = testClock.millis()),
                        )
                        coEvery { remoteDataSource.fetchExchangeRates() } returns remoteRates
                        coEvery { localDataSource.replaceRates(any()) } just Runs

                        repository.refreshExchangeRates()

                        coVerify { localDataSource.replaceRates(remoteRates) }
                    }
                }
            }
        }
    })

private fun usdEntity(
    baseRate: Double,
    fetchedAt: Long,
): ExchangeRateEntity {
    return ExchangeRateEntity(
        code = "USD",
        currencyUnit = "USD",
        currencyName = "US Dollar",
        baseRate = baseRate,
        fetchedAt = fetchedAt,
        rateDate = "20260601",
    )
}

private fun oldFetchedAt(): Long {
    return testClock.millis() - (13 * 60 * 60 * 1000L)
}
