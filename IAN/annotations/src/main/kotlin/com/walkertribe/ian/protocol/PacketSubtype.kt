package com.walkertribe.ian.protocol

/** Annotation for packet classes with a subtype. This annotation is not inherited. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class PacketSubtype(val subtype: Byte)
