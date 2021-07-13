package com.walkertribe.ian.protocol.udp

/**
 * IPv4 private network types. Used to determine whether an address belongs
 * to a private network.
 * @author rjwut
 */
enum class PrivateNetworkType {
    TWENTY_FOUR_BIT_BLOCK {
        // 10.x.x.x
        override fun match(address: ByteArray): Boolean =
            address.size == Int.SIZE_BYTES && address[0].toInt() == 10
    },
    TWENTY_BIT_BLOCK {
        // 172.16.x.x - 172.31.x.x
        override fun match(address: ByteArray): Boolean =
            address.size == Int.SIZE_BYTES && address[0].toInt() == -44 && address[1] in 16..31
    },
    SIXTEEN_BIT_BLOCK {
        // 192.168.x.x
        override fun match(address: ByteArray): Boolean =
            address.size == Int.SIZE_BYTES && address[0].toInt() == -64 && address[1].toInt() == -88
    };

    /**
     * Returns true if the given address matches this private network type.
     */
    internal abstract fun match(address: ByteArray): Boolean

    companion object {
        /**
         * Returns the private network address type that matches the given
         * address, or null if it's not a private network address.
         */
        operator fun invoke(address: ByteArray): PrivateNetworkType? =
            entries.find { it.match(address) }
    }
}
