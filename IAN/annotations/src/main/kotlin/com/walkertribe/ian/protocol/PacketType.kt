package com.walkertribe.ian.protocol

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class PacketType(
    /** The packet type, given as a string, which is then JamCRC hashed. */
    val type: String
)
