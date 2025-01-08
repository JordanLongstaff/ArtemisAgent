package com.walkertribe.ian.enums

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class AudioModeTest :
    DescribeSpec({
        it("Playing") { AudioMode.Playing.shouldBeInstanceOf<AudioMode>() }

        describe("Incoming") {
            it("Constructor") {
                checkAll(Arb.string(), Arb.string()) { title, filename ->
                    val incomingAudio = AudioMode.Incoming(title, filename)
                    incomingAudio.title shouldBeEqual title
                    incomingAudio.filename shouldBeEqual filename
                }
            }
        }
    })
