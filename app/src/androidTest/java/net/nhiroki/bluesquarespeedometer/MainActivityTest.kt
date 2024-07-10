package net.nhiroki.bluesquarespeedometer

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        // TODO: this code should guarantee that the language is set to English
        // val config = Configuration()
        // config.setLocale(Locale("en", "US"))
        // context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    @Test
    fun testJustStartingUp() {
        // Just test app starts without problems loading libraries
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(3000)
    }
}