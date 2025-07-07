package artemis.agent.cpu

import artemis.agent.game.ObjectEntry
import artemis.agent.game.allies.AllyStatus
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.world.ArtemisNpc
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.clearMocks
import io.mockk.mockk
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

        val mockVesselData = mockk<VesselData>()

        afterSpec { clearMocks(mockVesselData) }

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
                                    response(long, null) shouldBeEqual expected
                                }
                            }
                        }
                    }
                }
            }

            describe("Invoke on ally") {
                val ally = ObjectEntry.Ally(ArtemisNpc(0, 0L), mockVesselData, false)

                val flyingBlindPartition =
                    testData.partition { it.status == AllyStatus.FLYING_BLIND }
                val invokeTestData = flyingBlindPartition.run { second + first + second }

                invokeTestData.forEachIndexed { index, (response, status, messages) ->
                    val expectedStatus =
                        if (index >= testData.size) AllyStatus.FLYING_BLIND else status

                    describe(response.name) {
                        messages.forEach { message ->
                            val (long, short) = message
                            it(short) {
                                response(long, ally)
                                ally.status shouldBeEqual expectedStatus
                                ally.hasEnergy.shouldBeEqual(index > 0)
                            }
                        }

                        messages.forEach { message ->
                            val (enLong, enShort) = message.hasEnergy()
                            it(enShort) {
                                ally.hasEnergy = false
                                response(enLong, ally)
                                ally.status shouldBeEqual expectedStatus
                                ally.hasEnergy.shouldBeTrue()
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
