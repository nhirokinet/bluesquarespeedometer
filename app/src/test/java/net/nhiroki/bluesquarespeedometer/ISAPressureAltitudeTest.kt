package net.nhiroki.bluesquarespeedometer

import org.junit.Test

import org.junit.Assert.*

class ISAPressureAltitudeTest {
    @Test
    fun testPressureToHeight() {
        assertTrue(ISAPressureAltitude.pressureHpaToAltitudeM(10000.0).isNaN())
        assertTrue(ISAPressureAltitude.pressureHpaToAltitudeM(1501.0).isNaN())

        assertEquals(-3000.0, ISAPressureAltitude.pressureHpaToAltitudeM(1499.99), 1000.0)

        // https://ja.wikipedia.org/wiki/%E5%9B%BD%E9%9A%9B%E6%A8%99%E6%BA%96%E5%A4%A7%E6%B0%97
        assertEquals(0.0, ISAPressureAltitude.pressureHpaToAltitudeM(1013.25), 0.01)
        assertEquals(3776.0, ISAPressureAltitude.pressureHpaToAltitudeM(634.613), 0.1)
        assertEquals(11000.0, ISAPressureAltitude.pressureHpaToAltitudeM(226.3201), 0.01)
        assertEquals(11000.0, ISAPressureAltitude.pressureHpaToAltitudeM(226.3199), 0.01)
        assertEquals(19999.96, ISAPressureAltitude.pressureHpaToAltitudeM(54.7490001), 0.01)
        assertEquals(19999.86, ISAPressureAltitude.pressureHpaToAltitudeM(54.7498999), 0.01)
        assertEquals(31999.99, ISAPressureAltitude.pressureHpaToAltitudeM(8.68020001), 0.01)
        assertEquals(32000.01, ISAPressureAltitude.pressureHpaToAltitudeM(8.68019999), 0.01)
        assertEquals(46999.9, ISAPressureAltitude.pressureHpaToAltitudeM(1.10910001), 0.2)
        assertEquals(47000.01, ISAPressureAltitude.pressureHpaToAltitudeM(1.10909999), 0.01)
        assertEquals(50999.9, ISAPressureAltitude.pressureHpaToAltitudeM(0.66939001), 0.4)
        assertEquals(51000.01, ISAPressureAltitude.pressureHpaToAltitudeM(0.66938999), 0.01)
        assertEquals(70999.99, ISAPressureAltitude.pressureHpaToAltitudeM(0.03956401), 0.1)
        assertEquals(71000.01, ISAPressureAltitude.pressureHpaToAltitudeM(0.03956399), 0.1)
        assertEquals(84851.99, ISAPressureAltitude.pressureHpaToAltitudeM(0.00373401), 0.3)

        assertTrue(ISAPressureAltitude.pressureHpaToAltitudeM(0.003733999).isNaN())

        assertTrue(ISAPressureAltitude.pressureHpaToAltitudeM(0.00000001).isNaN())
    }

    @Test
    fun testPressureHpaAtSeaLevel() {
        assertTrue(ISAPressureAltitude.pressureHpaAtSeaLevel(225.0, 0.0).isNaN())
        assertTrue(ISAPressureAltitude.pressureHpaAtSeaLevel(1013.25, 11000.1).isNaN())

        assertEquals(1013.25, ISAPressureAltitude.pressureHpaAtSeaLevel(1013.25, 0.0), 0.01)
        assertEquals(1013.25, ISAPressureAltitude.pressureHpaAtSeaLevel(634.613, 3776.0), 0.01)
        assertEquals(1013.25, ISAPressureAltitude.pressureHpaAtSeaLevel(226.3201, 10999.999), 0.01)

        for (pressureHpa in 227 until 1015) {
            assertEquals(1013.25, ISAPressureAltitude.pressureHpaAtSeaLevel(pressureHpa.toDouble(), ISAPressureAltitude.pressureHpaToAltitudeM(pressureHpa.toDouble())), 0.001)
        }

        assertEquals(1000.0, ISAPressureAltitude.pressureHpaAtSeaLevel(1000.0, 0.0), 0.01)
        assertEquals(1005.95, ISAPressureAltitude.pressureHpaAtSeaLevel(1000.0, 50.0), 0.01)
    }
}