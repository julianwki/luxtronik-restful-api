package com.wiersbitzki.luxtronik

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class Navigation(
    @XmlId val id: String,
    @XmlElement val data: List<NavigationItem>
) {
    fun readNavigationId(name: String): String {
        for (item in data) {
            val itemWithName = item.findNavigationItemWithName(name)
            if (itemWithName != null) {
                return itemWithName.id
            }
        }
        throw IllegalArgumentException("Unable to find item with name $name")
    }
}

@Serializable
@XmlSerialName("item")
data class NavigationItem(
    @XmlId val id: String,
    @XmlElement val name: String,
    @XmlElement val readOnly: Boolean?,
    @XmlElement val value: String?,
    @XmlElement val data: List<NavigationItem>
) {
    internal fun findNavigationItemWithName(name: String): NavigationItem? {
        if (name == this.name) {
            return this
        }
        for (item in data) {
            val itemWithName = item.findNavigationItemWithName(name)
            if (itemWithName != null) {
                return itemWithName
            }
        }
        return null
    }
}

@Serializable
data class Content(
    @XmlElement val name: String,
    @XmlElement val data: List<ContentItem>
) {
    fun Content.readValue(name: String, suffix: String = "") =
        findDataItemWithName(name)
            .value
            .let { if (suffix != "") it.substringBefore(suffix) else it }

    private fun findDataItemWithName(name: String) =
        data.firstOrNull { it.name == name }
            ?: throw IllegalArgumentException("Unable to find item with name $name")
}

@Serializable
@XmlSerialName("item")
data class ContentItem(
    @XmlId val id: String,
    @XmlElement val name: String,
    @XmlElement val value: String,
)