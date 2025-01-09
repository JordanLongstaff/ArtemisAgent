package com.walkertribe.ian.vesseldata

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import korlibs.io.serialization.xml.Xml

class TauntTest :
    DescribeSpec({
        describe("Taunt") {
            describe("XML") {
                it("Success") {
                    checkAll(Arb.string(), Arb.string()) { immunity, text ->
                        val taunt = Taunt(Xml("taunt", "immunity" to immunity, "text" to text))
                        taunt.immunity shouldBeEqual immunity
                        taunt.text shouldBeEqual text
                    }
                }

                describe("Error") {
                    withData(
                        nameFn = { "Missing ${it.first}" },
                        "immunity" to "text",
                        "text" to "immunity",
                    ) { (missing, included) ->
                        val exception =
                            shouldThrow<IllegalArgumentException> {
                                Taunt(Xml("""<taunt $included="A" />"""))
                            }
                        exception.message.shouldNotBeNull() shouldBeEqual
                            missingAttribute("taunt", missing, "String")
                    }
                }
            }
        }
    })
