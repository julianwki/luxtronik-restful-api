package com.wiersbitzki.luxtronik

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class ModelTest : ShouldSpec({
    context("navigation") {
        val navigation = Navigation(
            id = "navId",
            items = listOf(
                NavigationItem(
                    id = "navItemId1",
                    name = "navItem1"
                ), NavigationItem(
                    id = "navItemId2",
                    name = "navItem2",
                    items = listOf(
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

        should("read navigation id") {
            navigation.readNavigationId("navItem1").shouldBe("navItemId1")
            navigation.readNavigationId("navItem2").shouldBe("navItemId2")
            navigation.readNavigationId("navItem2", "navItem2.1").shouldBe("navItemId2.1")
            navigation.readNavigationId("navItem2", "navItem2.2").shouldBe("navItemId2.2")
        }

        should("throw exception if not finding matching item") {
            shouldThrow<IllegalArgumentException> { navigation.readNavigationId("foo") }
                .message.shouldBe("Unable to find item with path foo")
            shouldThrow<IllegalArgumentException> { navigation.readNavigationId("navItem1", "foo") }
                .message.shouldBe("Unable to find item with path navItem1/foo")
        }
    }

    context("content") {
        val content = Content(
            name = "some content",
            items = listOf(
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

        should("read value") {
            content.readValue("contentItem1").shouldBe("123")
            content.readValue("contentItem2").shouldBe("456 kW")
            content.readValue("contentItem3").shouldBe("789 kWh")
        }
        should("read value without suffix") {
            content.readValue("contentItem2", "kW").shouldBe("456")
            content.readValue("contentItem3", "kWh").shouldBe("789")
        }
    }
})