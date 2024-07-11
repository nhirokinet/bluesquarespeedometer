package net.nhiroki.bluesquarespeedometer

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.TimeZone


@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList(Locale("en", "US"))
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(Locale("en", "US")))
        }
        Locale.setDefault(Locale("en", "US"))
    }

    @Test
    fun testJustStartingUp() {
        // Just test app starts without problems loading libraries
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(3000)
    }
}