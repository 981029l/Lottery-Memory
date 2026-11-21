package com.caiji.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.caiji.app.MainActivity
import com.caiji.app.data.LocalStore
import com.caiji.app.data.OfficialLotteryApi
import com.caiji.app.model.LotteryRules
import com.caiji.app.model.LotteryType

class DrawCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = LocalStore(applicationContext)
        if (!store.isAutoNotifyEnabled()) return Result.success()

        val api = OfficialLotteryApi()
        val tickets = store.loadTickets()

        LotteryType.entries.forEach { type ->
            val fetched = api.fetchDraws(type, pageSize = 10).getOrNull() ?: return@forEach
            if (fetched.isEmpty()) return@forEach
            store.saveDraws(type, fetched)

            val latest = fetched.first()
            val lastNotified = store.getLastNotifiedIssue(type)
            if (latest.issue == lastNotified) return@forEach

            val myCurrentTickets = tickets.filter { it.type == type && it.issue == latest.issue }
            if (myCurrentTickets.isNotEmpty()) {
                val results = myCurrentTickets.map { LotteryRules.checkTicket(it, fetched) }
                val wonCount = results.count { it.isWon }
                val fixedTotal = results.sumOf { it.fixedAmount }
                val title = "${type.displayName} 第 ${latest.issue} 期已开奖"
                val body = if (wonCount > 0) {
                    val amtText = if (fixedTotal > 0) "，固定奖合计 ¥$fixedTotal" else ""
                    "恭喜！你记录的 ${myCurrentTickets.size} 注中有 $wonCount 注中奖$amtText"
                } else {
                    "本期开奖号码已更新，你记录的 ${myCurrentTickets.size} 注未中奖"
                }
                sendNotification(type.ordinal + 1001, title, body)
                store.setLastNotifiedIssue(type, latest.issue)
            }
        }
        return Result.success()
    }

    private fun sendNotification(id: Int, title: String, content: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = "caiji_draw_channel"

        fun createChannel(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "开奖与中奖提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "双色球与大乐透开奖后自动核对个人记录并推送提醒"
            }
            manager.createNotificationChannel(channel)
        }
    }
}
