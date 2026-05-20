package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import java.math.RoundingMode

enum class Betriebszustand {
    HEIZEN { // Heizung
        override fun heizung(leistungsaufnahme: Double) = leistungsaufnahme
        override fun warmwasser(leistungsaufnahme: Double) = 0.0
        override fun copWarmwasser(heizleistung: Double, leistungsaufnahme: Double) = 0.0
    },
    WW { // Warmwasser
        override fun heizung(leistungsaufnahme: Double) = 0.0
        override fun warmwasser(leistungsaufnahme: Double) = leistungsaufnahme
        override fun copHeizung(heizleistung: Double, leistungsaufnahme: Double) = 0.0
    },
    ABT, // Abtauung
    UNDEFINED;

    companion object {
        fun from(content: Content) = Betriebszustand.entries.find {
            it.name == content.readValue("Betriebszustand").uppercase()
        } ?: UNDEFINED
    }

    open fun heizung(leistungsaufnahme: Double) = leistungsaufnahme / 2
    open fun warmwasser(leistungsaufnahme: Double) = leistungsaufnahme / 2
    open fun copHeizung(heizleistung: Double, leistungsaufnahme: Double) = cop(heizleistung, leistungsaufnahme)
    open fun copWarmwasser(heizleistung: Double, leistungsaufnahme: Double) = cop(heizleistung, leistungsaufnahme)
    fun copGesamt(heizleistung: Double, leistungsaufnahme: Double) = cop(heizleistung, leistungsaufnahme)

    private fun cop(heizleistung: Double, leistungsaufnahme: Double) =
        if (leistungsaufnahme != 0.0) (heizleistung / leistungsaufnahme).round() else 0.0

    private fun Double.round(scale: Int = 2) = toBigDecimal()
        .setScale(scale, RoundingMode.HALF_UP)
        .toDouble()
}
