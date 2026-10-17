package com.cappuccino.kiosk

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant

object KioskDiagnostics {
    private const val tag = "CappuccinoKiosk"
    private const val maxLogBytes = 256 * 1024L
    private val lock = Any()

    fun record(context: Context, event: String, details: String = "") {
        val entry = "${Instant.now()} event=$event ${details.replace('\n', ' ').replace('\r', ' ')}".trim()
        Log.i(tag, entry)

        try {
            synchronized(lock) {
                val logFile = File(context.filesDir, "kiosk-diagnostics.log")
                val previousLog = File(context.filesDir, "kiosk-diagnostics.previous.log")
                val bytes = "$entry\n".toByteArray(StandardCharsets.UTF_8)
                if (logFile.exists() && logFile.length() + bytes.size > maxLogBytes) {
                    previousLog.delete()
                    if (!logFile.renameTo(previousLog)) logFile.delete()
                }
                FileOutputStream(logFile, true).use { it.write(bytes) }
            }
        } catch (error: Exception) {
            Log.e(tag, "diagnostics_file_write_failed", error)
        }
    }

    fun read(context: Context): String = synchronized(lock) {
        try {
            listOf(
                File(context.filesDir, "kiosk-diagnostics.previous.log"),
                File(context.filesDir, "kiosk-diagnostics.log"),
            ).filter { it.exists() }.joinToString("\n") { it.readText() }
        } catch (error: Exception) {
            Log.e(tag, "diagnostics_file_read_failed", error)
            ""
        }
    }
}