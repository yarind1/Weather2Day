package com.example.weatherapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.databinding.ForecastViewholderBinding
import com.example.weatherapp.models.forecast.ForecastResponse
import com.example.weatherapp.util.WeatherIconProvider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ForecastAdapter(
    private val temperatureUnit: String
) : RecyclerView.Adapter<ForecastAdapter.WeatherViewHolder>() {

    private val differCallback = object : DiffUtil.ItemCallback<ForecastResponse.data>() {
        override fun areItemsTheSame(oldItem: ForecastResponse.data, newItem: ForecastResponse.data): Boolean {
            return oldItem.dtTxt == newItem.dtTxt
        }
        override fun areContentsTheSame(oldItem: ForecastResponse.data, newItem: ForecastResponse.data): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherViewHolder {
        val binding = ForecastViewholderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WeatherViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WeatherViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    override fun getItemCount() = differ.currentList.size

    inner class WeatherViewHolder(private val binding: ForecastViewholderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ForecastResponse.data) {
            val context = binding.root.context


            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = dateFormat.parse(item.dtTxt)
            val calendar = Calendar.getInstance()
            date?.let { calendar.time = it }


            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())  // e.g. "Sat"
            val hourFormat = SimpleDateFormat("h", Locale.getDefault())  // e.g. "3PM"
            val amPmFormat = SimpleDateFormat("a", Locale.getDefault()) // e.g. "AM"/"PM"
            val dayOfWeek = dayFormat.format(calendar.time)
            val hour = hourFormat.format(calendar.time)
            val amPm = amPmFormat.format(calendar.time)


            val amPmString = if (amPm == "AM") {
                context.getString(R.string.am)
            } else {
                context.getString(R.string.pm)
            }


            val displayTemp = item.main.temp


            val unitSymbol = if (temperatureUnit == "imperial") "°F" else "°C"


            val iconCode = item.weather[0].icon
            val iconRes = WeatherIconProvider.getWeatherIcon(iconCode)


            binding.apply {
                tvDay.text = dayOfWeek
                tvHour.text = "$hour $amPmString"

                tvTemperature.text = String.format(Locale.getDefault(), "%.1f%s", displayTemp, unitSymbol)
                imgWeatherIcon.setImageResource(iconRes)
            }
        }
    }
}
