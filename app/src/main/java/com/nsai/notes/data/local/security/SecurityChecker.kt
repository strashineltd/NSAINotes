package com.nsai.notes.data.local.security

import android.os.Build
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityChecker @Inject constructor() {

    // Detect common root indicators
    val isRooted: Boolean by lazy {
        detectRoot() || detectMagisk() || detectSuBinary()
    }

    // Detect if running in emulator
    val isEmulator: Boolean by lazy {
        (Build.FINGERPRINT.startsWith("google/sdk_gphone") ||
         Build.FINGERPRINT.startsWith("generic") ||
         Build.MODEL.contains("Emulator") ||
         Build.MODEL.contains("Android SDK") ||
         Build.HARDWARE.contains("goldfish") ||
         Build.HARDWARE.contains("ranchu"))
    }

    // Detect if debugger is attached
    val isDebugged: Boolean
        get() = android.os.Debug.isDebuggerConnected()

    // Detect common hooking framework files
    val isHooked: Boolean by lazy {
        detectHookFrameworks()
    }

    // Overall threat level
    enum class ThreatLevel { SAFE, SUSPICIOUS, HIGH, COMPROMISED }

    val threatLevel: ThreatLevel
        get() = when {
            isRooted && isHooked -> ThreatLevel.COMPROMISED
            isRooted -> ThreatLevel.HIGH
            isEmulator || isDebugged -> ThreatLevel.SUSPICIOUS
            else -> ThreatLevel.SAFE
        }

    fun onThreatDetected(action: () -> Unit) {
        if (threatLevel >= ThreatLevel.HIGH) {
            action()
        }
    }

    // --- Detection methods ---

    private fun detectRoot(): Boolean {
        val rootPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        return rootPaths.any { File(it).exists() }
    }

    private fun detectMagisk(): Boolean {
        val magiskPaths = arrayOf(
            "/sbin/.magisk",
            "/data/adb/magisk",
            "/data/adb/modules"
        )
        return magiskPaths.any { File(it).exists() }
    }

    private fun detectSuBinary(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            process.waitFor()
            process.exitValue() == 0
        } catch (_: Exception) { false }
    }

    private fun detectHookFrameworks(): Boolean {
        val hookFiles = arrayOf(
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/system/lib/libfrida-gadget.so",
            "/data/local/tmp/xposed",
            "/data/app/de.robv.android.xposed.installer"
        )
        return hookFiles.any { File(it).exists() } ||
            detectSuspiciousPorts()
    }

    private fun detectSuspiciousPorts(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("cat", "/proc/net/tcp"))
            val output = process.inputStream.bufferedReader().readText()
            // Frida default port 27042
            val fridaHexPort = "69A2" // 27042 in hex
            output.contains(fridaHexPort)
        } catch (_: Exception) { false }
    }
}
