package artemis.agent.setup.settings

import android.widget.ToggleButton
import artemis.agent.UserSettingsKt
import artemis.agent.UserSettingsOuterClass.UserSettings
import artemis.agent.databinding.SettingsBiomechsBinding

enum class BiomechSortToggle {
    SORT_CLASS_FIRST {
        override fun getButton(binding: SettingsBiomechsBinding): ToggleButton =
            binding.biomechSortingClassButton1

        override fun isChecked(settings: UserSettings): Boolean = settings.biomechSortClassFirst

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.biomechSortClassFirst = isChecked
            if (isChecked && settings.biomechSortClassSecond)
                settings.biomechSortClassSecond = false
        }
    },
    SORT_STATUS {
        override fun getButton(binding: SettingsBiomechsBinding): ToggleButton =
            binding.biomechSortingStatusButton

        override fun isChecked(settings: UserSettings): Boolean = settings.biomechSortStatus

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.biomechSortStatus = isChecked
        }
    },
    SORT_CLASS_SECOND {
        override fun getButton(binding: SettingsBiomechsBinding): ToggleButton =
            binding.biomechSortingClassButton2

        override fun isChecked(settings: UserSettings): Boolean = settings.biomechSortClassSecond

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.biomechSortClassSecond = isChecked
            if (isChecked && settings.biomechSortClassFirst) settings.biomechSortClassFirst = false
        }
    },
    SORT_NAME {
        override fun getButton(binding: SettingsBiomechsBinding): ToggleButton =
            binding.biomechSortingNameButton

        override fun isChecked(settings: UserSettings): Boolean = settings.biomechSortName

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.biomechSortName = isChecked
        }
    };

    abstract fun getButton(binding: SettingsBiomechsBinding): ToggleButton

    abstract fun isChecked(settings: UserSettings): Boolean

    abstract fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean)
}
