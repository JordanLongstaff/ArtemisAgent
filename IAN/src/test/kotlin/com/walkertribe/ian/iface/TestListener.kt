package com.walkertribe.ian.iface

import io.kotest.datatest.WithDataTestName
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

object TestListener {
    private val callsMap =
        mutableMapOf<KClass<out ListenerArgument>, MutableList<ListenerArgument>>()

    @Listener
    fun listen(arg: ListenerArgument) {
        callsMap.putIfAbsent(arg::class, mutableListOf())
        callsMap[arg::class]?.add(arg)
    }

    fun <T : ListenerArgument> calls(kClass: KClass<T>): List<T> = callsMap.filterKeys {
        it.isSubclassOf(kClass)
    }.flatMap {
        it.value
    }.filterIsInstance(kClass.java)

    inline fun <reified T : ListenerArgument> calls(): List<T> = calls(T::class)

    val registry: ListenerRegistry by lazy {
        ListenerRegistry().apply { register(this@TestListener) }
    }

    fun clear() {
        callsMap.clear()
    }

    open class Invalid private constructor(private val testName: String) : WithDataTestName {
        data object NonPublicFunction : Invalid("Non-public function") {
            @Suppress("UnusedPrivateMember")
            @Listener
            private fun listen(t: ListenerArgument) = Unit
        }

        data object NonUnitFunction : Invalid("Non-Unit returning function") {
            @Suppress("FunctionOnlyReturningConstant")
            @Listener
            fun listen(t: ListenerArgument): Boolean = false
        }

        data object NoArguments : Invalid("Function with no arguments") {
            @Listener
            fun listen() = Unit
        }

        data object MultipleArguments : Invalid("Function with too many arguments") {
            @Listener
            fun listen(a: ListenerArgument, b: ListenerArgument) = Unit
        }

        data object ParameterType : Invalid("Function with invalid parameter") {
            @Listener
            fun listen(t: Int) = Unit
        }

        companion object {
            val NonPublicClass = object : Invalid("Instance of non-public class") {
                @Listener
                fun listen(t: ListenerArgument) = Unit
            }
        }

        override fun dataTestName(): String = testName
    }
}
