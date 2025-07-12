package com.wiersbitzki.luxtronik

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class Navigation(
    @XmlId val id: String,
    @XmlElement val data: List<NavigationItem>
) : Logging {
    fun readNavigationId(vararg path: String): String {
        logger.debug(this.toString())
        for (item in data) {
            val first = path[0]
            val rest = path.drop(1)
            if (item.name.trim() == first) {
                return if (rest.isEmpty()) {
                    item.id
                } else {
                    item.findNavigationItemWithName(rest)?.id ?: break
                }
            }
        }
        throw IllegalArgumentException("Unable to find item with path ${path.joinToString("/")}")
    }
}

@Serializable
@XmlSerialName("item")
data class NavigationItem(
    @XmlId val id: String,
    @XmlElement val name: String,
    @XmlElement val readOnly: Boolean? = null,
    @XmlElement val value: String? = null,
    @XmlElement val data: List<NavigationItem> = emptyList()
) {
    internal fun findNavigationItemWithName(path: List<String>): NavigationItem? {
        for (item in data) {
            val first = path[0]
            val rest = path.drop(1)
            if (item.name.trim() == first) {
                return if (rest.isEmpty()) {
                    item
                } else {
                    item.findNavigationItemWithName(rest)
                }
            }
        }
        return null
    }
}

@Serializable
data class Content(
    @XmlElement val name: String,
    @XmlElement val data: List<ContentItem>
) : Logging {
    fun readValue(name: String, suffix: String = "") =
        data.first { it.name.trim() == name }
            .value
            .let { if (suffix.isNotEmpty()) it.substringBefore(suffix) else it }
            .trim()
            .also { logger.info("{}: {}", name, it) }
            .also { logger.debug(this.toString()) }
}

@Serializable
@XmlSerialName("item")
data class ContentItem(
    @XmlId val id: String,
    @XmlElement val name: String,
    @XmlElement val value: String,
)