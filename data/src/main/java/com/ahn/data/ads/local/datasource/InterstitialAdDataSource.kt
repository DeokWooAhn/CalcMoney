package com.ahn.data.ads.local.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.ahn.data.di.InterstitialAdPreferencesDataStore
import com.ahn.domain.ads.model.InterstitialAdRecord
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InterstitialAdDataSource @Inject constructor(
    @param:InterstitialAdPreferencesDataStore private val dataStore: DataStore<Preferences>,
) {
    private val firstCalculatedAtKey = longPreferencesKey("first_calculated_at")
    private val calculationCountSinceLastAdKey = intPreferencesKey("calculation_count_since_last_ad")
    private val lastShownAtKey = longPreferencesKey("last_shown_at")
    private val lastShownEpochDayKey = longPreferencesKey("last_shown_epoch_day")
    private val shownCountOfDayKey = intPreferencesKey("shown_count_of_day")

    suspend fun getRecord(): InterstitialAdRecord {
        return dataStore.data.first().toRecord()
    }

    /**
     * 저장된 기록을 [transform]으로 고쳐 저장합니다.
     *
     * DataStore의 edit 한 번 안에서 읽고 쓰므로 동시에 들어온 갱신이 서로를 덮어쓰지 않습니다.
     */
    suspend fun updateRecord(transform: (InterstitialAdRecord) -> InterstitialAdRecord): InterstitialAdRecord {
        val updated = dataStore.edit { prefs ->
            prefs.writeRecord(transform(prefs.toRecord()))
        }

        return updated.toRecord()
    }

    private fun Preferences.toRecord(): InterstitialAdRecord {
        return InterstitialAdRecord(
            firstCalculatedAt = this[firstCalculatedAtKey],
            calculationCountSinceLastAd = this[calculationCountSinceLastAdKey] ?: 0,
            lastShownAt = this[lastShownAtKey],
            lastShownEpochDay = this[lastShownEpochDayKey],
            shownCountOfDay = this[shownCountOfDayKey] ?: 0,
        )
    }

    private fun MutablePreferences.writeRecord(record: InterstitialAdRecord) {
        setOrRemove(firstCalculatedAtKey, record.firstCalculatedAt)
        this[calculationCountSinceLastAdKey] = record.calculationCountSinceLastAd
        setOrRemove(lastShownAtKey, record.lastShownAt)
        setOrRemove(lastShownEpochDayKey, record.lastShownEpochDay)
        this[shownCountOfDayKey] = record.shownCountOfDay
    }

    private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }
}
