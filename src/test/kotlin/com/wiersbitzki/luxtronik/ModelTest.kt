package com.wiersbitzki.luxtronik

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelTest {

    @Test
    fun testNavigationReadNavigationId() {
        val navigation = Navigation(
            id = "navId",
            data = listOf(
                NavigationItem(
                    id = "navItemId1",
                    name = "navItem1"
                ), NavigationItem(
                    id = "navItemId2",
                    name = "navItem2",
                    data = listOf(
                        NavigationItem(
                            id = "navItemId2.1",
                            name = "navItem2.1"
                        ), NavigationItem(
                            id = "navItemId2.2",
                            name = "navItem2.2"
                        )
                    )
                )
            )
        )

        assertEquals("navItemId1", navigation.readNavigationId("navItem1"))
        assertEquals("navItemId2", navigation.readNavigationId("navItem2"))
        assertEquals("navItemId2.1", navigation.readNavigationId("navItem2", "navItem2.1"))
        assertEquals("navItemId2.2", navigation.readNavigationId("navItem2", "navItem2.2"))

        assertEquals(
            "Unable to find item with path foo",
            assertThrows<IllegalArgumentException> {
                navigation.readNavigationId("foo")
            }.message
        )
        assertEquals(
            "Unable to find item with path navItem1/foo",
            assertThrows<IllegalArgumentException> {
                navigation.readNavigationId("navItem1", "foo")
            }.message
        )
    }

    @Test
    fun testContentReadValue() {
        val content = Content(
            name = "some content",
            data = listOf(
                ContentItem(
                    id = "contentItemId1",
                    name = "contentItem1",
                    value = "123"
                ),
                ContentItem(
                    id = "contentItemId2",
                    name = "contentItem2",
                    value = "456 kW"
                ),
                ContentItem(
                    id = "contentItemId3",
                    name = "contentItem3",
                    value = "789 kWh"
                )
            )
        )

        assertEquals("123", content.readValue("contentItem1"))
        assertEquals("456 kW", content.readValue("contentItem2"))
        assertEquals("456", content.readValue("contentItem2", "kW"))
        assertEquals("789 kWh", content.readValue("contentItem3"))
        assertEquals("789", content.readValue("contentItem3", "kWh"))
    }
}