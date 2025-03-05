package com.walkertribe.ian.protocol.udp

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class ServerTest :
    DescribeSpec({
        describe("Server") {
            it("Constructor") {
                checkAll(Arb.string(), Arb.string()) { ip, hostName ->
                    val server = Server(ip, hostName)
                    server.ip shouldBeEqual ip
                    server.hostName shouldBeEqual hostName
                }
            }
        }
    })
