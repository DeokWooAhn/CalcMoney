package com.ahn.data.exchange.repository

import com.ahn.data.exchange.local.datasource.ExchangeRateLocalDataSource
import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import com.ahn.data.exchange.remote.datasource.ExchangeRateRemoteDataSource
import com.ahn.data.exchange.remote.dto.ExchangeRateResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.shouldBe
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
                it("원격 데이터를 요청하지 않고 캐시된 환율을 반환한다") {
                    runTest {
                        coEvery { localDataSource.getCachedRates() } returns listOf(
                            usdEntity(baseRate = 1300.0),
                        )
                        coEvery { localDataSource.getLatestFetchedAt() } returns testClock.millis()

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
                                    rates.single().code == "USD" &&
                                        rates.single().baseRate == 1400.0 &&
                                        rates.single().rateDate == "20260601"
                                },
                            )
                        }
                    }
                }

                it("오늘 환율이 비어 있고 직전 영업일 환율이 유효하면 캐시에 저장한다") {
                    runTest {
                        coEvery { localDataSource.getCachedRates() } returns emptyList()
                        coEvery { localDataSource.getLatestFetchedAt() } returns 0L
                        coEvery { remoteDataSource.fetchExchangeRates("") } returns emptyList()
                        coEvery { remoteDataSource.fetchExchangeRates(match { it.isNotBlank() }) } returns listOf(
                            usdResponse(baseRate = "1,400.0"),
                        )
                        coEvery { localDataSource.replaceRates(any()) } just Runs

                        val rate = repository.getExchangeRate(from = "USD", to = "KRW")

                        rate shouldBeExactly 1400.0
                        coVerify { remoteDataSource.fetchExchangeRates("") }
                        coVerify { remoteDataSource.fetchExchangeRates(match { it.isNotBlank() }) }
                        coVerify {
                            localDataSource.replaceRates(
                                match { rates ->
                                    rates.single().code == "USD" &&
                                        rates.single().baseRate == 1400.0 &&
                                        rates.single().rateDate == "20260529"
                                },
                            )
                        }
                    }
                }

                it("주말에는 직전 영업일 날짜로 fallback한다") {
                    runTest {
                        repository = ExchangeRateRepositoryImpl(
                            remoteDataSource = remoteDataSource,
                            localDataSource = localDataSource,
                            clock = Clock.fixed(
                                Instant.parse("2026-05-31T01:00:00Z"),
                                ZoneId.of("Asia/Seoul"),
                            ),
                        )
                        coEvery { localDataSource.getCachedRates() } returns emptyList()
                        coEvery { localDataSource.getLatestFetchedAt() } returns 0L
                        coEvery { remoteDataSource.fetchExchangeRates("") } returns emptyList()
                        coEvery { remoteDataSource.fetchExchangeRates("20260529") } returns listOf(
                            usdResponse(baseRate = "1,400.0"),
                        )
                        coEvery { localDataSource.replaceRates(any()) } just Runs

                        val rate = repository.getExchangeRate(from = "USD", to = "KRW")

                        rate shouldBeExactly 1400.0
                        coVerify { remoteDataSource.fetchExchangeRates("20260529") }
                        coVerify(exactly = 0) { remoteDataSource.fetchExchangeRates("20260530") }
                        coVerify {
                            localDataSource.replaceRates(
                                match { rates ->
                                    rates.single().rateDate == "20260529"
                                },
                            )
                        }
                    }
                }

                it("자정 경계에서는 주입된 Clock의 시간대 기준 날짜를 사용한다") {
                    runTest {
                        repository = ExchangeRateRepositoryImpl(
                            remoteDataSource = remoteDataSource,
                            localDataSource = localDataSource,
                            clock = Clock.fixed(
                                Instant.parse("2026-05-29T15:30:00Z"),
                                ZoneId.of("Asia/Seoul"),
                            ),
                        )
                        coEvery { localDataSource.getCachedRates() } returns emptyList()
                        coEvery { localDataSource.getLatestFetchedAt() } returns 0L
                        coEvery { remoteDataSource.fetchExchangeRates("") } returns emptyList()
                        coEvery { remoteDataSource.fetchExchangeRates("20260529") } returns listOf(
                            usdResponse(baseRate = "1,400.0"),
                        )
                        coEvery { localDataSource.replaceRates(any()) } just Runs

                        val rate = repository.getExchangeRate(from = "USD", to = "KRW")

                        rate shouldBeExactly 1400.0
                        coVerify { remoteDataSource.fetchExchangeRates("20260529") }
                        coVerify(exactly = 0) { remoteDataSource.fetchExchangeRates("20260528") }
                        coVerify {
                            localDataSource.replaceRates(
                                match { rates ->
                                    rates.single().rateDate == "20260529"
                                },
                            )
                        }
                    }
                }
            }

            context("캐시 만료 후 원격 요청이 실패하면") {
                it("오늘과 이전 날짜 환율이 모두 비어 있고 기존 캐시가 있으면 캐시를 사용한다") {
                    runTest {
                        coEvery { localDataSource.getCachedRates() } returns listOf(
                            usdEntity(baseRate = 1300.0),
                        )
                        coEvery { localDataSource.getLatestFetchedAt() } returns oldFetchedAt()
                        coEvery { remoteDataSource.fetchExchangeRates(any()) } returns emptyList()

                        val rate = repository.getExchangeRate(from = "USD", to = "KRW")

                        rate shouldBeExactly 1300.0
                        coVerify(exactly = 0) { localDataSource.replaceRates(any()) }
                    }
                }

                it("오늘과 이전 날짜 환율이 모두 비어 있고 캐시도 없으면 사용자 친화적인 오류를 던진다") {
                    runTest {
                        coEvery { localDataSource.getCachedRates() } returns emptyList()
                        coEvery { localDataSource.getLatestFetchedAt() } returns 0L
                        coEvery { remoteDataSource.fetchExchangeRates(any()) } returns emptyList()

                        val error = shouldThrow<IllegalStateException> {
                            repository.getExchangeRate(from = "USD", to = "KRW")
                        }

                        error.message shouldBe "오늘 환율이 아직 고시되지 않았습니다. 잠시 후 다시 시도해 주세요."
                        coVerify(exactly = 0) { localDataSource.replaceRates(any()) }
                    }
                }

                it("인증 오류 응답은 직전 영업일 fallback 없이 인증 오류를 던진다") {
                    runTest {
                        coEvery { localDataSource.getCachedRates() } returns emptyList()
                        coEvery { localDataSource.getLatestFetchedAt() } returns 0L
                        coEvery { remoteDataSource.fetchExchangeRates("") } returns listOf(
                            errorResponse(result = 3),
                        )

                        val error = shouldThrow<IllegalStateException> {
                            repository.getExchangeRate(from = "USD", to = "KRW")
                        }

                        error.message shouldBe "Invalid exchange rate API authKey (result=3)"
                        coVerify(exactly = 0) { remoteDataSource.fetchExchangeRates(match { it.isNotBlank() }) }
                        coVerify(exactly = 0) { localDataSource.replaceRates(any()) }
                    }
                }

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
        fetchedAt = testClock.millis(),
        rateDate = "20260601",
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

private fun errorResponse(result: Int): ExchangeRateResponse {
    return ExchangeRateResponse(result = result)
}

private fun oldFetchedAt(): Long {
    return testClock.millis() - (2 * 60 * 60 * 1000L)
}
