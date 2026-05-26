package com.nsai.notes.data.local.db.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nsai.notes.data.local.datastore.SettingsDataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SearchHistoryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settingsDataStore: SettingsDataStore
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            settingsDataStore.clearSearchHistory()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
