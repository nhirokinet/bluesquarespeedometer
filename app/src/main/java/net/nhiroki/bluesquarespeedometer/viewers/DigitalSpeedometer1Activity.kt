package net.nhiroki.bluesquarespeedometer.viewers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import net.nhiroki.bluesquarespeedometer.MainActivity
import net.nhiroki.bluesquarespeedometer.R

class DigitalSpeedometer1Activity : AppCompatActivity() {
    var _locationManager: LocationManager? = null
    var _locationListener: LocationListener? = null

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
        enableEdgeToEdge()
        setContentView(R.layout.activity_viewers_digital_meter1)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById<View?>(R.id.main),
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                val systemBars = insets!!.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
                v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            })

        this._locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        this._locationListener = MyLocationListener(this)

        findViewById<View>(R.id.car_meter1_root_view).setOnClickListener {
            finish()
        }
    }

    override fun onStop() {
        this._locationManager!!.removeUpdates(this._locationListener!!)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.digital_meter1_textview).setText("...")
        updateLocationProvider()
        updateUnitText()
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
    private fun updateUnitText() {
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

    // Confidence for each unit is on comment on MainActivity
    private fun updateLocation(location: Location) {
        val speedUnit:Int = PreferenceManager.getDefaultSharedPreferences(this).getInt(MainActivity.PREFERENCE_KEY_SPEED_UNIT, MainActivity.PREFERENCE_VAL_SPEED_UNIT_DEFAULT )!!

        when(speedUnit) {
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_KM_H -> {
                findViewById<TextView>(R.id.digital_meter1_textview).setText((location.speed * 3.6).toInt().toString())
            }
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_KNOT -> {
                findViewById<TextView>(R.id.digital_meter1_textview).setText((location.speed * 3.6 / 1.852).toInt().toString())
            }
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_M_S -> {
                findViewById<TextView>(R.id.digital_meter1_textview).setText((location.speed).toInt().toString())
            }
            MainActivity.PREFERENCE_VAL_SPEED_UNIT_MPH -> {
                findViewById<TextView>(R.id.digital_meter1_textview).setText((location.speed * 3.6 / 1.609344).toInt().toString())
            }
        }
        updateUnitText()
    }
}