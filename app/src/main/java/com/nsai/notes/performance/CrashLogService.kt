package com.nsai.notes.performance

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

data class CrashRecord(
    val timestamp: Long,
    val type: String,       // CRASH, ERROR, WARNING
    val message: String,
    val stackTrace: String,
    val triggerInfo: String // screen/action context
) {
    val formattedTime: String
        get() = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

    val summary: String
        get() = "[$formattedTime] $type: $message"
}

@Singleton
class CrashLogService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val crashLogDir = File(context.filesDir, "crash_logs")
    private val crashLogFile = File(crashLogDir, "crash_history.txt")

    @Volatile var lastScreen: String = "unknown"
    @Volatile var lastAction: String = "none"
    @Volatile var lastInput: String = ""

    private val crashRecords = Collections.synchronizedList(mutableListOf<CrashRecord>())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    @Volatile private var historyLoaded = false

    init {
        crashLogDir.mkdirs()
        installExceptionHandler()
    }

    private fun ensureHistoryLoaded() {
        if (!historyLoaded) { loadHistory(); historyLoaded = true }
    }

    fun recordScreen(screen: String) {
        lastScreen = screen
    }

    fun recordAction(action: String) {
        lastAction = action
    }

    fun recordInput(input: String) {
        if (input.length > 100) lastInput = input.take(100) + "..."
        else lastInput = input
    }

    fun log(level: String, message: String, stackTrace: String = "") {
        val trigger = "屏幕: $lastScreen | 操作: $lastAction${if (lastInput.isNotBlank()) " | 输入: $lastInput" else ""}"
        val record = CrashRecord(
            timestamp = System.currentTimeMillis(),
            type = level,
            message = message,
            stackTrace = stackTrace,
            triggerInfo = trigger
        )
        crashRecords.add(0, record) // newest first
        if (crashRecords.size > 200) crashRecords.removeAt(crashRecords.size - 1)
        appendToFile(record)
    }

    fun getRecords(typeFilter: String? = null): List<CrashRecord> {
        ensureHistoryLoaded()
        return if (typeFilter != null) crashRecords.filter { it.type == typeFilter }
        else crashRecords.toList()
    }

    fun getRecentCrashes(): List<CrashRecord> =
        crashRecords.filter { it.type == "CRASH" || it.type == "FATAL" }

    fun clearLogs() {
        crashRecords.clear()
        crashLogFile.writeText("")
    }

    fun exportLogs(): String {
        return buildString {
            appendLine("=== NSAI笔记 崩溃日志 ===")
            appendLine("导出时间: ${dateFormat.format(Date())}")
            appendLine("设备型号: ${android.os.Build.MODEL}")
            appendLine("系统版本: Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
            appendLine("App版本: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}")
            appendLine("=" .repeat(50))
            crashRecords.forEach { record ->
                appendLine(record.summary)
                appendLine("  触发上下文: ${record.triggerInfo}")
                if (record.stackTrace.isNotBlank()) {
                    appendLine("  堆栈:")
                    record.stackTrace.lines().forEach { appendLine("    $it") }
                }
                appendLine()
            }
        }
    }

    private fun appendToFile(record: CrashRecord) {
        try {
            crashLogFile.appendText("${record.summary}\n触发: ${record.triggerInfo}\n${record.stackTrace}\n---\n")
            if (crashLogFile.length() > 2 * 1024 * 1024) { // 2MB limit
                crashLogFile.writeText(crashLogFile.readText().takeLast(1024 * 1024))
            }
        } catch (_: Exception) {}
    }

    private fun loadHistory() {
        if (!crashLogFile.exists()) return
        try {
            val content = crashLogFile.readText()
            val entries = content.split("---\n").filter { it.isNotBlank() }
            entries.takeLast(100).forEach { entry ->
                try {
                    val lines = entry.trim().lines()
                    if (lines.size >= 2) {
                        val header = lines[0].removePrefix("[")
                        val timeStr = header.substringBefore("]")
                        val typeAndMsg = header.substringAfter("] ").trim()
                        val level = typeAndMsg.substringBefore(":")
                        val msg = typeAndMsg.substringAfter(":").trim()
                        val trigger = lines[1].removePrefix("触发: ").trim()
                        val stack = lines.drop(2).joinToString("\n").trim()
                        crashRecords.add(CrashRecord(
                            timestamp = parseTime(timeStr),
                            type = level,
                            message = msg,
                            stackTrace = stack,
                            triggerInfo = trigger
                        ))
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun parseTime(timeStr: String): Long {
        return try {
            dateFormat.parse("2026-$timeStr")?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun installExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            log("FATAL", throwable.message ?: "未知崩溃", sw.toString().take(4000))
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
