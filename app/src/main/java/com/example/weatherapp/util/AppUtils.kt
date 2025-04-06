package com.example.weatherapp.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.weatherapp.R

class AppUtils {

    companion object{

        private const val PREFS_NAME = "weather_prefs"
        private const val KEY_USE_GPS = "use_gps"

        private const val NOTIFICATION_ID = 123
        private const val CHANNEL_NAME = "WeatherApp"
        private const val CHANNEL_ID = "weather_channel"

        fun createNotificationChannel(context: Context){

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                notificationManager.createNotificationChannel(channel)
            }
        }

        fun cancelNotification(context: Context) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }

        fun notify(
            context: Context,
            title: String,
            msg: String,
            iconRes: Int? = null
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            //TODO Here more details
            val remoteView = RemoteViews(context.packageName, R.layout.notification_weather).apply {
                setTextViewText(R.id.tvTitle_Notify, title)
                setTextViewText(R.id.tvMessage_Notify, msg)
                setImageViewResource(R.id.weather_icon, iconRes ?: R.drawable.ic_sunny)//TODO DEFAULT ICON
            }

            //TODO DEFAULT ICON
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(iconRes ?: R.drawable.ic_sunnycloudy)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(remoteView)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }
    }
}