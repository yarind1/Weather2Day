package com.example.weatherapp.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import com.example.weatherapp.models.FavoriteWeather
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.models.forecast.ForecastResponse
import com.example.weatherapp.repository.WeatherRepository
import com.example.weatherapp.util.Resource
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    application: Application
) : AndroidViewModel(application) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    val weatherData: MutableLiveData<Resource<WeatherResponse>> = MutableLiveData()
    val cityWeatherData: MutableLiveData<Resource<WeatherResponse>> = MutableLiveData()
    val forecastData: MutableLiveData<Resource<ForecastResponse>> = MutableLiveData()
    val cityForecastData: MutableLiveData<Resource<ForecastResponse>> = MutableLiveData()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private var lastKnownLocation: Pair<Double, Double>? = null

    private val prefs = application.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
    private val KEY_UNIT = "temperature_unit"

    val favoriteWeatherList: LiveData<List<FavoriteWeather>> = repository.getFavoriteWeather()

    var temperatureUnit: String
        get() = prefs.getString(KEY_UNIT, "metric") ?: "metric"
        set(value) {
            prefs.edit().putString(KEY_UNIT, value).apply()
        }

    init {
        viewModelScope.launch {
            requestLocation(application.applicationContext)
        }
    }

    fun requestLocation(context: Context) {
        lastKnownLocation?.let { (lat, lon) ->
            getWeather(lat, lon, temperatureUnit)
            getForecast(lat, lon, temperatureUnit, isCityForecast = false)
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            weatherData.postValue(Resource.Error("Location permission not granted"))
            forecastData.postValue(Resource.Error("Location permission not granted"))
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let {
                    lastKnownLocation = it.latitude to it.longitude
                    getWeather(it.latitude, it.longitude, temperatureUnit)
                    getForecast(it.latitude, it.longitude, temperatureUnit, isCityForecast = false)
                } ?: run {
                    weatherData.postValue(Resource.Error("Location unavailable"))
                    forecastData.postValue(Resource.Error("Location unavailable"))
                }
            }
            .addOnFailureListener { exception ->
                weatherData.postValue(Resource.Error("Failed to get location: ${exception.message}"))
                forecastData.postValue(Resource.Error("Failed to get location: ${exception.message}"))
            }
    }

    private fun getWeather(lat: Double, lon: Double, unit: String = "metric") {
        weatherData.postValue(Resource.Loading())
        val lang = if (Locale.getDefault().language == "iw") "he" else Locale.getDefault().language
        viewModelScope.launch {
            try {
                val response = repository.getWeatherData(lat, lon, unit, lang)
                if (response.isSuccessful) {
                    response.body()?.let {
                        weatherData.postValue(Resource.Success(it))
                    } ?: run {
                        weatherData.postValue(Resource.Error("No data found"))
                    }
                } else {
                    weatherData.postValue(Resource.Error(response.message()))
                }
            } catch (e: Exception) {
                weatherData.postValue(Resource.Error("Error fetching weather"))
            }
        }
    }

    fun getForecast(lat: Double, lon: Double, unit: String = "metric", isCityForecast: Boolean = false) {
        val targetLiveData = if (isCityForecast) cityForecastData else forecastData
        targetLiveData.postValue(Resource.Loading())
        val lang = if (Locale.getDefault().language == "iw") "he" else Locale.getDefault().language
        viewModelScope.launch {
            try {
                val response = repository.getForecastData(lat, lon, unit, lang)
                if (response.isSuccessful) {
                    response.body()?.let {
                        targetLiveData.postValue(Resource.Success(it))
                    } ?: run {
                        targetLiveData.postValue(Resource.Error("No forecast data found"))
                    }
                } else {
                    targetLiveData.postValue(Resource.Error(response.message()))
                }
            } catch (e: Exception) {
                targetLiveData.postValue(Resource.Error("Error fetching forecast"))
            } finally {
                if (!isCityForecast) _isLoading.value = false
            }
        }
    }

    fun getWeatherByCity(city: String, country: String) {
        cityWeatherData.postValue(Resource.Loading())
        val lang = if (Locale.getDefault().language == "iw") "he" else Locale.getDefault().language
        viewModelScope.launch {
            try {
                val response = repository.getCoordinates(city, country)
                if (response.isSuccessful) {
                    val firstCity = response.body()?.firstOrNull()
                    firstCity?.let {
                        val weatherResponse = repository.getWeatherData(it.lat, it.lon, temperatureUnit, lang)
                        if (weatherResponse.isSuccessful) {
                            weatherResponse.body()?.let {
                                cityWeatherData.postValue(Resource.Success(it))
                            } ?: cityWeatherData.postValue(Resource.Error("No data found"))
                        } else {
                            cityWeatherData.postValue(Resource.Error(weatherResponse.message()))
                        }
                    } ?: cityWeatherData.postValue(Resource.Error("City not found"))
                } else {
                    cityWeatherData.postValue(Resource.Error("Failed to fetch city coordinates"))
                }
            } catch (e: Exception) {
                cityWeatherData.postValue(Resource.Error("Error fetching weather for city"))
            }
        }
    }

    fun saveWeatherToFavorites(weather: WeatherResponse) {
        viewModelScope.launch {
            try {
                val favorite = FavoriteWeather(
                    cityName = weather.name,
                    country = weather.sys.country,
                    lat = weather.coord.lat,
                    lon = weather.coord.lon,
                    temperature = weather.main.temp,
                    description = weather.weather.firstOrNull()?.description ?: "",
                    minTemp = weather.main.temp_min,
                    maxTemp = weather.main.temp_max,
                    feelsLike = weather.main.feels_like,
                    windSpeed = weather.wind.speed,
                    humidity = weather.main.humidity,
                    timezone = weather.timezone,
                    sunrise = weather.sys.sunrise,
                    sunset = weather.sys.sunset,
                    iconCode = weather.weather.firstOrNull()?.icon ?: ""
                )
                repository.insertFavoriteWeather(favorite)
            } catch (e: Exception) {
               //
            }
        }
    }

    fun updateTemperatureUnit(newUnit: String) {
        temperatureUnit = newUnit
        lastKnownLocation?.let { (lat, lon) ->
            getWeather(lat, lon, newUnit)
            getForecast(lat, lon, newUnit, isCityForecast = false)
        }
    }
}