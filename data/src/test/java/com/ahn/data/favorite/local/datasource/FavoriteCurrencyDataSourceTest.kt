package com.ahn.data.favorite.local.datasource

import com.ahn.data.common.datastore.createTestPreferenceDataStore
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class FavoriteCurrencyDataSourceTest :
    DescribeSpec({
        describe("즐겨찾기 통화 DataStore") {
            it("저장된 즐겨찾기가 없으면 빈 목록을 반환한다") {
                runTest {
                    val dataSource = FavoriteCurrencyDataSource(
                        dataStore = createTestPreferenceDataStore("favorite-empty"),
                    )

                    dataSource.getFavoriteCodes().first() shouldBe emptyList()
                }
            }

            it("즐겨찾기 통화를 추가 순서대로 저장한다") {
                runTest {
                    val dataSource = FavoriteCurrencyDataSource(
                        dataStore = createTestPreferenceDataStore("favorite-add"),
                    )

                    dataSource.addFavorite("USD")
                    dataSource.addFavorite("JPY")

                    dataSource.getFavoriteCodes().first() shouldContainExactly listOf("USD", "JPY")
                }
            }

            it("이미 추가된 즐겨찾기는 중복 저장하지 않는다") {
                runTest {
                    val dataSource = FavoriteCurrencyDataSource(
                        dataStore = createTestPreferenceDataStore("favorite-duplicate"),
                    )

                    dataSource.addFavorite("USD")
                    dataSource.addFavorite("USD")

                    dataSource.getFavoriteCodes().first() shouldContainExactly listOf("USD")
                }
            }

            it("즐겨찾기 통화를 제거한다") {
                runTest {
                    val dataSource = FavoriteCurrencyDataSource(
                        dataStore = createTestPreferenceDataStore("favorite-remove"),
                    )

                    dataSource.addFavorite("USD")
                    dataSource.addFavorite("JPY")
                    dataSource.removeFavorite("USD")

                    dataSource.getFavoriteCodes().first() shouldContainExactly listOf("JPY")
                }
            }

            it("즐겨찾기 여부를 반환한다") {
                runTest {
                    val dataSource = FavoriteCurrencyDataSource(
                        dataStore = createTestPreferenceDataStore("favorite-check"),
                    )

                    dataSource.addFavorite("USD")

                    dataSource.isFavorite("USD") shouldBe true
                    dataSource.isFavorite("JPY") shouldBe false
                }
            }
        }
    })
