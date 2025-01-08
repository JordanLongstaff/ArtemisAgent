package artemis.agent.cpu

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class HailResponseEffectTest {
    @Test
    fun appliedMessagesTest() {
        testData.data.forEach { (response) ->
            allMessages.forEach { message ->
                val expected = testDataMap[response]?.any { it.messages.contains(message) } != false
                listOf(message, message + HAS_ENERGY).forEach { string ->
                    Assertions.assertEquals(expected, response.appliesTo(string))
                }
            }
        }
    }

    @Test
    fun ignoredMessagesTest() {
        testData.ignored.forEach { message ->
            val strings = listOf(message, message + HAS_ENERGY)
            HailResponseEffect.entries.forEach { response ->
                strings.forEach { string ->
                    Assertions.assertFalse(response.appliesTo(string), string)
                }
            }
        }
    }

    @Test
    fun allyStatusTest() {
        testData.data.forEach { (response, status, messages) ->
            messages.forEach { message ->
                Assertions.assertEquals(status, response.getAllyStatus(message))
            }
        }
    }

    private companion object {
        const val HAS_ENERGY = "  We also have energy to spare, if you need some."

        @OptIn(ExperimentalSerializationApi::class)
        val testData by lazy {
            HailResponseEffect::class.java.getResourceAsStream("hail-responses.json")!!.use {
                Json.decodeFromStream<HailResponseEffectTestData>(it)
            }
        }

        val testDataMap by lazy { testData.data.groupBy { it.response } - HailResponseEffect.OTHER }

        val allMessages by lazy { testData.data.flatMap { it.messages } }
    }
}
