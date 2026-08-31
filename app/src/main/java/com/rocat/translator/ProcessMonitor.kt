package com.rocat.translator

import android.os.SystemClock

data class MonitoredProcess(
    val id: String,
    val category: String,
    val detail: String,
    val state: String,
    val startedAt: Long,
    val updatedAt: Long,
    val active: Boolean,
)

/** Thread-safe, in-memory process registry consumed by the debug screen. */
object ProcessMonitor {
    private const val MAX_ENTRIES = 50
    private val entries = LinkedHashMap<String, MonitoredProcess>()

    fun start(id: String, category: String, detail: String) = update(id, category, detail, "RUNNING", true)

    fun update(id: String, category: String, detail: String, state: String, active: Boolean = true) {
        synchronized(entries) {
            val now = SystemClock.elapsedRealtime()
            val startedAt = entries[id]?.startedAt ?: now
            entries[id] = MonitoredProcess(id, category, detail, state, startedAt, now, active)
            trimLocked()
        }
    }

    fun finish(id: String, detail: String, state: String = "COMPLETED") {
        synchronized(entries) {
            val current = entries[id] ?: return
            entries[id] = current.copy(detail = detail, state = state, updatedAt = SystemClock.elapsedRealtime(), active = false)
            trimLocked()
        }
    }

    fun cancelActive(reason: String) {
        synchronized(entries) {
            val now = SystemClock.elapsedRealtime()
            entries.replaceAll { _, item ->
                if (item.active) item.copy(detail = reason, state = "CANCELLED", updatedAt = now, active = false) else item
            }
        }
    }

    fun snapshot(): List<MonitoredProcess> = synchronized(entries) {
        entries.values.sortedWith(compareByDescending<MonitoredProcess> { it.active }.thenByDescending { it.updatedAt })
    }

    private fun trimLocked() {
        while (entries.size > MAX_ENTRIES) {
            val removable = entries.entries.firstOrNull { !it.value.active } ?: entries.entries.first()
            entries.remove(removable.key)
        }
    }
}
