package com.walkertribe.ian.vesseldata

import korlibs.io.serialization.xml.Xml

data class Taunt internal constructor(val immunity: String, val text: String) {
    internal constructor(xml: Xml) : this(
        immunity = xml.requiredString("immunity"),
        text = xml.requiredString("text"),
    )

    companion object {
        const val COUNT = 3
    }
}
