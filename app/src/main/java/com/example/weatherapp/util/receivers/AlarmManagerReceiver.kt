package com.example.weatherapp.util.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.weatherapp.R
import com.example.weatherapp.util.worker.WeatherNotificationWorker
import java.util.concurrent.TimeUnit

class AlarmManagerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val dataConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest =
            PeriodicWorkRequestBuilder<WeatherNotificationWorker>(
                24L, TimeUnit.HOURS
            )
                .addTag(context.getString(R.string.periodicuniquejob))
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5L, TimeUnit.MINUTES)
                .setConstraints(dataConstraints)
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            context.getString(R.string.periodicuniquejob),
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )
    }
}