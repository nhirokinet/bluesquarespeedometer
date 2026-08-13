package net.nhiroki.bluesquarespeedometer

object ISAPressureAltitude {
    fun pressureHpaToAltitudeM(pressureHPa: Double): Double {
        // Note when I checked the correctness of the formula referencing Chat AI
        //
        // p = ρRT
        //   here R is constant for each gas with dimension as the same as p/ρT, which is
        //   (kg・m/s^2・m^-2)/(kg/m^3)/K = m^2・s^-2・K^-1
        //   Here 1J = 1N・m = kg・m/s^2・m = kg・m^2・s^-2
        //   Thus R has J/(kg・K)
        //
        // dp = -ρg dh
        // here, ρ = p/(RT)
        // therefore dp = - pg/(RT) dh
        // or dp/p = -g/RT dh
        //
        // So if T is constant, h/(RT) is constant, so
        // From
        // dp/p = -g/RT dh
        // it is
        // log_e(p / p_b) = -g/RT (h - h_b)
        // transformed to
        // h - h_b = - R T log_e(p / p_b) / g
        //
        // If T is changing by height, the situation is complex
        // If T is decreasing at constant rate of Γ = -dT/dh,
        // dp/p = -g/(R・(T_b - Γ(h - h_b)) dh
        // Here, dh = - dT/Γ
        // Therefore
        // dp/p = g/(RT) dt/Γ = g/RΓ dT/T
        // Integrate,
        // log_e(p/p_b) = g/RΓ log_e(T/T_b)
        // Thefore p/p_b = (T/T_b) ^ (g/RΓ)
        // T = T_b * (p/p_b) ^(RΓ/g)
        // And we want height
        // h = h_b + -(T-T_b)/Γ
        //   = h_b + - T_b * ((p/p_b)^(RΓ/g) - 1) / Γ
        //   = h_b + T_b * (1 - (p/p_b)^(RΓ/g)) / Γ

        val pressurePa:Double = pressureHPa * 100.0
        val g_m_s2:Double = 9.80665
        // Specific constant of dry air
        // at least it is one of used values
        // https://www.atmospheris.org/iso-2533
        val R:Double = 287.05287

        if (pressurePa > 150000.0) {
            return Double.NaN
        }

        // https://ja.wikipedia.org/wiki/%E5%9B%BD%E9%9A%9B%E6%A8%99%E6%BA%96%E5%A4%A7%E6%B0%97

        if (pressurePa > 22632.0) {
            return ((15.0 + 273.15) / 0.0065) * (1 - Math.pow(pressurePa / 101325.0, (0.0065 * R / g_m_s2)))
        }
        if (pressurePa > 5474.9) {
            return 11000.0 - ((-56.5 + 273.15) * R / g_m_s2) * Math.log(pressurePa / 22632.0)
        }
        if (pressurePa > 868.02) {
            return 20000.0 + ((-56.5 + 273.15) / -0.001) * (1 - Math.pow(pressurePa / 5474.9, (-0.001 * R / g_m_s2)))
        }
        if (pressurePa > 110.91) {
            return 32000.00 + ((-44.5 + 273.15) / -0.0028) * (1 - Math.pow(pressurePa / 868.02, (-0.0028 * R / g_m_s2)))
        }
        if (pressurePa > 66.939) {
            return 47000.0 - ((-2.5 + 273.15) * R / g_m_s2) * Math.log(pressurePa / 110.91)
        }
        if (pressurePa > 3.9564) {
            return 51000.0 + ((-2.5 + 273.15) / 0.0028) * (1 - Math.pow(pressurePa / 66.939, (0.0028 * R / g_m_s2)))
        }
        if (pressurePa > 0.3734) {
            return 71000.0 + ((-58.5 + 273.15) / 0.002) * (1 - Math.pow(pressurePa / 3.9564, (0.002 * R / g_m_s2)))
        }

        return Double.NaN
    }

    fun pressureHpaAtSeaLevel(pressureHPa:Double, altitudeM:Double):Double {
        // Note
        // in comment on pressureHpaToAltitudeM
        // h = h_b + T_b * (1 - (p/p_b)^(RΓ/g)) / Γ
        // Therefore
        // (p/p_b)^(RΓ/g) = 1 - Γ (h - h_b) / T_b
        // which means
        // p_b = p・(1 - Γ (h - h_b) / T_b)^(-g/RΓ)
        // which matches
        // https://ja.wikipedia.org/wiki/%E6%B5%B7%E9%9D%A2%E6%9B%B4%E6%AD%A3

        val g_m_s2:Double = 9.80665
        // Specific constant of dry air
        // at least it is one of used values
        // https://www.atmospheris.org/iso-2533
        val R:Double = 287.05287

        if (pressureHPa < 226.32 || altitudeM > 11000.0) {
            // If higher than 11km, it would not be suitable to calculate the pressure at sea level
            // as this is stratosphere and changing of temperature is not expected as the same
            return Double.NaN
        }

        return pressureHPa * Math.pow(1 - 0.0065 * altitudeM / (15.0 + 273.15), - g_m_s2 / R / 0.0065)
    }
}