package com.walkertribe.ian.iface

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

class ListenerFunctionTest :
    DescribeSpec({
        var calledA = false
        var calledB = false
        var calledC = false

        val functionA = ListenerFunction(ArgumentTypeA::class) { calledA = true }
        val functionB = ListenerFunction(ArgumentTypeB::class) { calledB = true }
        val functionC = ListenerFunction(ArgumentTypeC::class) { calledC = true }

        describe("ListenerFunction") {
            it("Holds argument class object") {
                functionA.argumentClass shouldBeEqual ArgumentTypeA::class
                functionB.argumentClass shouldBeEqual ArgumentTypeB::class
                functionC.argumentClass shouldBeEqual ArgumentTypeC::class
            }

            it("Accepting check casts correctly") {
                functionA.checkIfAccepting(ArgumentTypeA::class).shouldNotBeNull() shouldBeEqual
                    functionA
                functionB.checkIfAccepting(ArgumentTypeA::class).shouldNotBeNull() shouldBeEqual
                    functionB
                functionB.checkIfAccepting(ArgumentTypeB::class).shouldNotBeNull() shouldBeEqual
                    functionB
                functionC.checkIfAccepting(ArgumentTypeC::class).shouldNotBeNull() shouldBeEqual
                    functionC
            }

            it("Accepting check returns null if cast is incorrect") {
                functionA.checkIfAccepting(ArgumentTypeB::class).shouldBeNull()
                functionA.checkIfAccepting(ArgumentTypeC::class).shouldBeNull()
                functionB.checkIfAccepting(ArgumentTypeC::class).shouldBeNull()
                functionC.checkIfAccepting(ArgumentTypeA::class).shouldBeNull()
                functionC.checkIfAccepting(ArgumentTypeB::class).shouldBeNull()
            }

            it("Accepts offer of valid argument") {
                functionA.offer(ArgumentTypeA())
                calledA.shouldBeTrue()

                calledA = false
                functionA.offer(ArgumentTypeB())
                calledA.shouldBeTrue()

                functionB.offer(ArgumentTypeB())
                calledB.shouldBeTrue()

                functionC.offer(ArgumentTypeC())
                calledC.shouldBeTrue()
            }

            it("Rejects offer of invalid argument") {
                calledA = false
                functionA.offer(ArgumentTypeC())
                calledA.shouldBeFalse()

                calledB = false
                functionB.offer(ArgumentTypeA())
                functionB.offer(ArgumentTypeC())
                calledB.shouldBeFalse()

                calledC = false
                functionC.offer(ArgumentTypeA())
                functionC.offer(ArgumentTypeB())
                calledC.shouldBeFalse()
            }
        }
    })
