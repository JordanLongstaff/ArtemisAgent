package com.walkertribe.ian.vesseldata

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.string
import io.kotest.property.forAll

class XmlUtilTest :
    DescribeSpec({
        it("Missing attribute message") {
            val arbString = Arb.string(codepoints = Codepoint.alphanumeric())

            forAll(arbString, arbString, arbString) { type, attr, tag ->
                missingAttribute(tag, attr, type) ==
                    "$type attribute $attr is required in $tag element, but was not found"
            }
        }
    })
