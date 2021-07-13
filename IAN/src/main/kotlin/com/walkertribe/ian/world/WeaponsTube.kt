package com.walkertribe.ian.world

import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.enums.TubeState

class WeaponsTube(timestamp: Long) {
    val state = Property.ObjectProperty<TubeState>(timestamp)
    val lastContents = Property.ObjectProperty<OrdnanceType>(timestamp)

    var contents: OrdnanceType?
        get() = lastContents.value?.takeIf {
            state.value == TubeState.LOADED || state.value == TubeState.LOADING
        }
        set(ordnanceType) { lastContents.value = ordnanceType }

    val hasData: Boolean get() = state.hasValue || lastContents.hasValue

    infix fun updates(tube: WeaponsTube) {
        state updates tube.state
        lastContents updates tube.lastContents
    }
}
