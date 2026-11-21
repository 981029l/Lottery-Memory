package com.caiji.app.data

import android.content.Context
import com.caiji.app.model.DrawRecord
import com.caiji.app.model.LotteryType
import com.caiji.app.model.UserTicket
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LocalStore(context: Context) {

    private val prefs = context.getSharedPreferences("caiji_store", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun loadTickets(): List<UserTicket> {
        val raw = prefs.getString(KEY_TICKETS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<UserTicket>>(raw) }.getOrDefault(emptyList())
    }

    fun saveTickets(tickets: List<UserTicket>) {
        prefs.edit().putString(KEY_TICKETS, json.encodeToString(tickets)).apply()
    }

    fun loadDraws(type: LotteryType): List<DrawRecord> {
        val raw = prefs.getString(keyDraws(type), null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<DrawRecord>>(raw) }.getOrDefault(emptyList())
    }

    fun saveDraws(type: LotteryType, draws: List<DrawRecord>) {
        prefs.edit().putString(keyDraws(type), json.encodeToString(draws)).apply()
    }

    fun isAutoNotifyEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFY_ENABLED, true)

    fun setAutoNotifyEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_ENABLED, enabled).apply()
    }

    fun getLastNotifiedIssue(type: LotteryType): String =
        prefs.getString("last_notified_${type.name}", "").orEmpty()

    fun setLastNotifiedIssue(type: LotteryType, issue: String) {
        prefs.edit().putString("last_notified_${type.name}", issue).apply()
    }

    fun isCnAccelEnabled(): Boolean = prefs.getBoolean(KEY_CN_ACCEL, true)

    fun setCnAccelEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CN_ACCEL, enabled).apply()
    }

    fun loadDownloadMirror(): DownloadMirror {
        val name = prefs.getString(KEY_MIRROR, DownloadMirror.AUTO.name)
        return DownloadMirror.entries.firstOrNull { it.name == name } ?: DownloadMirror.AUTO
    }

    fun saveDownloadMirror(mirror: DownloadMirror) {
        prefs.edit().putString(KEY_MIRROR, mirror.name).apply()
    }

    private fun keyDraws(type: LotteryType) = "draws_${type.name}"

    companion object {
        private const val KEY_TICKETS = "user_tickets_v1"
        private const val KEY_NOTIFY_ENABLED = "auto_notify_enabled"
        private const val KEY_CN_ACCEL = "cn_accel_enabled"
        private const val KEY_MIRROR = "download_mirror"
    }
}
