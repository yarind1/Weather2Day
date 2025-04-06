package com.example.weatherapp.ui.fragments

import androidx.navigation.fragment.findNavController
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import com.example.weatherapp.Animations.Click_button_animation
import com.example.weatherapp.R
import com.example.weatherapp.adapters.CityAutoCompleteAdapter
import com.example.weatherapp.databinding.FragmentCitySearchBinding
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.ui.CitySearchViewModel
import com.example.weatherapp.util.Resource
import com.example.weatherapp.util.WeatherIconProvider
import com.example.weatherapp.util.convertUnixToTime
import dagger.hilt.android.AndroidEntryPoint
import il.co.syntax.fullarchitectureretrofithiltkotlin.utils.autoCleared
import java.util.Locale

@AndroidEntryPoint
class CitySearchFragment : Fragment() {


    private var binding: FragmentCitySearchBinding by autoCleared()
    private val viewModel: CitySearchViewModel by viewModels()
    private lateinit var cityAdapter: CityAutoCompleteAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCitySearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAdapter()
        observeCityList()
        setupSearchListener()
        setupSearchButton()

        binding.btnHome.setOnClickListener {
            Click_button_animation.scaleView(it) {
                findNavController().popBackStack(R.id.weatherLocalFragment, false)
            }
        }

        binding.btnGoToFavorites.setOnClickListener {
            findNavController().navigate(R.id.action_citySearchFragment_to_favoritesFragment)
        }


    }


    private fun initAdapter() {
        cityAdapter = CityAutoCompleteAdapter(requireContext(), emptyList())
        binding.etCityName.setAdapter(cityAdapter)
    }


    private fun observeCityList() {
        viewModel.cityList.observe(viewLifecycleOwner) { cities ->
            cityAdapter.updateCities(cities)
        }
    }

    //  האזנה לעדכונים בשדה החיפוש (לשאילתות בזמן אמת)
    private fun setupSearchListener() {
        binding.etCityName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    viewModel.searchCities(s.toString()) // חיפוש בזמן אמת
                }
            }
        })

        // בחירת עיר מתוך רשימת ההשלמות האוטומטיות
        binding.etCityName.setOnItemClickListener { _, _, position, _ ->
            val selectedCity = cityAdapter.getFullCityItem(position)
            val countryName = Locale("", selectedCity.country).displayCountry
            val formattedCity = "${selectedCity.name}, $countryName"

            binding.etCityName.setText(formattedCity)
            viewModel.selectedCityName.value = selectedCity.name
            viewModel.selectedCountryCode.value = selectedCity.country
        }
    }


    private fun setupSearchButton() {
        binding.btnSearch.setOnClickListener { view ->
            Click_button_animation.scaleView(view) {
                val city = viewModel.selectedCityName.value
                val country = viewModel.selectedCountryCode.value
                if (!city.isNullOrEmpty() && !country.isNullOrEmpty()) {

                    val bundle = bundleOf("cityName" to city, "countryCode" to country)
                    findNavController().navigate(R.id.action_citySearchFragment_to_weatherDetailsFragment, bundle)

                    binding.etCityName.setText("")
                } else {
                    Toast.makeText(requireContext(), getString(R.string.enter_city_name), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}

