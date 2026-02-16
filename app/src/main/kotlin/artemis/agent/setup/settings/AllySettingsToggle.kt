package artemis.agent.setup.settings

import android.widget.ToggleButton
import artemis.agent.UserSettingsKt
import artemis.agent.UserSettingsOuterClass.UserSettings
import artemis.agent.databinding.SettingsAlliesBinding

enum class AllySettingsToggle {
    SORT_CLASS_FIRST {
        override fun getButton(binding: SettingsAlliesBinding): ToggleButton =
            binding.allySortingClassButton1

        override fun isChecked(settings: UserSettings): Boolean = settings.allySortClassFirst

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.allySortClassFirst = isChecked
            if (isChecked && settings.allySortClassSecond) settings.allySortClassSecond = false
        }
    },
    SORT_STATUS {
        override fun getButton(binding: SettingsAlliesBinding): ToggleButton =
            binding.allySortingStatusButton

        override fun isChecked(settings: UserSettings): Boolean = settings.allySortStatus

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.allySortStatus = isChecked
            if (!isChecked && settings.allySortEnergyFirst) settings.allySortEnergyFirst = false
        }
    },
    SORT_CLASS_SECOND {
        override fun getButton(binding: SettingsAlliesBinding): ToggleButton =
            binding.allySortingClassButton2

        override fun isChecked(settings: UserSettings): Boolean = settings.allySortClassSecond

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.allySortClassSecond = isChecked
            if (isChecked && settings.allySortClassFirst) settings.allySortClassFirst = false
        }
    },
    SORT_NAME {
        override fun getButton(binding: SettingsAlliesBinding): ToggleButton =
            binding.allySortingNameButton

        override fun isChecked(settings: UserSettings): Boolean = settings.allySortName

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.allySortName = isChecked
        }
    },
    SORT_ENERGY_FIRST {
        override fun getButton(binding: SettingsAlliesBinding): ToggleButton =
            binding.allySortingEnergyButton

        override fun isChecked(settings: UserSettings): Boolean = settings.allySortEnergyFirst

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.allySortEnergyFirst = isChecked
            if (isChecked && !settings.allySortStatus) settings.allySortStatus = true
        }
    },
    TOGGLE_SHOW_DESTROYED {
        override fun getButton(binding: SettingsAlliesBinding): ToggleButton =
            binding.showDestroyedAlliesButton

        override fun isChecked(settings: UserSettings): Boolean = settings.showDestroyedAllies

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.showDestroyedAllies = isChecked
        }
    },
    TOGGLE_MANUAL_RETURN {
        override fun getButton(binding: SettingsAlliesBinding): ToggleButton =
            binding.manuallyReturnButton

        override fun isChecked(settings: UserSettings): Boolean = settings.allyCommandManualReturn

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.allyCommandManualReturn = isChecked
        }
    },
    TOGGLE_RECAPS {
        override fun getButton(binding: SettingsAlliesBinding): ToggleButton =
            binding.enableRecapsButton

        override fun isChecked(settings: UserSettings): Boolean = settings.allyRecapsEnabled

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.allyRecapsEnabled = isChecked
        }
    },
    TOGGLE_BACK_CANCEL {
        override fun getButton(binding: SettingsAlliesBinding): ToggleButton =
            binding.backButtonCancelButton

        override fun isChecked(settings: UserSettings): Boolean = settings.allyBackEnabled

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.allyBackEnabled = isChecked
        }
    };

    val isSort: Boolean by lazy { this < TOGGLE_SHOW_DESTROYED }

    abstract fun getButton(binding: SettingsAlliesBinding): ToggleButton

    abstract fun isChecked(settings: UserSettings): Boolean

    abstract fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean)

    companion object {
        val sortEntries by lazy { entries.takeWhile { it.isSort } }
    }
}
