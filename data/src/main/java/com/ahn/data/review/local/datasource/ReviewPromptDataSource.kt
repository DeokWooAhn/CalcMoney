package com.ahn.data.review.local.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.ahn.data.di.ReviewPromptPreferencesDataStore
import com.ahn.domain.review.model.ReviewPromptRecord
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewPromptDataSource @Inject constructor(
    @param:ReviewPromptPreferencesDataStore private val dataStore: DataStore<Preferences>,
) {
    private val firstOpenedAtKey = longPreferencesKey("first_opened_at")
    private val lastOpenedEpochDayKey = longPreferencesKey("last_opened_epoch_day")
    private val openedDayCountKey = intPreferencesKey("opened_day_count")
    private val calculationSuccessCountKey = intPreferencesKey("calculation_success_count")
    private val lastRequestedAtKey = longPreferencesKey("last_requested_at")

    /**
     * 저장된 기록을 [transform]으로 고쳐 저장합니다.
     *
     * DataStore의 edit 한 번 안에서 읽고 쓰므로 동시에 들어온 갱신이 서로를 덮어쓰지 않습니다.
     */
    suspend fun updateRecord(transform: (ReviewPromptRecord) -> ReviewPromptRecord): ReviewPromptRecord {
        val updated = dataStore.edit { prefs ->
            prefs.writeRecord(transform(prefs.toRecord()))
        }

        return updated.toRecord()
    }

    private fun Preferences.toRecord(): ReviewPromptRecord {
        return ReviewPromptRecord(
            firstOpenedAt = this[firstOpenedAtKey],
            lastOpenedEpochDay = this[lastOpenedEpochDayKey],
            openedDayCount = this[openedDayCountKey] ?: 0,
            calculationSuccessCount = this[calculationSuccessCountKey] ?: 0,
            lastRequestedAt = this[lastRequestedAtKey],
        )
    }

    private fun MutablePreferences.writeRecord(record: ReviewPromptRecord) {
        setOrRemove(firstOpenedAtKey, record.firstOpenedAt)
        setOrRemove(lastOpenedEpochDayKey, record.lastOpenedEpochDay)
        this[openedDayCountKey] = record.openedDayCount
        this[calculationSuccessCountKey] = record.calculationSuccessCount
        setOrRemove(lastRequestedAtKey, record.lastRequestedAt)
    }

    private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }
}
