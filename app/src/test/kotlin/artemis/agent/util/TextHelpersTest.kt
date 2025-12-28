package artemis.agent.util

import android.content.Context
import artemis.agent.R
import com.walkertribe.ian.world.Shields
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk

class TextHelpersTest :
    DescribeSpec({
        val systemLabels =
            listOf(
                "Hallway",
                "Beams",
                "Torpedoes",
                "Sensors",
                "Maneuvering",
                "Impulse",
                "Warp drive",
                "Jump drive",
                "Front shields",
                "Rear shields",
            )

        val mockContext =
            mockk<Context> {
                every { getString(R.string.front_shield, *varargAny { nArgs == 2 }) } answers
                    {
                        val args = lastArg<Array<Any?>>()
                        val current = args.first().toString().toFloatOrNull()?.toInt() ?: 0
                        val max = args.last().toString().toFloatOrNull()?.toInt() ?: 0
                        "F $current/$max"
                    }
                every { getString(R.string.rear_shield, *varargAny { nArgs == 2 }) } answers
                    {
                        val args = lastArg<Array<Any?>>()
                        val current = args.first().toString().toFloatOrNull()?.toInt() ?: 0
                        val max = args.last().toString().toFloatOrNull()?.toInt() ?: 0
                        "R $current/$max"
                    }

                every { getString(R.string.node_count, *varargAny { nArgs == 3 }) } answers
                    {
                        val args = lastArg<Array<Any?>>()
                        val system = args.first().toString()
                        val count = args.last().toString().toIntOrNull() ?: 0
                        val damaged = args[1].toString().toIntOrNull() ?: 0
                        "$system: $damaged/$count nodes"
                    }

                every { getString(R.string.percentage_paren, *varargAny { nArgs == 1 }) } answers
                    {
                        val percentage =
                            lastArg<Array<Any?>>().first().toString().toFloatOrNull()?.toInt()
                                ?: 100
                        "$percentage%"
                    }
            }

        describe("Shield text") {
            val shields = Shields(0L)

            withData(
                nameFn = { it.first },
                "Front" to R.string.front_shield,
                "Rear" to R.string.rear_shield,
            ) { (name, stringId) ->
                val startChar = name[0]
                checkAll(Arb.int(0..100), Arb.of(100, 200)) { current, max ->
                    shields.maxStrength.value = max.toFloat()
                    shields.strength.value = current.toFloat()

                    val percentage = current.toFloat() / max.toFloat() * 100f

                    val text = getShieldText(mockContext, stringId, shields)
                    text shouldBeEqual "$startChar $current/$max ${percentage.toInt()}%"
                }
            }
        }

        describe("Damage report") {
            withData(systemLabels) { systemLabel ->
                checkAll(Arb.int(0..100), Arb.int(0..100), Arb.int(0..100)) {
                    nodeCount,
                    damageCount,
                    percentage ->
                    val text =
                        getDamageReportText(
                            mockContext,
                            systemLabel,
                            nodeCount,
                            damageCount,
                            percentage / 100.0,
                        )
                    text shouldBeEqual "$systemLabel: $damageCount/$nodeCount nodes $percentage%"
                }
            }
        }
    })
