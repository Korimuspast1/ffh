package com.ffh.vpn.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** In-memory ring buffer with the most recent core / app messages. */
object LogStore {

    private const val MAX_LINES = 600
    private val lock = Any()
    private val lines = ArrayDeque<String>()
    private val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val _flow = MutableStateFlow<List<String>>(emptyList())

    val flow: StateFlow<List<String>> = _flow.asStateFlow()

    fun append(tag: String, message: String) {
        val line = "${formatter.format(Date())} $tag $message"
        synchronized(lock) {
            lines.addLast(line)
            while (lines.size > MAX_LINES) lines.removeFirst()
            _flow.value = lines.toList()
        }
    }

    fun clear() {
        synchronized(lock) {
            lines.clear()
            _flow.value = emptyList()
        }
    }

    fun snapshot(): String = synchronized(lock) {
        if (lines.isEmpty()) "" else lines.joinToString("\n")
    }
}
