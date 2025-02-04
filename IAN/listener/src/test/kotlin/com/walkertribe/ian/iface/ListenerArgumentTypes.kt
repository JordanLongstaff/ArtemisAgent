package com.walkertribe.ian.iface

open class ArgumentTypeA : ListenerArgument {
    override val timestamp: Long = 0L

    override fun offerTo(module: ListenerModule) {
        module.onPacket(this)
    }
}

class ArgumentTypeB : ArgumentTypeA()

class ArgumentTypeC : ListenerArgument {
    override val timestamp: Long = 0L

    override fun offerTo(module: ListenerModule) {
        module.onPacket(this)
    }
}
