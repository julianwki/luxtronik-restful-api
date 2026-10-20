package com.wiersbitzki.luxtronik

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

@Serializable
data class Forecast(
    val latitude: Double,
    val longitude: Double,
    @SerialName("generationtime_ms")
    val generationtimeMs: Double,
    @SerialName("utc_offset_seconds")
    val utcOffsetSeconds: Int,
    val timezone: String,
    @SerialName("timezone_abbreviation")
    val timezoneAbbreviation: String,
    val elevation: Double,
    @SerialName("current_units")
    val currentUnits: CurrentUnits,
    val current: Current,
)

@Serializable
data class CurrentUnits(
    val time: String,
    val interval: String,
    @SerialName("temperature_2m")
    val temperature2m: String,
)

@Serializable
data class Current(
    val time: String,
    val interval: Int,
    @SerialName("temperature_2m")
    val temperature2m: Double,
)