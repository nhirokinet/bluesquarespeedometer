package net.nhiroki.bluesquarespeedometer

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.altitude.AltitudeConverter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.os.ConfigurationCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import net.nhiroki.bluesquarespeedometer.viewers.DigitalSpeedometer1Activity
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.time.TimeSource


class MainActivity : AppCompatActivity() {
    companion object {
        const val PREFERENCE_KEY_LOCATION_PROVIDER:String = "preference_location_provider"

        const val PREFERENCE_KEY_SPEED_UNIT:String = "preference_speed_unit"
        const val PREFERENCE_VAL_SPEED_UNIT_DEFAULT:Int = 0
        const val PREFERENCE_VAL_SPEED_UNIT_KM_H:Int = 0
        const val PREFERENCE_VAL_SPEED_UNIT_KNOT:Int = 1
        const val PREFERENCE_VAL_SPEED_UNIT_M_S:Int = 2
        const val PREFERENCE_VAL_SPEED_UNIT_MPH:Int = 3

        const val PREFERENCE_KEY_ALTITUDE_UNIT:String = "preference_altitude_unit"
        const val PREFERENCE_VAL_ALTITUDE_DEFAULT:Int = 0
        const val PREFERENCE_VAL_ALTITUDE_METERS:Int = 0
        const val PREFERENCE_VAL_ALTITUDE_FEET:Int = 1

        const val PREFERENCE_KEY_AIR_PRESSURE_UNIT:String = "preference_air_pressure_unit"
        const val PREFERENCE_VAL_AIR_PRESSURE_DEFAULT:Int = 0
        const val PREFERENCE_VAL_AIR_PRESSURE_HPA:Int = 0
        const val PREFERENCE_VAL_AIR_PRESSURE_INHG:Int = 1
    }

    class MyLocationListener : LocationListener {
        val mainActivity:MainActivity

        constructor(mainActivity: MainActivity) {
            this.mainActivity = mainActivity
        }

        override fun onLocationChanged(location: Location) {
            mainActivity.updateLocation(location)
        }

        // called in some Android version and fails
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }
    }

    var _locationManager:LocationManager? = null
    var _locationListener:LocationListener? = null
    var _displayedHeightM:Double = Double.NaN

    class MySensorEventListener : SensorEventListener {
        val mainActivity:MainActivity

        constructor(mainActivity: MainActivity) {
            this.mainActivity = mainActivity
        }

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
                if (event.values.size > 0) {
                    mainActivity.updatePressure(event.values[0])
                }
            }
        }
    }

    var _sensorManager:SensorManager? = null
    var _sensorEventListener: SensorEventListener? = null

    private val _pressureAltitudeHistory = ArrayDeque<Pair<TimeSource.Monotonic.ValueTimeMark, Double>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById<View?>(R.id.main),
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
                v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            })

        this._locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        this._locationListener = MyLocationListener(this)

        this._sensorManager = this.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        this._sensorEventListener = MySensorEventListener(this)

        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {}

        this.findViewById<Button>(R.id.main_activity_grant_location_button).setOnClickListener {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION))
        }

        this.findViewById<Button>(R.id.main_activity_licensing_information_button).setOnClickListener {
            startActivity(Intent(this, LicensingInformationActivity::class.java))
        }
        this.findViewById<Button>(R.id.main_activity_meter_car1_button).setOnClickListener {
            startActivity(Intent(this, DigitalSpeedometer1Activity::class.java))
        }

        this.findViewById<Button>(R.id.main_activity_change_provider_button).setOnClickListener {
            changeProviderButtonClicked()
        }
        this.findViewById<Button>(R.id.main_activity_speed_unit_button).setOnClickListener {
            changeSpeedUnitButtonClicked()
        }
        this.findViewById<Button>(R.id.main_activity_altitude_unit_button).setOnClickListener {
            changeAltitudeUnitButtonClicked()
        }
        this.findViewById<Button>(R.id.main_activity_air_pressure_unit_button).setOnClickListener {
            changePressureUnitButtonClicked()
        }
        this.findViewById<Button>(R.id.main_activity_refresh_location_provider_button).setOnClickListener {
            updateLocationProvider()
        }

        findViewById<TextView>(R.id.main_activity_version_info_footer).setText(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME)
    }

    override fun onResume() {
        super.onResume()

        updateOptionsShown()

        val fineLocationPermission:Boolean = ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED
        val coarseLocationPermission:Boolean = ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED

        findViewById<View>(R.id.main_activity_permission_location_not_granted).visibility = if (fineLocationPermission) View.GONE else View.VISIBLE
        findViewById<View>(R.id.main_activity_links_to_direct_meters).visibility = View.GONE

        if (fineLocationPermission) {
            findViewById<TextView>(R.id.main_activity_permission_status_textview).setText(R.string.permission_location_fine);
            this.updateLocationProvider()

            if (PreferenceManager.getDefaultSharedPreferences(this).getString(PREFERENCE_KEY_LOCATION_PROVIDER, "")!!.isEmpty()) {
                changeProviderButtonClicked()
            }

        } else {
            clearLocationDisplay()
            findViewById<TextView>(R.id.main_activity_permission_status_textview).setText(if (coarseLocationPermission) {R.string.permission_location_coarse} else {R.string.permission_location_no});
        }

        this._pressureAltitudeHistory.clear()

        val pressureSensorList = this._sensorManager!!.getSensorList(Sensor.TYPE_PRESSURE)
        if (pressureSensorList.size > 0) {
            findViewById<View>(R.id.main_activity_pressure_area).visibility = View.VISIBLE
            findViewById<View>(R.id.main_activity_config_air_pressure_area).visibility = View.VISIBLE
            for (sensor in pressureSensorList) {
                this._sensorManager!!.registerListener(this._sensorEventListener, sensor, 250000)
            }

        } else {
            findViewById<View>(R.id.main_activity_pressure_area).visibility = View.GONE
            findViewById<View>(R.id.main_activity_config_air_pressure_area).visibility = View.GONE
        }
    }

    override fun onStop() {
        this._locationManager!!.removeUpdates(this._locationListener!!)
        this._sensorManager!!.unregisterListener(this._sensorEventListener)
        this._displayedHeightM = Double.NaN
        super.onStop()
    }

    private fun updateOptionsShown() {
        val speedUnit:Int = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREFERENCE_KEY_SPEED_UNIT, PREFERENCE_VAL_SPEED_UNIT_DEFAULT)!!
        val speedUnitName = {
            when(speedUnit) {
                PREFERENCE_VAL_SPEED_UNIT_KM_H -> getText(R.string.unit_km_per_hour)
                PREFERENCE_VAL_SPEED_UNIT_KNOT -> getText(R.string.unit_knot)
                PREFERENCE_VAL_SPEED_UNIT_M_S -> getText(R.string.unit_meter_per_second)
                PREFERENCE_VAL_SPEED_UNIT_MPH -> getText(R.string.unit_mile_per_hour)
                else -> ""
            }
        }()

        findViewById<TextView>(R.id.main_activity_speed_digits_textview).setText("-")
        findViewById<TextView>(R.id.main_activity_config_speed_unit_textview).setText(speedUnitName)
        findViewById<TextView>(R.id.main_activity_speed_unit_textview).setText(speedUnitName)

        val altitudeUnit:Int = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREFERENCE_KEY_ALTITUDE_UNIT, PREFERENCE_VAL_ALTITUDE_DEFAULT)!!
        findViewById<TextView>(R.id.main_activity_altitude_digits_textview).setText("-")
        val altitudeUnitName = {
            when(altitudeUnit) {
                PREFERENCE_VAL_ALTITUDE_METERS -> getText(R.string.unit_meter)
                PREFERENCE_VAL_ALTITUDE_FEET -> getText(R.string.unit_feet)
                else -> ""
            }
        }()
        findViewById<TextView>(R.id.main_activity_config_altitude_unit_textview).setText(altitudeUnitName)
        findViewById<TextView>(R.id.main_activity_altitude_unit_textview).setText(altitudeUnitName)

        val pressureUnit:Int = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREFERENCE_KEY_AIR_PRESSURE_UNIT, PREFERENCE_VAL_AIR_PRESSURE_DEFAULT)!!
        findViewById<TextView>(R.id.main_activity_pressure_digits_textview).setText("-")
        val pressureUnitName = {
            when(pressureUnit) {
                PREFERENCE_VAL_AIR_PRESSURE_HPA -> getText(R.string.unit_hpa)
                PREFERENCE_VAL_AIR_PRESSURE_INHG -> getText(R.string.unit_inhg)
                else -> ""
            }
        }()
        findViewById<TextView>(R.id.main_activity_config_air_pressure_unit_textview).setText(pressureUnitName)
        findViewById<TextView>(R.id.main_activity_pressure_unit_textview).setText(pressureUnitName)

        displayPressureAltitude(Double.NaN)
        displayPressureAltitudeSpeed(Double.NaN)

        findViewById<TextView>(R.id.main_activity_pressure_sea_level_digits_textview).setText("-")
        findViewById<TextView>(R.id.main_activity_pressure_sea_level_unit_textview).setText(pressureUnitName)
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationProvider() {
        this._locationManager!!.removeUpdates(this._locationListener!!)
        clearLocationDisplay()

        val currentPreferenceLocationProvider:String = PreferenceManager.getDefaultSharedPreferences(this).getString(PREFERENCE_KEY_LOCATION_PROVIDER, "")!!

        findViewById<TextView>(R.id.main_activity_config_location_provider_textview).setText(if (currentPreferenceLocationProvider.isEmpty()) getText(R.string.general_caption_unset) else currentPreferenceLocationProvider)

        if (Build.VERSION.SDK_INT >= 28) {
            if (!this._locationManager!!.isLocationEnabled()) {
                findViewById<View>(R.id.main_activity_location_not_enabled).visibility = View.VISIBLE
                findViewById<TextView>(R.id.main_activity_location_not_enabled_message).setText(R.string.error_soft_location_not_enabled)
                return
            }
        }

        if (currentPreferenceLocationProvider.isEmpty()) {
            return
        }

        if (!this._locationManager!!.isProviderEnabled(currentPreferenceLocationProvider)) {
            findViewById<View>(R.id.main_activity_location_not_enabled).visibility = View.VISIBLE
            findViewById<TextView>(R.id.main_activity_location_not_enabled_message).setText(getText(R.string.error_soft_location_provider_not_enabled).toString().format(currentPreferenceLocationProvider))
            return
        }

        findViewById<View>(R.id.main_activity_location_not_enabled).visibility = View.GONE

        this._locationManager!!.requestLocationUpdates(currentPreferenceLocationProvider, 0, 0.0f, this._locationListener!!)
        findViewById<View>(R.id.main_activity_links_to_direct_meters).visibility = View.VISIBLE
    }

    private fun changeProviderButtonClicked() {
        val locationProviders:List<String> = _locationManager!!.getProviders(false)

        val currentPreferenceLocationProvider:String = PreferenceManager.getDefaultSharedPreferences(this).getString(PREFERENCE_KEY_LOCATION_PROVIDER, "")!!

        var checkedItem:Int = -1

        var candidates:Array<CharSequence> = Array(locationProviders.size, {
            val ret:String = locationProviders.get(it)
            if (currentPreferenceLocationProvider.equals(ret)) {
                checkedItem = it
            }
            ret
        })

        AlertDialog.Builder(this).setTitle(R.string.dialog_select_location_provider).setSingleChoiceItems(candidates, checkedItem, DialogInterface.OnClickListener {
                dialog, which ->
            val prefEdit =
                PreferenceManager.getDefaultSharedPreferences(this).edit()
            prefEdit.putString(PREFERENCE_KEY_LOCATION_PROVIDER, locationProviders.get(which))
            prefEdit.apply()
            dialog.cancel()
            this.updateLocationProvider()

        }).create().show()
    }

    private fun changeSpeedUnitButtonClicked() {
        val currentSpeedUnit:Int = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREFERENCE_KEY_SPEED_UNIT, PREFERENCE_VAL_SPEED_UNIT_DEFAULT)!!

        val candidates:Array<CharSequence> = Array(4, {
            when(it) {
                PREFERENCE_VAL_SPEED_UNIT_KM_H -> getText(R.string.unit_km_per_hour)
                PREFERENCE_VAL_SPEED_UNIT_KNOT -> getText(R.string.unit_knot)
                PREFERENCE_VAL_SPEED_UNIT_M_S -> getText(R.string.unit_meter_per_second)
                PREFERENCE_VAL_SPEED_UNIT_MPH -> getText(R.string.unit_mile_per_hour)
                else -> ""
            }
        })
        AlertDialog.Builder(this).setTitle(R.string.dialog_select_speed_unit).setSingleChoiceItems(candidates, currentSpeedUnit, DialogInterface.OnClickListener {
                dialog, which ->
            val prefEdit =
                PreferenceManager.getDefaultSharedPreferences(this).edit()
            prefEdit.putInt(PREFERENCE_KEY_SPEED_UNIT, which)
            prefEdit.apply()
            dialog.cancel()
            this.updateOptionsShown()
        }).create().show()
    }

    private fun changeAltitudeUnitButtonClicked() {
        val currentAltitudeUnit:Int = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREFERENCE_KEY_ALTITUDE_UNIT, PREFERENCE_VAL_ALTITUDE_DEFAULT)!!

        val candidates:Array<CharSequence> = Array(2, {
            when(it) {
                PREFERENCE_VAL_ALTITUDE_METERS -> getText(R.string.unit_meter)
                PREFERENCE_VAL_ALTITUDE_FEET -> getText(R.string.unit_feet)
                else -> ""
            }
        })
        AlertDialog.Builder(this).setTitle(R.string.dialog_select_altitude_unit).setSingleChoiceItems(candidates, currentAltitudeUnit, DialogInterface.OnClickListener {
            dialog, which ->
                val prefEdit =
                    PreferenceManager.getDefaultSharedPreferences(this).edit()
                prefEdit.putInt(PREFERENCE_KEY_ALTITUDE_UNIT, which)
                prefEdit.apply()
                dialog.cancel()
                this.updateOptionsShown()
        }).create().show()
    }

    private fun changePressureUnitButtonClicked() {
        val currentPressureUnit:Int = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREFERENCE_KEY_AIR_PRESSURE_UNIT, PREFERENCE_VAL_AIR_PRESSURE_DEFAULT)!!

        val candidates:Array<CharSequence> = Array(2, {
            when(it) {
                PREFERENCE_VAL_AIR_PRESSURE_HPA -> getText(R.string.unit_hpa)
                PREFERENCE_VAL_AIR_PRESSURE_INHG -> getText(R.string.unit_inhg)
                else -> ""
            }
        })
        AlertDialog.Builder(this).setTitle(R.string.dialog_select_air_pressure_unit).setSingleChoiceItems(candidates, currentPressureUnit, DialogInterface.OnClickListener {
                dialog, which ->
            val prefEdit =
                PreferenceManager.getDefaultSharedPreferences(this).edit()
            prefEdit.putInt(PREFERENCE_KEY_AIR_PRESSURE_UNIT, which)
            prefEdit.apply()
            dialog.cancel()
            this.updateOptionsShown()
        }).create().show()
    }

    private fun degreeToDisplayText(degree:Double, positiveAsix:String, negativeAxis:String): String {
        var degreeRemain:Double = degree
        var axisText = positiveAsix

        if (degreeRemain < 0) {
            degreeRemain = -degreeRemain
            axisText = negativeAxis
        }

        val degInt:Int = degreeRemain.toInt()
        degreeRemain -= degInt
        degreeRemain *= 60

        val degMinInt:Int = degreeRemain.toInt()
        degreeRemain -= degMinInt
        degreeRemain *= 60

        val degSecInt:Int = degreeRemain.toInt()
        degreeRemain -= degSecInt
        val degSubSecInt:Int = (degreeRemain * 10.0).toInt()

        return axisText + getText(R.string.unit_angle_deg).toString().format(degInt, degMinInt, degSecInt, degSubSecInt)
    }

    private fun calculateLinearRegressionSlope(
        samples: Collection<Pair<TimeSource.Monotonic.ValueTimeMark, Double>>,
    ): Double {
        // https://ja.wikipedia.org/wiki/%E7%B7%9A%E5%BD%A2%E5%9B%9E%E5%B8%B0
        if (samples.size < 2) {
            return 0.0
        }

        val originTimeMark = samples.first().first

        val points: List<Pair<Double, Double>> = samples.map { (time, height) ->
            val elapsedSeconds =
                (time - originTimeMark).inWholeNanoseconds / 1_000_000_000.0

            elapsedSeconds to height
        }

        val meanTime =
            points.map { it.first }.average()

        val meanHeight =
            points.map { it.second }.average()

        var numerator = 0.0
        var denominator = 0.0

        for ((time, height) in points) {
            val timeDeviation = time - meanTime
            val heightDeviation = height - meanHeight

            numerator += timeDeviation * heightDeviation
            denominator += timeDeviation * timeDeviation
        }

        if (denominator == 0.0) {
            return 0.0
        }

        return numerator / denominator
    }

    private fun displayPressureAltitude(pressureAltitudeM: Double) {
        val altitudeUnit:Int = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREFERENCE_KEY_ALTITUDE_UNIT, PREFERENCE_VAL_ALTITUDE_DEFAULT)!!
        when(altitudeUnit) {
            PREFERENCE_VAL_ALTITUDE_METERS -> {
                if(pressureAltitudeM.isNaN()) {
                    findViewById<TextView>(R.id.main_activity_pressure_altitude_digits_textview).setText("      -")
                } else {
                    findViewById<TextView>(R.id.main_activity_pressure_altitude_digits_textview).setText(String.format("%7.1f", pressureAltitudeM))
                }
                findViewById<TextView>(R.id.main_activity_pressure_altitude_unit_textview).setText(R.string.unit_meter)
            }
            PREFERENCE_VAL_ALTITUDE_FEET -> {
                if(pressureAltitudeM.isNaN()) {
                    findViewById<TextView>(R.id.main_activity_pressure_altitude_digits_textview).setText("     -")
                } else {
                    findViewById<TextView>(R.id.main_activity_pressure_altitude_digits_textview).setText(String.format("%6.0f", (pressureAltitudeM / 0.3048)))
                }
                findViewById<TextView>(R.id.main_activity_pressure_altitude_unit_textview).setText(R.string.unit_feet)
            }
        }

    }

    /*
     * inHg conversion
     *
     * 1 inHg ~= 3386.389 Pa
     *   Couldn't find the precise information, but at least, the following refers inHg as 3386.389 Pa
     *     https://en.wikipedia.org/wiki/Inch_of_mercury
     *     https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication811e2008.pdf
     *
     *   Japanese 計量法 (Measrurement Act.): defines inHg as 3386.39 Pa
     *     https://laws.e-gov.go.jp/law/404CO0000000357
     *     > 水銀柱インチ パスカル又はニュートン毎平方メートルの三千三百八十六・三九倍
     *
     *   As long as displaying just 5 digits (like 29.921 inHg), further precision is not a problem. Using 3386.389.
     */
    fun updatePressure(pressure_hPa: Float) {
        val nowTimeMark: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()

        val pressureUnit:Int = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREFERENCE_KEY_AIR_PRESSURE_UNIT, PREFERENCE_VAL_AIR_PRESSURE_DEFAULT)!!
        when(pressureUnit) {
            PREFERENCE_VAL_AIR_PRESSURE_HPA -> {
                findViewById<TextView>(R.id.main_activity_pressure_digits_textview).setText(String.format("%.2f", pressure_hPa))
                findViewById<TextView>(R.id.main_activity_pressure_unit_textview).setText(R.string.unit_hpa)
            }
            PREFERENCE_VAL_AIR_PRESSURE_INHG -> {
                findViewById<TextView>(R.id.main_activity_pressure_digits_textview).setText(String.format("%.3f", pressure_hPa / 33.86389))
                findViewById<TextView>(R.id.main_activity_pressure_unit_textview).setText(R.string.unit_inhg)
            }
        }

        val pressureAltitude:Double = ISAPressureAltitude.pressureHpaToAltitudeM(pressure_hPa.toDouble())
        this.displayPressureAltitude(pressureAltitude)

        var pressureSeaLevel:Double = Double.NaN
        if (! this._displayedHeightM.isNaN()) {
            pressureSeaLevel = ISAPressureAltitude.pressureHpaAtSeaLevel(
                pressure_hPa.toDouble(),
                this._displayedHeightM
            )
        }
        if (! pressureSeaLevel.isNaN()) {
            when(pressureUnit) {
                PREFERENCE_VAL_AIR_PRESSURE_HPA -> {
                    findViewById<TextView>(R.id.main_activity_pressure_sea_level_digits_textview).setText(String.format("%.1f", pressureSeaLevel))
                    findViewById<TextView>(R.id.main_activity_pressure_sea_level_unit_textview).setText(R.string.unit_hpa)
                }
                PREFERENCE_VAL_AIR_PRESSURE_INHG -> {
                    findViewById<TextView>(R.id.main_activity_pressure_sea_level_digits_textview).setText(String.format("%.2f", pressureSeaLevel / 33.86389))
                    findViewById<TextView>(R.id.main_activity_pressure_sea_level_unit_textview).setText(R.string.unit_inhg)
                }
            }
        } else {
            findViewById<TextView>(R.id.main_activity_pressure_sea_level_digits_textview).setText("-")
            when(pressureUnit) {
                PREFERENCE_VAL_AIR_PRESSURE_HPA -> {
                    findViewById<TextView>(R.id.main_activity_pressure_sea_level_unit_textview).setText(R.string.unit_hpa)
                }
                PREFERENCE_VAL_AIR_PRESSURE_INHG -> {
                    findViewById<TextView>(R.id.main_activity_pressure_sea_level_unit_textview).setText(R.string.unit_inhg)
                }
            }
        }

        while (_pressureAltitudeHistory.size > 12) {
            _pressureAltitudeHistory.removeFirst()
        }
        _pressureAltitudeHistory.add(nowTimeMark to pressureAltitude)

        if (_pressureAltitudeHistory.size >= 13) {
            val speedMPerMin:Double = calculateLinearRegressionSlope(_pressureAltitudeHistory) * 60.0
            displayPressureAltitudeSpeed(speedMPerMin)

        } else {
            displayPressureAltitudeSpeed(Double.NaN)
        }
    }

    private fun displayPressureAltitudeSpeed(altitudeSpeedMPerMin: Double) {
        val altitudeUnit:Int = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREFERENCE_KEY_ALTITUDE_UNIT, PREFERENCE_VAL_ALTITUDE_DEFAULT)!!

        when(altitudeUnit) {
            PREFERENCE_VAL_ALTITUDE_METERS -> {
                if (altitudeSpeedMPerMin.isNaN()) {
                    findViewById<TextView>(R.id.main_activity_pressure_altitude_speed_digits_textview).setText("    -")
                } else {
                    val valToShow = Math.floor(altitudeSpeedMPerMin + 0.5).toInt()
                    val valToShowAbs:Int = Math.abs(valToShow)
                    var valToShowSign:String = " "
                    if (valToShow > 0) {
                        valToShowSign = "+"
                    }
                    if (valToShow < 0) {
                        valToShowSign = "-"
                    }
                    findViewById<TextView>(R.id.main_activity_pressure_altitude_speed_digits_textview).setText(
                        String.format( ConfigurationCompat.getLocales(resources.configuration)[0], "%s%4d", valToShowSign, valToShowAbs)
                    )
                }
                findViewById<TextView>(R.id.main_activity_pressure_altitude_speed_unit_textview).setText(R.string.unit_meter_per_minute)
            }
            PREFERENCE_VAL_ALTITUDE_FEET -> {
                if (altitudeSpeedMPerMin.isNaN()) {
                    findViewById<TextView>(R.id.main_activity_pressure_altitude_speed_digits_textview).setText("    -")
                } else {
                    val valToShow = Math.floor(altitudeSpeedMPerMin / 0.3048 + 0.5).toInt()
                    val valToShowAbs:Int = Math.abs(valToShow)
                    var valToShowSign:String = " "
                    if (valToShow > 0) {
                        valToShowSign = "+"
                    }
                    if (valToShow < 0) {
                        valToShowSign = "-"
                    }
                    findViewById<TextView>(R.id.main_activity_pressure_altitude_speed_digits_textview).setText(
                        String.format( ConfigurationCompat.getLocales(resources.configuration)[0], "%s%4d", valToShowSign, valToShowAbs)
                    )
                }
                findViewById<TextView>(R.id.main_activity_pressure_altitude_speed_unit_textview).setText(R.string.unit_feet_per_minute)
            }
        }
    }

    /*
     * Imperial units conversion
     *
     * 1 yard = 0.9144 m
     *
     * https://books.google.com/books?id=4aWN-VRV1AoC&pg=PA13  ([1] in Wikipedia link below on 2021/09/12)
     * > According to the agreement, the international yard equals 0.9144 meter and the international pound equals 0.453 592 37 kilogram.
     *
     * https://en.wikipedia.org/wiki/International_yard_and_pound
     * > The international yard and pound are two units of measurement that were the subject of an agreement among representatives of six nations signed on 1 July 1959; the United States, United Kingdom, Canada, Australia, New Zealand, and South Africa. The agreement defined the yard as exactly 0.9144 meters and the (avoirdupois) pound as exactly 0.45359237 kilograms.[1]
     *
     * https://www.legislation.gov.uk
     * yard
     * > 0.9144 metre
     *
     * https://elaws.e-gov.go.jp/document?lawid=404CO0000000357
     * ヤード
     * > メートルの〇・九一四四倍
     *
     *
     * 1 mile = 1760 yard
     *
     * https://en.wikipedia.org/wiki/Mile
     * >  The statute mile was standardised between the British Commonwealth and the United States by an international agreement in 1959, when it was formally redefined with respect to SI units as exactly 1,609.344 metres.
     *
     * https://www.legislation.gov.uk/uksi/1995/1804/schedule/made
     * mile
     * > 1.609344 kilometres
     *
     * https://elaws.e-gov.go.jp/document?lawid=404CO0000000357
     * マイル
     * > ヤードの千七百六十倍
     *
     * 1 nautical mile = 1852 m
     *
     * https://en.wikipedia.org/wiki/Nautical_mile
     * > Today the international nautical mile is defined as exactly 1852 metres (6076 ft; 1.151 mi). The derived unit of speed is the knot, one nautical mile per hour.
     *
     * https://elaws.e-gov.go.jp/document?lawid=404CO0000000357
     * ノット
     * > 一時間に千八百五十二メートルの速さ
     */
    fun updateLocation(location:Location) {
        val speedUnit:Int = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREFERENCE_KEY_SPEED_UNIT, PREFERENCE_VAL_SPEED_UNIT_DEFAULT)!!
        when(speedUnit) {
            PREFERENCE_VAL_SPEED_UNIT_KM_H -> {
                findViewById<TextView>(R.id.main_activity_speed_digits_textview).setText((location.speed * 3.6).toInt().toString())
                findViewById<TextView>(R.id.main_activity_speed_unit_textview).setText(R.string.unit_km_per_hour)
            }
            PREFERENCE_VAL_SPEED_UNIT_KNOT -> {
                findViewById<TextView>(R.id.main_activity_speed_digits_textview).setText((location.speed * 3.6 / 1.852).toInt().toString())
                findViewById<TextView>(R.id.main_activity_speed_unit_textview).setText(R.string.unit_knot)
            }
            PREFERENCE_VAL_SPEED_UNIT_M_S -> {
                findViewById<TextView>(R.id.main_activity_speed_digits_textview).setText((location.speed).toInt().toString())
                findViewById<TextView>(R.id.main_activity_speed_unit_textview).setText(R.string.unit_meter_per_second)
            }
            PREFERENCE_VAL_SPEED_UNIT_MPH -> {
                findViewById<TextView>(R.id.main_activity_speed_digits_textview).setText((location.speed * 3.6 / 1.609344).toInt().toString())
                findViewById<TextView>(R.id.main_activity_speed_unit_textview).setText(R.string.unit_mile_per_hour)
            }
        }

        if (Build.VERSION.SDK_INT >= 34 && !location.hasMslAltitude()) {
            // There are both cases that msl altitude is automatically added and not
            try {
                AltitudeConverter().addMslAltitudeToLocation(this, location)
            } catch (e:IllegalArgumentException) {
                // ignore, just continue without MSL altitude
                // There is a documented case
            } catch (e:IOException) {
                // IOException is also documented
            }
        }

        val altitudeUnit:Int = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREFERENCE_KEY_ALTITUDE_UNIT, PREFERENCE_VAL_ALTITUDE_DEFAULT)!!
        var altitudeMeterToShow:Double
        if (Build.VERSION.SDK_INT >= 34 && location.hasMslAltitude()) {
            altitudeMeterToShow = location.mslAltitudeMeters
            findViewById<TextView>(R.id.main_activity_altitude_caption_textview).setText(R.string.metrics_msl_altitude)
        } else {
            // This case the height is WGS84 based
            // https://developer.android.com/reference/android/location/Location#getAltitude()
            altitudeMeterToShow = location.altitude
            findViewById<TextView>(R.id.main_activity_altitude_caption_textview).setText(R.string.metrics_wgs84_altitude)
        }
        when(altitudeUnit) {
            PREFERENCE_VAL_ALTITUDE_METERS -> {
                findViewById<TextView>(R.id.main_activity_altitude_digits_textview).setText(altitudeMeterToShow.toInt().toString())
                findViewById<TextView>(R.id.main_activity_altitude_unit_textview).setText(R.string.unit_meter)
            }
            PREFERENCE_VAL_ALTITUDE_FEET -> {
                findViewById<TextView>(R.id.main_activity_altitude_digits_textview).setText((altitudeMeterToShow / 0.3048).toInt().toString())
                findViewById<TextView>(R.id.main_activity_altitude_unit_textview).setText(R.string.unit_feet)
            }
        }
        this._displayedHeightM = altitudeMeterToShow

        var currentCordinateText:String = ""
        currentCordinateText += (degreeToDisplayText(location.longitude, getText(R.string.coordinate_display_east).toString(), getText(R.string.coordinate_display_west).toString()) + " " +
                degreeToDisplayText(location.latitude, getText(R.string.coordinate_display_north).toString(), getText(R.string.coordinate_display_south).toString()))
        if (Build.VERSION.SDK_INT >= 34 && location.hasMslAltitude()) {
            // Here also adds WGS84 altitude
            // https://developer.android.com/reference/android/location/Location#getAltitude()
            currentCordinateText += "\n" + getText(R.string.metrics_wgs84_altitude) + ": "
            when(altitudeUnit) {
                PREFERENCE_VAL_ALTITUDE_METERS -> {
                    currentCordinateText += location.altitude.toInt().toString() + " " + getText(R.string.unit_meter)
                }
                PREFERENCE_VAL_ALTITUDE_FEET -> {
                    currentCordinateText += ((location.altitude / 0.3048).toInt().toString()) + " " + getText(R.string.unit_feet)
                }
            }
        }
        findViewById<TextView>(R.id.main_activity_current_coordinate_textview).setText(currentCordinateText)

        var geolocationDetailText = ""
        geolocationDetailText += location.provider + "\n";
        geolocationDetailText += getText(R.string.metrics_accuracy).toString() + ": " + String.format("%.1f", location.accuracy) + " " + getText(R.string.unit_meter) +  "\n"
        if (Build.VERSION.SDK_INT >= 34 && location.hasMslAltitude()) {
            geolocationDetailText += getText(R.string.metrics_msl_altitude_accuracy).toString() + ": "
            when(altitudeUnit) {
                PREFERENCE_VAL_ALTITUDE_METERS -> {
                    geolocationDetailText += (location.mslAltitudeAccuracyMeters + 0.5).toInt().toString() + " " + getText(R.string.unit_meter)
                }
                PREFERENCE_VAL_ALTITUDE_FEET -> {
                    geolocationDetailText += ((location.mslAltitudeAccuracyMeters / 0.3048 + 0.5).toInt().toString()) + " " + getText(R.string.unit_feet)
                }
            }
            geolocationDetailText += "\n"
        }
        geolocationDetailText += getText(R.string.metrics_info_time).toString() + ": " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(location.time))

        findViewById<TextView>(R.id.main_activity_geolocation_detail_textview).setText(geolocationDetailText)

        findViewById<View>(R.id.main_activity_location_not_enabled).visibility = View.GONE
        findViewById<View>(R.id.main_activity_links_to_direct_meters).visibility = View.VISIBLE
    }

    fun clearLocationDisplay() {
        findViewById<TextView>(R.id.main_activity_config_location_provider_textview).setText(R.string.general_caption_unset)

        findViewById<TextView>(R.id.main_activity_speed_digits_textview).setText("-")

        findViewById<TextView>(R.id.main_activity_altitude_digits_textview).setText("-")

        findViewById<TextView>(R.id.main_activity_current_coordinate_textview).setText("---")

        findViewById<TextView>(R.id.main_activity_geolocation_detail_textview).setText("")
    }
}
