package com.walkertribe.ian.protocol.udp

/**
 * IPv4 private network types. Used to determine whether an address belongs to a private network.
 *
 * @author rjwut
 */
enum class PrivateNetworkType {
    TWENTY_FOUR_BIT_BLOCK {
        // 10.x.x.x
        override val broadcastAddress: String
            get() = TWENTY_FOUR_BIT_BROADCAST

        override val constraints: Array<IntConstraint>
            get() = TWENTY_FOUR_BIT_CONSTRAINTS
    },
    TWENTY_BIT_BLOCK {
        // 172.16.x.x - 172.31.x.x
        override val broadcastAddress: String
            get() = TWENTY_BIT_BROADCAST

        override val constraints: Array<IntConstraint>
            get() = TWENTY_BIT_CONSTRAINTS
    },
    SIXTEEN_BIT_BLOCK {
        // 192.168.x.x
        override val broadcastAddress: String
            get() = SIXTEEN_BIT_BROADCAST

        override val constraints: Array<IntConstraint>
            get() = SIXTEEN_BIT_CONSTRAINTS
    };

    abstract val broadcastAddress: String
    internal abstract val constraints: Array<IntConstraint>

    internal fun match(address: String): Boolean =
        address.split('.').run {
            size == Int.SIZE_BYTES &&
                zip(constraints).all { (byte, cons) -> cons.check(byte.toInt()) }
        }

    companion object {
        private const val TWENTY_FOUR_BIT_BROADCAST = "10.255.255.255"
        private const val TWENTY_BIT_BROADCAST = "172.31.255.255"
        private const val SIXTEEN_BIT_BROADCAST = "192.168.255.255"

        private val TWENTY_FOUR_BIT_CONSTRAINTS = arrayOf<IntConstraint>(IntConstraint.Equals(10))

        private val TWENTY_BIT_CONSTRAINTS =
            arrayOf(IntConstraint.Equals(172), IntConstraint.Range(16..31))

        private val SIXTEEN_BIT_CONSTRAINTS =
            arrayOf<IntConstraint>(IntConstraint.Equals(192), IntConstraint.Equals(168))

        /**
         * Returns the private network address type that matches the given address, or null if it's
         * not a private network address.
         */
        fun of(address: String): PrivateNetworkType? = entries.find { it.match(address) }
    }
}
