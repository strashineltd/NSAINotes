package com.nsai.notes.data.local.license

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceKeyGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun generateDeviceId(): String {
        val components = listOf(
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown",
            Build.SERIAL.takeIf { it.isNotBlank() } ?: "unknown",
            Build.MODEL,
            Build.HARDWARE.takeIf { it.isNotBlank() } ?: "generic"
        )
        val raw = components.joinToString("|")
        val hash = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return hash.take(12).joinToString("") { "%02x".format(it) }  // 24 hex chars
    }

    /** Generates display-friendly short device code */
    fun getShortDeviceCode(): String {
        return generateDeviceId().take(12).chunked(4).joinToString("-")
    }
}
