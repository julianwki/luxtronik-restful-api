package com.wiersbitzki.luxtronik

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.websocket.send
import kotlinx.serialization.Serializable
import java.math.RoundingMode

interface DataService : Logging {
    suspend fun data(): Map<String, Any>
    suspend fun DefaultClientWebSocketSession.receiveContent(id: String): Content {
        send("GET;${id}")
        return receiveDeserialized<Content>()
    }

    @Serializable
    data class Navigation(
        val type: String = "Navigation",
        val items: List<NavigationItem>,
        val id: String,
    ) : Logging {
        fun readNavigationId(vararg path: String): String {
            logger.trace(this.toString())
            for (item in items) {
                if (item.name.trim() == path[0]) {
                    return path.drop(1).let {
                        if (it.isEmpty()) item.id else item.findNavigationItemWithName(it)?.id ?: break
                    }
                }
            }
            throw IllegalArgumentException("Unable to find item with path ${path.joinToString("/")}")
        }
    }

    @Serializable
    data class NavigationItem(
        val items: List<NavigationItem> = emptyList(),
        val id: String,
        val name: String,
        val readOnly: Boolean? = null,
    ) {
        internal fun findNavigationItemWithName(path: List<String>): NavigationItem? {
            for (item in items) {
                if (item.name.trim() == path[0]) {
                    return path.drop(1).let {
                        if (it.isEmpty()) item else item.findNavigationItemWithName(it)
                    }
                }
            }
            return null
        }
    }

    @Serializable
    data class Content(
        val type: String = "Content",
        val items: List<ContentItem>,
        val name: String,
    ) : Logging {
        fun readValue(name: String, suffix: String = "") =
            items.first { it.name.trim() == name }
                .value
                .let { if (suffix.isNotEmpty()) it.substringBefore(suffix) else it }
                .trim()
                .also { logger.debug("{}: {}", name, it) }
                .also { logger.trace(this.toString()) }

        fun readDouble(name: String, suffix: String = "") = readValue(name.trim(), suffix).toDouble()
    }

    @Serializable
    data class ContentItem(
        val name: String,
        val value: String,
        val id: String,
    )

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
}

interface SwitchService : Logging {
    fun activate(function: String, register: Int)
    fun deactivate(function: String, register: Int)
    fun switch(function: String, register: Int, onOff: OnOff)
    fun read(function: String, register: Int): Int
    fun readInput(function: String, register: Int): Int
    fun write(function: String, register: Int, value: Int)

    enum class OnOff(
        val value: Int
    ) {
        ON(1),
        OFF(0)
    }
}

interface AdjustmentService : Logging {
    suspend fun adjust()
    suspend fun status(): Int
    suspend fun reset()
}
