package artemis.agent.setup.settings

import android.widget.ToggleButton
import artemis.agent.UserSettingsKt
import artemis.agent.UserSettingsOuterClass.UserSettings
import artemis.agent.databinding.SettingsMissionsBinding

enum class DisplayRewardSetting {
    BATTERY {
        override fun getButton(binding: SettingsMissionsBinding): ToggleButton =
            binding.rewardsBatteryButton

        override fun isChecked(settings: UserSettings): Boolean = settings.displayRewardBattery

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.displayRewardBattery = isChecked
        }
    },
    COOLANT {
        override fun getButton(binding: SettingsMissionsBinding): ToggleButton =
            binding.rewardsCoolantButton

        override fun isChecked(settings: UserSettings): Boolean = settings.displayRewardCoolant

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.displayRewardCoolant = isChecked
        }
    },
    NUKES {
        override fun getButton(binding: SettingsMissionsBinding): ToggleButton =
            binding.rewardsNukeButton

        override fun isChecked(settings: UserSettings): Boolean = settings.displayRewardNukes

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.displayRewardNukes = isChecked
        }
    },
    PRODUCTION {
        override fun getButton(binding: SettingsMissionsBinding): ToggleButton =
            binding.rewardsProductionButton

        override fun isChecked(settings: UserSettings): Boolean = settings.displayRewardProduction

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.displayRewardProduction = isChecked
        }
    },
    SHIELD {
        override fun getButton(binding: SettingsMissionsBinding): ToggleButton =
            binding.rewardsShieldButton

        override fun isChecked(settings: UserSettings): Boolean = settings.displayRewardShield

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.displayRewardShield = isChecked
        }
    };

    abstract fun getButton(binding: SettingsMissionsBinding): ToggleButton

    abstract fun isChecked(settings: UserSettings): Boolean

    abstract fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean)
}
