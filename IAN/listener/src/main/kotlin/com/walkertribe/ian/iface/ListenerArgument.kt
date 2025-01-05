package com.walkertribe.ian.iface

interface ListenerArgument {
    /** The timestamp of this object's creation. */
    val timestamp: Long

    fun offerTo(module: ListenerModule)
}
