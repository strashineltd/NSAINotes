package com.nsai.notes.data.local.db.worker

object NSAIWorkers {
    const val DATA_CLEANUP = "nsai_data_cleanup"
    const val SEARCH_HISTORY_CLEANUP = "nsai_search_history_cleanup"

    val DEFAULT_CLEANUP_INTERVAL_HOURS: Long = 24
    val DEFAULT_HISTORY_CLEANUP_INTERVAL_HOURS: Long = 168
}

