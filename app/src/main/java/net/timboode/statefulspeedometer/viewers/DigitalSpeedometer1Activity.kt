package net.timboode.statefulspeedometer.viewers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import net.timboode.statefulspeedometer.MainActivity
import net.timboode.statefulspeedometer.MainActivity.Companion.KEY_CURRENT_GAS_DISTANCE
import net.timboode.statefulspeedometer.MainActivity.Companion.KEY_GAS_WARNING_DISTANCE
import net.timboode.statefulspeedometer.MainActivity.Companion.KEY_TOTAL_DISTANCE
import net.timboode.statefulspeedometer.services.SpeedColorService
import net.timboode.statefulspeedometer.R

class DigitalSpeedometer1Activity : AppCompatActivity() {
    var _locationManager: LocationManager? = null
    var _locationListener: LocationListener? = null

    var _speedColorService: SpeedColorService = SpeedColorService()

    var totalDistanceInMetres: Double = 0.0
        private set

    var gasWarningDistanceInMetres: Double = 0.0
        private set

    var currentGasDistanceInMetres: Double = 0.0
        private set

    var lastLocation: Location? = null
        private set

    var updateCount: Int = 0

    class MyLocationListener : LocationListener {
        val digitalSpeedometer1Activity:DigitalSpeedometer1Activity

        constructor(digitalSpeedometer1Activity: DigitalSpeedometer1Activity) {
            this.digitalSpeedometer1Activity = digitalSpeedometer1Activity
        }

        override fun onLocationChanged(location: Location) {
            digitalSpeedometer1Activity.updateLocation(location)
        }

        // called in some Android version and fails
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewers_digital_meter1)

        this._locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        this._locationListener = MyLocationListener(this)

        findViewById<View>(R.id.car_meter1_root_view).setOnClickListener {
            finish()
        }

        totalDistanceInMetres = PreferenceManager.getDefaultSharedPreferences(this).getFloat(KEY_TOTAL_DISTANCE, 0f).toDouble()

        gasWarningDistanceInMetres = PreferenceManager.getDefaultSharedPreferences(this).getFloat(
            KEY_GAS_WARNING_DISTANCE, 50f).toDouble()
        currentGasDistanceInMetres = PreferenceManager.getDefaultSharedPreferences(this).getFloat(
            KEY_CURRENT_GAS_DISTANCE, 0f).toDouble()

        if (currentGasDistanceInMetres >= gasWarningDistanceInMetres) {
            findViewById<View>(R.id.digital_meter1_gas_icon).visibility = View.VISIBLE
        }
        else
            findViewById<View>(R.id.digital_meter1_gas_icon).visibility = View.INVISIBLE
    }

    override fun onStop() {
        this._locationManager!!.removeUpdates(this._locationListener!!)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.digital_meter1_textview).setText("...")
        updateLocationProvider()
        totalDistanceInMetres = PreferenceManager.getDefaultSharedPreferences(this).getFloat(KEY_TOTAL_DISTANCE, 0f).toDouble()
        gasWarningDistanceInMetres = PreferenceManager.getDefaultSharedPreferences(this).getFloat(KEY_GAS_WARNING_DISTANCE, 50f).toDouble()
        currentGasDistanceInMetres = PreferenceManager.getDefaultSharedPreferences(this).getFloat(KEY_CURRENT_GAS_DISTANCE, 0f).toDouble()
        updateView()

        if (currentGasDistanceInMetres >= gasWarningDistanceInMetres) {
            findViewById<View>(R.id.digital_meter1_gas_icon).visibility = View.VISIBLE
        }
        else
            findViewById<View>(R.id.digital_meter1_gas_icon).visibility = View.INVISIBLE
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationProvider() {
        this._locationManager!!.removeUpdates(this._locationListener!!)

        val locationProviders:List<String> = _locationManager!!.getProviders(true)

        val currentPreferenceLocationProvider:String = PreferenceManager.getDefaultSharedPreferences(this).getString(MainActivity.PREFERENCE_KEY_LOCATION_PROVIDER, "")!!

        var providerAvailable:Boolean = false

        for (p:String in locationProviders) {
            if (p.equals(currentPreferenceLocationProvider)) {
                providerAvailable = true
            }
        }

        if (providerAvailable) {
            this._locationManager!!.requestLocationUpdates(currentPreferenceLocationProvider, 0, 0.0f, this._locationListener!!)
        }
    }

    // Confidence for each unit is on comment on MainActivity
    private fun updateView() {
        val speedUnit:Int = PreferenceManager.getDefaultSharedPreferences(this).getInt(MainActivity.PREFERENCE_KEY_SPEED_UNIT, MainActivity.PREFERENCE_VAL_SPEED_UNIT_DEFAULT )!!

        when(speedUnit) {
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_KM_H -> {
                findViewById<TextView>(R.id.digital_meter1_unit_textview).setText(getText(R.string.unit_km_per_hour))
            }
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_KNOT -> {
                findViewById<TextView>(R.id.digital_meter1_unit_textview).setText(getText(R.string.unit_knot))
            }
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_M_S -> {
                findViewById<TextView>(R.id.digital_meter1_unit_textview).setText(getText(R.string.unit_meter_per_second))
            }
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_MPH -> {
                findViewById<TextView>(R.id.digital_meter1_unit_textview).setText(getText(R.string.unit_mile_per_hour))
            }
        }
    }

    fun setLocation(location: Location) {
        val gasLightWasOff:Boolean = currentGasDistanceInMetres < gasWarningDistanceInMetres

        if (lastLocation != null) {
            totalDistanceInMetres += location.distanceTo(lastLocation!!)
            currentGasDistanceInMetres += location.distanceTo(lastLocation!!)

            if (currentGasDistanceInMetres >= gasWarningDistanceInMetres && gasLightWasOff) {
                findViewById<View>(R.id.digital_meter1_gas_icon).visibility = View.VISIBLE
            }
        }
        lastLocation = location

        if(updateCount++ % 25 == 0) {
            updateCount = 0
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putFloat(KEY_TOTAL_DISTANCE, totalDistanceInMetres.toFloat())
                .apply()
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putFloat(KEY_CURRENT_GAS_DISTANCE, currentGasDistanceInMetres.toFloat())
                .apply()
        }
    }

    private fun formatDistance(distance: Double): String {
        return when {
            distance >= 1000 -> String.format("%.1f", distance)
            distance >= 100 -> String.format("%.2f", distance)
            distance >= 10 -> String.format("%.3f", distance)
            else -> String.format("%.4f", distance)
        }
    }

    // Confidence for each unit is on comment on MainActivity
    private fun updateLocation(location: Location) {
        val speedUnit:Int = PreferenceManager.getDefaultSharedPreferences(this).getInt(MainActivity.PREFERENCE_KEY_SPEED_UNIT, MainActivity.PREFERENCE_VAL_SPEED_UNIT_DEFAULT )!!

        var color:Int = _speedColorService.getSpeedColor(location.speed * 3.6f)
        setLocation(location)

        var speedDigitsView = findViewById<TextView>(R.id.digital_meter1_textview)
        var distanceDigitsView = findViewById<TextView>(R.id.digital_meter1_distance_textview)
        var distanceUnitsView = findViewById<TextView>(R.id.digital_meter1_distance_unit_textview)

        when(speedUnit) {
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_KM_H -> {
                speedDigitsView.setText((location.speed * 3.6).toInt().toString())
                speedDigitsView.setTextColor(color)

                distanceDigitsView.setText(formatDistance(this.totalDistanceInMetres / 1000.0))
                distanceUnitsView.setText(R.string.unit_kilometres)
            }
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_KNOT -> {
                speedDigitsView.setText((location.speed * 3.6 / 1.852).toInt().toString())
                speedDigitsView.setTextColor(color)

                distanceDigitsView.setText(formatDistance(this.totalDistanceInMetres / 1000.0))
                distanceUnitsView.setText(R.string.unit_kilometres)
            }
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_M_S -> {
                speedDigitsView.setText((location.speed).toInt().toString())
                speedDigitsView.setTextColor(color)

                distanceDigitsView.setText(formatDistance(this.totalDistanceInMetres / 1000.0))
                distanceUnitsView.setText(R.string.unit_kilometres)
            }
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_MPH -> {
                speedDigitsView.setText((location.speed * 3.6 / 1.609344).toInt().toString())
                speedDigitsView.setTextColor(color)

                distanceDigitsView.setText(formatDistance(this.totalDistanceInMetres / 1609.344))
                distanceUnitsView.setText(R.string.unit_kilometres)
            }
        }
        updateView()
    }
}