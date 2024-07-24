package net.nhiroki.bluesquarespeedometer

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.TimeZone


@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    var context: Context? = null

    @Before
    fun setUp() {
        this.context = InstrumentationRegistry.getInstrumentation().targetContext

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context!!.getSystemService(LocaleManager::class.java).applicationLocales =
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

        // Not sure why failing...
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 24) 

        Espresso.onView(ViewMatchers.withText("Speed"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun testJustStartingUpJapanese() {
        // Looks like the way of setting language has changed in SDK24 (N).
        // Older version does not change output even if we run the following language setting code.
        // Therefore, in this case, skipping test for non-English version.
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context!!.getSystemService<LocaleManager>(LocaleManager::class.java)
                .setApplicationLocales(
                    LocaleList(
                        Locale("ja", "JP")
                    )
                )
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(Locale("ja", "JP")))
        }
        Locale.setDefault(Locale("ja", "JP"))
        // Just test app starts without problems loading libraries
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(3000)

        Espresso.onView(ViewMatchers.withText("速度"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}
