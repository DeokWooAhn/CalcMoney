package com.ahn.data.currency.local.datasource

import com.ahn.data.common.datastore.createTestPreferenceDataStore
import com.ahn.domain.currency.model.CalculatorCurrencySelection
import com.ahn.domain.currency.model.ExchangeCurrencySelection
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

class CurrencySelectionDataSourceTest :
    DescribeSpec({
        describe("통화 선택 DataStore") {
            it("저장된 계산기 통화가 없으면 null 선택값을 반환한다") {
                runTest {
                    val dataSource = CurrencySelectionDataSource(
                        dataStore = createTestPreferenceDataStore("currency-empty-calculator"),
                    )

                    dataSource.getCalculatorSelection() shouldBe CalculatorCurrencySelection(
                        mainCode = null,
                        subCode = null,
                    )
                }
            }

            it("계산기 통화 선택값을 저장하고 반환한다") {
                runTest {
                    val dataSource = CurrencySelectionDataSource(
                        dataStore = createTestPreferenceDataStore("currency-calculator"),
                    )

                    dataSource.saveCalculatorSelection(mainCode = "KRW", subCode = "USD")

                    dataSource.getCalculatorSelection() shouldBe CalculatorCurrencySelection(
                        mainCode = "KRW",
                        subCode = "USD",
                    )
                }
            }

            it("계산기 통화 선택값을 개별로 저장하고 반환한다") {
                runTest {
                    val dataSource = CurrencySelectionDataSource(
                        dataStore = createTestPreferenceDataStore("currency-calculator-each"),
                    )

                    dataSource.saveCalculatorMainCurrencyCode("JPY")
                    dataSource.saveCalculatorSubCurrencyCode("EUR")

                    dataSource.getCalculatorSelection() shouldBe CalculatorCurrencySelection(
                        mainCode = "JPY",
                        subCode = "EUR",
                    )
                }
            }

            it("환율 화면 통화 선택값을 저장하고 반환한다") {
                runTest {
                    val dataSource = CurrencySelectionDataSource(
                        dataStore = createTestPreferenceDataStore("currency-exchange"),
                    )

                    dataSource.saveExchangeSelection(fromCode = "USD", toCode = "KRW")

                    dataSource.getExchangeSelection() shouldBe ExchangeCurrencySelection(
                        fromCode = "USD",
                        toCode = "KRW",
                    )
                }
            }

            it("환율 화면 통화 선택값을 개별로 저장하고 반환한다") {
                runTest {
                    val dataSource = CurrencySelectionDataSource(
                        dataStore = createTestPreferenceDataStore("currency-exchange-each"),
                    )

                    dataSource.saveExchangeFromCurrencyCode("AUD")
                    dataSource.saveExchangeToCurrencyCode("CAD")

                    dataSource.getExchangeSelection() shouldBe ExchangeCurrencySelection(
                        fromCode = "AUD",
                        toCode = "CAD",
                    )
                }
            }
        }
    })
