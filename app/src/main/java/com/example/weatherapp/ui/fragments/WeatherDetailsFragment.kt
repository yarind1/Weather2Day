package com.example.weatherapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.weatherapp.Animations.Click_button_animation
import com.example.weatherapp.R
import com.example.weatherapp.adapters.ForecastAdapter
import com.example.weatherapp.databinding.FragmentWeatherDetailsBinding
import com.example.weatherapp.models.FavoriteWeather
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.models.forecast.ForecastResponse
import com.example.weatherapp.ui.WeatherViewModel
import com.example.weatherapp.util.Resource
import com.example.weatherapp.util.WeatherIconProvider
import com.example.weatherapp.util.convertUnixToTime
import dagger.hilt.android.AndroidEntryPoint
import il.co.syntax.fullarchitectureretrofithiltkotlin.utils.autoCleared
import java.util.Locale

@AndroidEntryPoint
class WeatherDetailsFragment : Fragment() {

    private var binding: FragmentWeatherDetailsBinding by autoCleared()
    private val weatherViewModel: WeatherViewModel by viewModels()
    private lateinit var forecastAdapter: ForecastAdapter

    private var currentFavorites: List<FavoriteWeather> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWeatherDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cityName = arguments?.getString("cityName")
        val countryCode = arguments?.getString("countryCode")
        val fromFavorites = arguments?.getBoolean("fromFavorites", false) ?: false

        weatherViewModel.favoriteWeatherList.observe(viewLifecycleOwner) { favorites ->
            currentFavorites = favorites
        }

        if (!cityName.isNullOrEmpty() && !countryCode.isNullOrEmpty()) {
            binding.tvCity.text = cityName
            binding.tvCountry.text = Locale("", countryCode).displayCountry
            weatherViewModel.getWeatherByCity(cityName, countryCode)
        } else {
            Toast.makeText(requireContext(), getString(R.string.city_not_found), Toast.LENGTH_SHORT).show()
        }

        binding.ivHeartIcon.visibility = if (fromFavorites) View.GONE else View.VISIBLE
        if (!fromFavorites) {
            binding.ivHeartIcon.setOnClickListener { view ->
                Click_button_animation.scaleView(view) {
                    weatherViewModel.cityWeatherData.value?.let { resource ->
                        if (resource is Resource.Success && resource.data != null) {
                            val weather = resource.data
                            if (currentFavorites.any {
                                    it.cityName.equals(weather.name, ignoreCase = true) &&
                                            it.country.equals(weather.sys.country, ignoreCase = true)
                                }) {
                                Toast.makeText(context, getString(R.string.already_in_favorites), Toast.LENGTH_SHORT).show()
                            } else {
                                weatherViewModel.saveWeatherToFavorites(weather)
                                Toast.makeText(context, getString(R.string.added_to_favorites), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        setupHourlyForecastRecyclerView()
        observeWeatherData()
        observeForecastData()

        binding.btnBack.setOnClickListener { btn ->
            Click_button_animation.scaleView(btn) {
                when (findNavController().previousBackStackEntry?.destination?.id) {
                    R.id.favoritesFragment -> findNavController().navigate(R.id.action_weatherDetailsFragment_to_favoritesFragment)
                    R.id.citySearchFragment -> findNavController().navigate(R.id.action_weatherDetailsFragment_to_citySearchFragment)
                    else -> findNavController().popBackStack()
                }
            }
        }
    }

    private fun setupHourlyForecastRecyclerView() {
        val unit = weatherViewModel.temperatureUnit
        forecastAdapter = ForecastAdapter(unit)
        binding.rvForecast.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = forecastAdapter
        }
    }

    private fun observeWeatherData() {
        weatherViewModel.cityWeatherData.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    resource.data?.let { weather ->
                        // Request city-specific forecast data
                        weatherViewModel.getForecast(weather.coord.lat, weather.coord.lon, weatherViewModel.temperatureUnit, isCityForecast = true)
                        updateUIWithWeather(weather)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), getString(R.string.error_fetch_weather), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeForecastData() {
        weatherViewModel.cityForecastData.observe(viewLifecycleOwner) { resource -> // Observe cityForecastData
            when (resource) {
                is Resource.Success -> {
                    resource.data?.let { forecastResponse ->
                        forecastAdapter.differ.submitList(forecastResponse.list)
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), getString(R.string.error_fetch_forecast), Toast.LENGTH_SHORT).show()
                }
                else -> { /* no-op */ }
            }
        }
    }

    private fun updateUIWithWeather(weather: WeatherResponse) {
        val tempUnit = if (weatherViewModel.temperatureUnit == "metric") "°C" else "°F"
        val windUnit = if (weatherViewModel.temperatureUnit == "metric") "m/s" else "mph"

        val iconCode = weather.weather.firstOrNull()?.icon

        with(binding) {
            tvTemperature.text = getString(R.string.weather_temp, weather.main.temp, tempUnit)
            tvFeelsLike.text = getString(R.string.weather_feels_like, weather.main.feels_like, tempUnit)
            tvHumidity.text = getString(R.string.weather_humidity, weather.main.humidity)
            tvWindSpeed.text = getString(R.string.weather_wind_speed, weather.wind.speed, windUnit)
            tvSunrise.text = getString(R.string.weather_sunrise, convertUnixToTime(weather.sys.sunrise, weather.timezone))
            tvSunset.text = getString(R.string.weather_sunset, convertUnixToTime(weather.sys.sunset, weather.timezone))
            tvWeatherDescription.text = weather.weather.firstOrNull()?.description ?: "--"

            Glide.with(requireContext())
                .load(WeatherIconProvider.getWeatherIcon(iconCode))
                .into(ivWeatherIcon)

            tvMinMax.text = getString(R.string.weather_max_temp, weather.main.temp_max, tempUnit) +
                    " • " + getString(R.string.weather_min_temp, weather.main.temp_min, tempUnit)
            tvPrecipitation.text = getString(R.string.weather_precipitation, weather.rain?.oneHour ?: 0.0)
            tvVisibility.text = getString(R.string.weather_visibility, (weather.visibility ?: 0) / 1000.0)
        }
    }
}