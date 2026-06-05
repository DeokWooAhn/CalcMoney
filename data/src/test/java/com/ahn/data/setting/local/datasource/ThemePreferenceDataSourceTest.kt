package com.ahn.data.setting.local.datasource

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ahn.data.common.datastore.createTestPreferenceDataStore
import com.ahn.domain.setting.model.ThemeMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class ThemePreferenceDataSourceTest :
    DescribeSpec({
        describe("테마 설정 DataStore") {
            it("저장된 테마가 없으면 시스템 모드를 반환한다") {
                runTest {
                    val dataSource = ThemePreferenceDataSource(
                        dataStore = createTestPreferenceDataStore("theme-default"),
                    )

                    dataSource.getThemeMode().first() shouldBe ThemeMode.SYSTEM
                }
            }

            it("저장한 테마 모드를 반환한다") {
                runTest {
                    val dataSource = ThemePreferenceDataSource(
                        dataStore = createTestPreferenceDataStore("theme-save"),
                    )

                    dataSource.saveThemeMode(ThemeMode.DARK)

                    dataSource.getThemeMode().first() shouldBe ThemeMode.DARK
                }
            }

            it("알 수 없는 테마 값이면 시스템 모드를 반환한다") {
                runTest {
                    val dataStore = createTestPreferenceDataStore("theme-invalid")
                    val dataSource = ThemePreferenceDataSource(dataStore = dataStore)
                    val themeModeKey = stringPreferencesKey("theme_mode")

                    dataStore.edit { prefs ->
                        prefs[themeModeKey] = "UNKNOWN"
                    }

                    dataSource.getThemeMode().first() shouldBe ThemeMode.SYSTEM
                }
            }
        }
    })
