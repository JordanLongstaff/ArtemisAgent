package com.walkertribe.ian.iface

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class ListenerRegistryTest :
    DescribeSpec({
        val mockModule =
            mockk<ListenerModule>(relaxUnitFun = true) {
                every { acceptedTypes } returns emptySet()
            }

        afterSpec {
            TestListener.clear()
            clearMocks(mockModule)
        }

        describe("ListenerRegistry") {
            lateinit var registry: ListenerRegistry

            it("Can create") { registry = ListenerRegistry() }

            it("Starts with no listener functions") {
                registry.listeningFor(ListenerArgument::class).shouldBeEmpty()
            }

            it("Registering an object with no listener functions does nothing") {
                registry.register(mockModule)
                registry.listeningFor(ListenerArgument::class).shouldBeEmpty()
            }

            it("Registering an object with a listener function registers that function") {
                registry.register(TestListener.module)
                registry.listeningFor(ListenerArgument::class).size shouldBeEqual 1
            }

            it("Can offer arguments") {
                registry.offer(ArgumentTypeA())
                TestListener.calls<ArgumentTypeA>().size shouldBeEqual 1
            }

            it("Can clear") {
                registry.clear()
                registry.listeningFor(ListenerArgument::class).shouldBeEmpty()
            }
        }
    })
