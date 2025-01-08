package artemis.agent.cpu

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.equals.shouldBeEqual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

@OptIn(ExperimentalSerializationApi::class)
class HailResponseEffectTest :
    DescribeSpec({
        val (testData, ignored) =
            HailResponseEffect::class.java.getResourceAsStream("hail-responses.json")!!.use {
                Json.decodeFromStream<HailResponseEffectTestData>(it)
            }

        val testDataMap = testData.groupBy { it.response } - HailResponseEffect.OTHER

        val allMessages = testData.flatMap { it.messages }

        describe("HailResponseEffect") {
            describe("Applies to message") {
                testData.forEach { (response) ->
                    val data = testDataMap[response] ?: return@forEach
                    describe(response.name) {
                        allMessages.forEach { message ->
                            val expected = data.any { it.messages.contains(message) }
                            listOf(message, message.hasEnergy()).forEach { (long, short) ->
                                it("$short: $expected") {
                                    response.appliesTo(long) shouldBeEqual expected
                                }
                            }
                        }
                    }
                }
            }

            describe("Ignored messages") {
                ignored
                    .flatMap { listOf(it, it.hasEnergy()) }
                    .forEach { (long, short) ->
                        describe(short) {
                            HailResponseEffect.entries.forEach { response ->
                                it(response.name) { response.appliesTo(long).shouldBeFalse() }
                            }
                        }
                    }
            }

            describe("Ally status") {
                testData.forEach { (response, status, messages) ->
                    describe(status.name) {
                        messages.forEach { (long, short) ->
                            it(short) { response.getAllyStatus(long) shouldBeEqual status }
                        }
                    }
                }
            }
        }
    })
