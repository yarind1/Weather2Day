package com.example.weatherapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.R
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.models.CityResponse
import com.example.weatherapp.models.FavoriteWeather
import com.example.weatherapp.repository.WeatherRepository
import com.example.weatherapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import android.util.Log
import java.util.Locale
import android.widget.Toast
import javax.inject.Inject

@HiltViewModel
class CitySearchViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val app: Application
) : AndroidViewModel(app) {

    val weatherData = MutableLiveData<Resource<WeatherResponse>>()
    val cityList = MutableLiveData<List<CityResponse>>()
    val isRefreshing = MutableLiveData<Boolean>()
    val selectedCityName = MutableLiveData<String?>()
    val selectedCountryCode = MutableLiveData<String?>()


    fun searchCities(query: String) {
        if (query.length < 2) return

        viewModelScope.launch {
            try {
                val response = repository.getCitiesFromAPI(query)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    cityList.postValue(response.body()) // עדכון רשימת הערים בתוצאות
                } else {
                    cityList.postValue(emptyList()) // אם אין תוצאות, מחזירים רשימה ריקה
                }
            } catch (e: Exception) {
                cityList.postValue(emptyList())
                Log.e("CitySearchViewModel", "Error searching cities", e)
            }
        }
    }

    val favoriteWeatherList = repository.getFavoriteWeather()
    fun removeWeatherFromFavorites(favorite: FavoriteWeather) {
        viewModelScope.launch {
            repository.deleteFavoriteWeather(favorite)
        }
    }

    fun updateFavoriteNote(city: String, country: String, note: String) {
        viewModelScope.launch {
            repository.updateUserNote(city, country, note)
        }
    }

    fun refreshFavoriteWeather(callback: (Boolean) -> Unit) {
        isRefreshing.postValue(true)
        val lang = if (Locale.getDefault().language == "iw") "he" else Locale.getDefault().language

        viewModelScope.launch {
            try {
                val updatedFavorites = favoriteWeatherList.value?.map { favorite ->
                    val response = repository.getWeatherData(favorite.lat, favorite.lon, "metric",lang)

                    if (response.isSuccessful) {
                        response.body()?.let { weather ->
                            favorite.copy(
                                temperature = weather.main.temp,
                                description = weather.weather[0].description,
                                minTemp = weather.main.temp_min,
                                maxTemp = weather.main.temp_max,
                                feelsLike = weather.main.feels_like,
                                windSpeed = weather.wind.speed,
                                humidity = weather.main.humidity,
                                sunrise = weather.sys.sunrise,
                                sunset = weather.sys.sunset,
                                timezone = weather.timezone,
                                iconCode = weather.weather[0].icon // ✅ הוספת עדכון לאייקון
                            )
                        } ?: favorite
                    } else {
                        favorite
                    }
                } ?: emptyList()

                repository.updateFavoriteWeather(updatedFavorites)
                isRefreshing.postValue(false)
                callback(true)
            } catch (e: Exception) {
                isRefreshing.postValue(false)
                callback(false)
            }
        }
    }
}

