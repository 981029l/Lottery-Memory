package com.caiji.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.caiji.app.worker.DrawCheckWorker
import java.util.concurrent.TimeUnit

class CaijiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DrawCheckWorker.createChannel(this)
        scheduleBackgroundCheck()
    }

    private fun scheduleBackgroundCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<DrawCheckWorker>(2, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "caiji_auto_draw_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
