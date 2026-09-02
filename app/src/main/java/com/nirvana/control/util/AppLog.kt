package com.nirvana.control.util

import android.util.Log

object AppLog {
    // Strictly disable all logging by default for 100% offline privacy
    var isDebugLoggingEnabled: Boolean = false

    fun d(tag: String, msg: String) {
        if (isDebugLoggingEnabled) Log.d(tag, msg)
    }

    fun i(tag: String, msg: String) {
        if (isDebugLoggingEnabled) Log.i(tag, msg)
    }

    fun w(tag: String, msg: String) {
        if (isDebugLoggingEnabled) Log.w(tag, msg)
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        if (isDebugLoggingEnabled) {
            if (tr != null) Log.e(tag, msg, tr) else Log.e(tag, msg)
        }
    }
}
