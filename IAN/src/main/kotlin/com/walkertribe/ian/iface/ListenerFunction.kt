package com.walkertribe.ian.iface

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName

class ListenerFunction internal constructor(
    private val owner: Any,
    private val function: KFunction<*>
) {
    private val parameterType: KClass<*>

    internal fun accepts(kClass: KClass<out ListenerArgument>): Boolean =
        kClass.isSubclassOf(parameterType)

    internal fun <A : ListenerArgument> offer(arg: A) {
        if (accepts(arg::class)) {
            function.call(owner, arg)
        }
    }

    init {
        val decClass = owner::class
        require(decClass.visibility == KVisibility.PUBLIC) {
            "Class ${decClass.jvmName} must be public to have listener functions"
        }

        require(function.visibility == KVisibility.PUBLIC) {
            "Function ${function.name} must be public to be a listener"
        }

        require(function.returnType.jvmErasure == Unit::class) {
            "Function ${function.name} must return Unit to be a listener"
        }

        val paramTypes = function.valueParameters

        require(paramTypes.size == 1) {
            "Function ${function.name} must have exactly one argument"
        }

        val firstParam = paramTypes[0].type.jvmErasure
        require(firstParam.isSubclassOf(ListenerArgument::class)) {
            "Function ${function.name} must have ArtemisPacket, ArtemisObject or ConnectionEvent as its argument"
        }

        parameterType = firstParam
    }
}
