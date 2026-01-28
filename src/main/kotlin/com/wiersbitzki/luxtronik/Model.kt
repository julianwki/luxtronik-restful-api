package com.wiersbitzki.luxtronik

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
            val rest = path.drop(1)
            if (item.name.trim() == path[0]) {
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
data class NavigationItem(
    val items: List<NavigationItem> = emptyList(),
    val id: String,
    val name: String,
    val readOnly: Boolean? = null,
) {
    internal fun findNavigationItemWithName(path: List<String>): NavigationItem? {
        for (item in items) {
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
}

@Serializable
data class ContentItem(
    val name: String,
    val value: String,
    val id: String,
)