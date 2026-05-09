package com.nsai.notes.data.local.db.paging

import androidx.paging.PagingConfig

object PagingConfigFactory {
    val defaultConfig: PagingConfig
        get() = PagingConfig(
            pageSize = 30,
            prefetchDistance = 10,
            enablePlaceholders = false
        )
}
