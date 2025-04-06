package com.example.weatherapp.ui.fragments

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.example.weatherapp.R
import com.example.weatherapp.databinding.FragmentWeatherSettingsBinding
import com.example.weatherapp.ui.WeatherViewModel
import com.example.weatherapp.util.LocationHelper
import com.example.weatherapp.util.receivers.AlarmManagerReceiver
import dagger.hilt.android.AndroidEntryPoint
import il.co.syntax.fullarchitectureretrofithiltkotlin.utils.autoCleared
import java.util.Calendar

@AndroidEntryPoint
class WeatherSettingsFragment : Fragment() {

    companion object {
        private const val ALARM_REQUEST_CODE = 1000
    }

    private var binding: FragmentWeatherSettingsBinding by autoCleared()
    private val viewModel: WeatherViewModel by viewModels()
    private lateinit var setAlarmIntent: PendingIntent

    private val locationPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.location_permission_granted),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.location_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    private val notificationPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.notification_permission_granted),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.notification_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentWeatherSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()


        if (LocationHelper.hasLocationPermission(requireContext())) {
            binding.switchGps.isChecked = true
            binding.switchGps.isEnabled = false
        } else {
            binding.switchGps.isChecked = false
            binding.switchGps.isEnabled = true
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                binding.switchNotifications.isChecked = true
                binding.switchNotifications.isEnabled = false
            } else {
                binding.switchNotifications.isChecked = false
                binding.switchNotifications.isEnabled = true
            }
        } else {

            binding.switchNotifications.isChecked = true
            binding.switchNotifications.isEnabled = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        Glide.with(requireContext())
            .load(R.drawable.ic_home)
            .into(binding.btnHome)

        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.action_weatherSettingsFragment_to_weatherLocalFragment)
        }


        val unit = viewModel.temperatureUnit
        binding.rgTemperature.check(if (unit == "metric") R.id.rbCelsius else R.id.rbFahrenheit)
        binding.rgTemperature.setOnCheckedChangeListener { _, checkedId ->
            val selectedUnit = if (checkedId == R.id.rbCelsius) "metric" else "imperial"
            viewModel.updateTemperatureUnit(selectedUnit)
        }


        binding.switchGps.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {

            } else {

                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                ) {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.turn_on_from_settings),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.switchNotifications.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {

                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(),
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.turn_on_from_settings),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), getString(R.string.notification_not_required), Toast.LENGTH_SHORT).show()
            }
        }


        binding.containerAlarm.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    if (!alarmManager.canScheduleExactAlarms()) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        startActivity(intent)
                        return@setOnClickListener
                    }
                }
                showTimePicker()
            }
        }


        binding.containerCancelAlarm.setOnClickListener {
            if (binding.tvPickTime.text.toString() == getString(R.string.schedule_daily_notification)) {
                Toast.makeText(requireContext(), getString(R.string.no_alarm_scheduled), Toast.LENGTH_SHORT).show()
            } else {
                val workManager = WorkManager.getInstance(requireContext())
                workManager.cancelUniqueWork(getString(R.string.periodicuniquejob))
                binding.tvPickTime.text = getString(R.string.schedule_daily_notification)
                Toast.makeText(requireContext(), getString(R.string.alarm_canceled), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleAlarm(hour: Int, minute: Int) {
        val alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(intent)
            return
        } else {
            val intent = Intent(requireContext(), AlarmManagerReceiver::class.java)
            setAlarmIntent = PendingIntent.getBroadcast(
                requireActivity(),
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, setAlarmIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, setAlarmIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, setAlarmIntent)
            }

            val formattedTime = String.format("%02d:%02d", hour, minute)
            binding.tvPickTime.text = getString(R.string.notification_set_text, formattedTime)
            Toast.makeText(requireContext(), getString(R.string.notification_set_daily, formattedTime), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                if (hourOfDay in 0..23 && minute in 0..59) {
                    handleAlarm(hourOfDay, minute)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.invalid_time_selected), Toast.LENGTH_SHORT).show()
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }
}