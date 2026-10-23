package com.aminul.multibook.data.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: String,
    val level: String,
    val tag: String,
    val message: String
)

object DiagnosticLogger {
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val maxLogs = 200
    private val logBuffer = mutableListOf<LogEntry>()
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    init {
        log("INFO", "DiagnosticLogger", "Diagnostic system initialized")
    }

    @Synchronized
    fun log(level: String, tag: String, message: String) {
        val entry = LogEntry(
            timestamp = timeFormat.format(Date()),
            level = level,
            tag = tag,
            message = message
        )
        logBuffer.add(0, entry)
        if (logBuffer.size > maxLogs) {
            logBuffer.removeAt(logBuffer.size - 1)
        }
        _logsFlow.value = logBuffer.toList()
    }

    fun info(tag: String, message: String) = log("INFO", tag, message)
    fun debug(tag: String, message: String) = log("DEBUG", tag, message)
    fun warn(tag: String, message: String) = log("WARN", tag, message)
    fun error(tag: String, message: String) = log("ERROR", tag, message)

    @Synchronized
    fun clear() {
        logBuffer.clear()
        _logsFlow.value = emptyList()
        log("INFO", "DiagnosticLogger", "Logs cleared by developer")
    }
}
