package com.example.weatherapp.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.weatherapp.Animations.Click_button_animation
import com.example.weatherapp.R
import com.example.weatherapp.databinding.ItemFavoriteWeatherBinding
import com.example.weatherapp.models.FavoriteWeather
import com.example.weatherapp.ui.CitySearchViewModel
import com.example.weatherapp.util.WeatherIconProvider
import com.example.weatherapp.util.convertUnixToTime
import java.util.Locale

class FavoriteWeatherAdapter(
    private val viewModel: CitySearchViewModel,
    private val temperatureUnit: String,
    private val onDeleteClick: (FavoriteWeather) -> Unit
) : RecyclerView.Adapter<FavoriteWeatherAdapter.FavoriteViewHolder>() {

    private var favorites: List<FavoriteWeather> = emptyList()


    private fun celsiusToFahrenheit(c: Double): Double {
        return c * 9.0 / 5.0 + 32.0
    }


    private fun mpsToMph(mps: Double): Double {
        return mps * 2.23694
    }

    inner class FavoriteViewHolder(private val binding: ItemFavoriteWeatherBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(favorite: FavoriteWeather) {
            val context = binding.root.context
            val countryName = Locale("", favorite.country).displayCountry


            val unitSymbol = if (temperatureUnit == "imperial") "°F" else "°C"
            val windSymbol = if (temperatureUnit == "imperial") "mph" else "m/s"


            val rawTemp = favorite.temperature ?: 0.0
            val rawMin = favorite.minTemp ?: 0.0
            val rawMax = favorite.maxTemp ?: 0.0
            val rawFeelsLike = favorite.feelsLike ?: 0.0
            val rawHumidity = (favorite.humidity ?: 0.0).toInt()
            val rawWind = favorite.windSpeed ?: 0.0


            val displayedTemp = if (temperatureUnit == "imperial") celsiusToFahrenheit(rawTemp) else rawTemp
            val displayedMin = if (temperatureUnit == "imperial") celsiusToFahrenheit(rawMin) else rawMin
            val displayedMax = if (temperatureUnit == "imperial") celsiusToFahrenheit(rawMax) else rawMax
            val displayedFeelsLike = if (temperatureUnit == "imperial") celsiusToFahrenheit(rawFeelsLike) else rawFeelsLike
            val displayedWind = if (temperatureUnit == "imperial") mpsToMph(rawWind) else rawWind


            val description = favorite.description ?: "--"
            val sunriseTime = convertUnixToTime(favorite.sunrise, favorite.timezone)
            val sunsetTime = convertUnixToTime(favorite.sunset, favorite.timezone)


            val iconRes = WeatherIconProvider.getWeatherIcon(favorite.iconCode)
            val (startColor, endColor) = WeatherIconProvider.getWeatherCardGradient(favorite.iconCode)

            with(binding) {
                tvCityAndCountry.text = "${favorite.cityName}, $countryName"


                tvTemperature.text = context.getString(R.string.label_temp, displayedTemp, unitSymbol)
                tvDescription.text = context.getString(R.string.label_weather_description, description)
                tvMinTemperature.text = context.getString(R.string.label_temp_min, displayedMin, unitSymbol)
                tvMaxTemperature.text = context.getString(R.string.label_temp_max, displayedMax, unitSymbol)
                tvFeelsLike.text = context.getString(R.string.label_feels_like, displayedFeelsLike, unitSymbol)
                tvHumidity.text = context.getString(R.string.label_humidity, rawHumidity)
                tvWindSpeed.text = context.getString(R.string.label_wind, displayedWind, windSymbol)
                tvSunrise.text = context.getString(R.string.label_sunrise, sunriseTime)
                tvSunset.text = context.getString(R.string.label_sunset, sunsetTime)




                Glide.with(itemView.context)
                    .load(iconRes)
                    .into(ivWeatherIcon)



                val gradientDrawable = android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.BL_TR,
                    intArrayOf(
                        context.getColor(startColor),
                        context.getColor(endColor)
                    )
                )
                gradientDrawable.cornerRadius = 25f
                XmlWeatherListCard.background = gradientDrawable


                etUserNote.setText(favorite.userNote ?: "")
                etUserNote.isEnabled = false

                btnEditNote.setOnClickListener { view ->
                    Click_button_animation.scaleView(view) {
                        toggleEditMode()
                    }
                }

                btnSaveNote.setOnClickListener { view ->
                    Click_button_animation.scaleView(view) {
                        val newNote = etUserNote.text.toString()
                        viewModel.updateFavoriteNote(favorite.cityName, favorite.country, newNote)
                        etUserNote.isEnabled = false
                        btnEditNote.setColorFilter(context.getColor(R.color.black))
                        Toast.makeText(context, context.getString(R.string.note_saved), Toast.LENGTH_SHORT).show()
                    }
                }

                ivDeleteFavorite.setOnClickListener { view ->
                    Click_button_animation.scaleView(view) {
                        onDeleteClick(favorite)
                    }
                }

                // Navigation to details when btnDetails is clicked.
                var isNavigating = false
                btnDetails.setOnClickListener { view ->
                    if (!isNavigating) {
                        isNavigating = true
                        Click_button_animation.scaleView(view) {

                            val bundle = Bundle().apply {
                                putString("cityName", favorite.cityName)
                                putString("countryCode", favorite.country)
                                putBoolean("fromFavorites", true)
                            }
                            itemView.findNavController().navigate(
                                R.id.action_favoritesFragment_to_weatherDetailsFragment,
                                bundle
                            )
                            isNavigating = false
                        }
                    }
                }
            }
        }

        private fun toggleEditMode() {
            val context = binding.root.context
            val isEditable = binding.etUserNote.isEnabled
            binding.etUserNote.isEnabled = !isEditable

            val newColor = if (!isEditable) R.color.bright_blue else R.color.black
            binding.btnEditNote.setColorFilter(context.getColor(newColor))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemFavoriteWeatherBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(favorites[position])
    }

    override fun getItemCount() = favorites.size

    fun submitList(newList: List<FavoriteWeather>) {
        favorites = newList
        notifyDataSetChanged()
    }


    fun sortFavoritesByTemperature(ascending: Boolean) {
        favorites = if (ascending) {
            favorites.sortedBy { it.temperature ?: Double.MIN_VALUE }
        } else {
            favorites.sortedByDescending { it.temperature ?: Double.MIN_VALUE }
        }
        notifyDataSetChanged()
    }

    fun sortFavoritesByCityName(ascending: Boolean) {
        favorites = if (ascending) {
            favorites.sortedBy { it.cityName.lowercase() }
        } else {
            favorites.sortedByDescending { it.cityName.lowercase() }
        }
        notifyDataSetChanged()
    }
}