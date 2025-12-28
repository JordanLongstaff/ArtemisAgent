package com.walkertribe.ian.vesseldata

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import korlibs.io.serialization.xml.Xml

class VesselTest :
    DescribeSpec({
        describe("Vessel") {
            describe("XML") {
                describe("Success") {
                    TestVessel.arbitrary().checkAll { it.test(Vessel(it.serialize()), null) }
                }

                describe("Error") {
                    withData(
                        nameFn = { "Missing ${it.first}" },
                        Triple("uniqueID", "Integer", listOf("side" to "0", "classname" to "A")),
                        Triple("side", "Integer", listOf("uniqueID" to "0", "classname" to "A")),
                        Triple("classname", "String", listOf("uniqueID" to "0", "side" to "0")),
                    ) { (missing, type, included) ->
                        val attrs = included.joinToString(" ") { (key, value) -> "$key=\"$value\"" }
                        val exception =
                            shouldThrow<IllegalArgumentException> {
                                Vessel(Xml("<vessel $attrs></vessel>"))
                            }
                        exception.message.shouldNotBeNull() shouldBeEqual
                            missingAttribute("vessel", missing, type)
                    }

                    describe("Torpedo storage") {
                        withData(
                            nameFn = { "Missing ${it.first}" },
                            Triple("type", "String", "amount" to "0"),
                            Triple("amount", "Integer", "type" to "trp"),
                        ) { (missing, type, included) ->
                            val xml =
                                """
                            <vessel uniqueID="0" side="0" classname="A">
                                <torpedo_storage ${included.first}="${included.second}" />
                            </vessel>
                        """
                                    .trimIndent()
                            val exception =
                                shouldThrow<IllegalArgumentException> { Vessel(Xml(xml)) }
                            exception.message.shouldNotBeNull() shouldBeEqual
                                missingAttribute("torpedo_storage", missing, type)
                        }

                        it("Invalid code") {
                            Arb.string(size = 4, codepoints = Codepoint.alphanumeric()).checkAll {
                                code ->
                                val xml =
                                    """
                                <vessel uniqueID="0" side="0" classname="A">
                                    <torpedo_storage type="$code" amount="0" />
                                </vessel>
                            """
                                        .trimIndent()

                                val exception =
                                    shouldThrow<IllegalArgumentException> { Vessel(Xml(xml)) }
                                exception.message.shouldNotBeNull() shouldBeEqual
                                    "Invalid ordnance type code: $code"
                            }
                        }
                    }
                }
            }

            describe("Single-seat") {
                withData(
                    nameFn = { it.first },
                    Triple("True", "singleseat", true),
                    Triple("False", "", false),
                ) { (_, broadType, expectedValue) ->
                    val testVessel =
                        Vessel(
                            id = 0,
                            side = 0,
                            name = "",
                            broadType = broadType,
                            ordnanceStorage = emptyMap(),
                            productionCoefficient = 0f,
                            bayCount = 0,
                            description = null,
                            internalsFilePath = null,
                        )
                    testVessel.isSingleseat shouldBeEqual expectedValue
                }
            }
        }
    })
