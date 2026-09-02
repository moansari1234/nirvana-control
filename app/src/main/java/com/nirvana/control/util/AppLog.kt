package com.nirvana.control.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object AppLog {
    private val logBuffer = Collections.synchronizedList(mutableListOf<String>())
    private const val MAX_LOGS = 1000
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    init {
        log("I", "AppLog", "=== Nirvana Control Diagnostic Logger Initialized ===")
    }

    fun log(level: String, tag: String, msg: String, tr: Throwable? = null) {
        val time = dateFormat.format(Date())
        val stackTrace = if (tr != null) "\n" + Log.getStackTraceString(tr) else ""
        val line = "[] [/] "

        when (level) {
            "D" -> Log.d(tag, msg, tr)
            "I" -> Log.i(tag, msg, tr)
            "W" -> Log.w(tag, msg, tr)
            "E" -> Log.e(tag, msg, tr)
            else -> Log.i(tag, msg, tr)
        }

        synchronized(logBuffer) {
            if (logBuffer.size >= MAX_LOGS) {
                logBuffer.removeAt(0)
            }
            logBuffer.add(line)
        }
    }

    fun d(tag: String, msg: String) = log("D", tag, msg)
    fun i(tag: String, msg: String) = log("I", tag, msg)
    fun w(tag: String, msg: String) = log("W", tag, msg)
    fun e(tag: String, msg: String, tr: Throwable? = null) = log("E", tag, msg, tr)

    fun getAllLogs(): String = synchronized(logBuffer) {
        if (logBuffer.isEmpty()) "No logs captured yet."
        else logBuffer.joinToString("\n")
    }

    fun clear() = synchronized(logBuffer) {
        logBuffer.clear()
        log("I", "AppLog", "Logs cleared.")
    }
}
