package com.wiersbitzki.luxtronik.services

import com.wiersbitzki.luxtronik.Content
import com.wiersbitzki.luxtronik.cop

enum class Betriebszustand {
    HEIZEN { // Heizung
        override fun heizung(leistungsaufnahme: Double) = leistungsaufnahme
        override fun warmwasser(leistungsaufnahme: Double) = 0.0
        override fun copHeizung(heizleistung: Double, leistungsaufnahme: Double) =
            Pair(heizleistung, leistungsaufnahme).cop()
    },
    WW { // Warmwasser
        override fun heizung(leistungsaufnahme: Double) = 0.0
        override fun warmwasser(leistungsaufnahme: Double) = leistungsaufnahme
        override fun copWarmwasser(heizleistung: Double, leistungsaufnahme: Double) =
            Pair(heizleistung, leistungsaufnahme).cop()
    },
    ABT, // Abtauung
    UNDEFINED;

    open fun heizung(leistungsaufnahme: Double) = leistungsaufnahme / 2
    open fun warmwasser(leistungsaufnahme: Double) = leistungsaufnahme / 2
    open fun copHeizung(heizleistung: Double, leistungsaufnahme: Double) = 0.0
    open fun copWarmwasser(heizleistung: Double, leistungsaufnahme: Double) = 0.0

    companion object {
        fun from(content: Content) = Betriebszustand.entries.find {
            it.name == content.readValue("Betriebszustand").uppercase()
        } ?: UNDEFINED
    }
}
