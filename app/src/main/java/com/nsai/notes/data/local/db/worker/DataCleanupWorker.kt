package com.nsai.notes.data.local.db.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nsai.notes.data.local.db.dao.NoteDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DataCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val noteDao: NoteDao
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000 // 30 days
            val expiredNotes = noteDao.getSoftDeletedBefore(cutoff)
            expiredNotes.forEach { noteDao.permanentlyDeleteNote(it) }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount >= 3) Result.failure()
            else Result.retry()
        }
    }
}
