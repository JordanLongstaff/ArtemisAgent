package com.walkertribe.ian.vesseldata

import korlibs.io.serialization.xml.Xml

fun Xml.requiredInt(name: String): Int = required(name, "Integer", Xml::intNull)
fun Xml.requiredString(name: String): String = required(name, "String", Xml::strNull)

private fun <T> Xml.required(attr: String, type: String, getter: Xml.(String) -> T?): T =
    requireNotNull(getter(attr)) { missingAttribute(name, attr, type) }

internal fun missingAttribute(tag: String, attr: String, type: String): String =
    "$type attribute $attr is required in $tag element, but was not found"
