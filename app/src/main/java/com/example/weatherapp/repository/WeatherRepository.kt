package com.example.weatherapp.repository

import com.example.weatherapp.api.WeatherAPI
import com.example.weatherapp.db.WeatherDatabase
import com.example.weatherapp.models.CityResponse
import com.example.weatherapp.models.FavoriteWeather
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.models.forecast.ForecastResponse
import retrofit2.Response
import javax.inject.Inject

class WeatherRepository @Inject constructor(
    private val api: WeatherAPI,
    private val db : WeatherDatabase
) {

    suspend fun getCoordinates(city: String, country: String? = null): Response<List<CityResponse>> {
        val query = if (country.isNullOrEmpty()) city else "$city,$country"
        return api.getCoordinates(query)
    }

    suspend fun getCitiesFromAPI(query: String): Response<List<CityResponse>> {
        return api.searchCities(query)
    }


    suspend fun insertFavoriteWeather(favoriteWeather: FavoriteWeather) {
        db.getFavoriteDao().insertFavorite(favoriteWeather)
    }

    fun getFavoriteWeather() = db.getFavoriteDao().getAllFavorites()

    suspend fun deleteFavoriteWeather(favoriteWeather: FavoriteWeather) {
        db.getFavoriteDao().deleteFavorite(favoriteWeather)
    }

    suspend fun updateFavoriteWeather(favorites: List<FavoriteWeather>) {
        favorites.forEach { favorite ->
            db.getFavoriteDao().insertFavorite(favorite)
        }
    }

    suspend fun updateUserNote(cityName: String, country: String, note: String) {
        db.getFavoriteDao().updateUserNote(cityName, country, note)
    }


    suspend fun getWeatherData(lat: Double, lon: Double, unit: String, lang: String) =
        api.getWeatherData(lat, lon, unit, lang)


    suspend fun getForecastData(lat: Double, lon: Double, unit: String, lang: String): Response<ForecastResponse> {

        return api.getForecastData(lat, lon, lang, unit)
    }

}
