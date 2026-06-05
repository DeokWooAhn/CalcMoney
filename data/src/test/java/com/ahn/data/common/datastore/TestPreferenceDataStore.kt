package com.ahn.data.common.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.test.TestScope
import java.nio.file.Files

fun TestScope.createTestPreferenceDataStore(name: String): DataStore<Preferences> {
    val file = Files.createTempDirectory(name)
        .resolve("test.preferences_pb")
        .toFile()

    return PreferenceDataStoreFactory.create(
        scope = backgroundScope,
        produceFile = { file },
    )
}
