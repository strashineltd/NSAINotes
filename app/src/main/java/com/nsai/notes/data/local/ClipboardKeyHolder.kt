package com.nsai.notes.data.local

object ClipboardKeyHolder {
    @Volatile
    var pendingKey: String? = null
}
