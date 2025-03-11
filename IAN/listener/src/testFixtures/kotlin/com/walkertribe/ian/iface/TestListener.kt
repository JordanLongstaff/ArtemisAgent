package com.walkertribe.ian.iface

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

object TestListener {
    private val listeners by lazy {
        listOf(ListenerFunction(ListenerArgument::class, this::listen))
    }

    val module: ListenerModule by lazy { TestListenerModule(listeners) }

    private val callsMap =
        mutableMapOf<KClass<out ListenerArgument>, MutableList<ListenerArgument>>()

    @Listener
    fun listen(arg: ListenerArgument) {
        callsMap.putIfAbsent(arg::class, mutableListOf())
        callsMap[arg::class]?.add(arg)
    }

    fun <T : ListenerArgument> calls(kClass: KClass<T>): List<T> =
        callsMap
            .filterKeys { it.isSubclassOf(kClass) }
            .flatMap { it.value }
            .filterIsInstance(kClass.java)

    inline fun <reified T : ListenerArgument> calls(): List<T> = calls(T::class)

    fun clear() {
        callsMap.clear()
    }
}
