package com.example.weatherapp.util.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.app.ActivityCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.weatherapp.R
import com.example.weatherapp.repository.WeatherRepository
import com.example.weatherapp.util.AppUtils
import com.example.weatherapp.util.WeatherIconProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.Tasks
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Locale

@HiltWorker
class WeatherNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val repository: WeatherRepository,
    private val fusedLocationProviderClient: FusedLocationProviderClient
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        try {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val locationResult = fusedLocationProviderClient.lastLocation
                val taskResult = Tasks.await(locationResult)

                if (taskResult != null) {
                    val lat = taskResult.latitude
                    val lon = taskResult.longitude
                    val cityName = getCityName(lat, lon)
                    val lang = if (Locale.getDefault().language == "iw") "he" else Locale.getDefault().language

                    val response = repository.getForecastData(lat, lon, "metric", lang)

                    if (response.isSuccessful && response.body() != null) {
                        val weatherData = response.body()
                        val forecast = weatherData?.list?.firstOrNull()

                        forecast?.let {
                            val highTemp = it.main.tempMax
                            val lowTemp = it.main.tempMin
                            val description = it.weather.firstOrNull()?.description ?:
                            applicationContext.getString(R.string.no_data)
                            val iconCode = it.weather[0].icon
                            val iconResult = WeatherIconProvider.getWeatherIcon(iconCode)

                            AppUtils.notify(
                                applicationContext,
                                applicationContext.getString(R.string.tomorrow_s_forecast),
                                applicationContext.getString(
                                    R.string.weather_notification_text,
                                    highTemp,
                                    lowTemp,
                                    description,
                                    cityName
                                ),
                                iconResult
                            )
                        }
                    }
                }
                return Result.success()
            } else {
                AppUtils.notify(
                    applicationContext,
                    applicationContext.getString(R.string.notification_error_title),
                    applicationContext.getString(R.string.notification_error_message)
                )
                return Result.failure()
            }
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private fun getCityName(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(applicationContext, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].locality ?: applicationContext.getString(R.string.unknown_city)
            } else {
                applicationContext.getString(R.string.unknown_city)
            }
        } catch (e: Exception) {
            applicationContext.getString(R.string.unknown_city)
        }
    }
}