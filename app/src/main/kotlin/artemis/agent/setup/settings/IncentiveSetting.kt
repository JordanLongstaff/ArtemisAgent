package artemis.agent.setup.settings

import android.widget.ToggleButton
import artemis.agent.UserSettingsKt
import artemis.agent.UserSettingsOuterClass.UserSettings
import artemis.agent.databinding.SettingsRoutingBinding

enum class IncentiveSetting {
    MISSIONS {
        override fun getButton(binding: SettingsRoutingBinding): ToggleButton =
            binding.incentivesMissionsButton

        override fun isChecked(settings: UserSettings): Boolean = settings.routeMissions

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.routeMissions = isChecked
        }
    },
    NEEDS_DAMCON {
        override fun getButton(binding: SettingsRoutingBinding): ToggleButton =
            binding.incentivesNeedsDamConButton

        override fun isChecked(settings: UserSettings): Boolean = settings.routeNeedsDamcon

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.routeNeedsDamcon = isChecked
        }
    },
    NEEDS_ENERGY {
        override fun getButton(binding: SettingsRoutingBinding): ToggleButton =
            binding.incentivesNeedsEnergyButton

        override fun isChecked(settings: UserSettings): Boolean = settings.routeNeedsEnergy

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.routeNeedsEnergy = isChecked
        }
    },
    HAS_ENERGY {
        override fun getButton(binding: SettingsRoutingBinding): ToggleButton =
            binding.incentivesHasEnergyButton

        override fun isChecked(settings: UserSettings): Boolean = settings.routeHasEnergy

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.routeHasEnergy = isChecked
        }
    },
    MALFUNCTION {
        override fun getButton(binding: SettingsRoutingBinding): ToggleButton =
            binding.incentivesMalfunctionButton

        override fun isChecked(settings: UserSettings): Boolean = settings.routeMalfunction

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.routeMalfunction = isChecked
        }
    },
    AMBASSADOR {
        override fun getButton(binding: SettingsRoutingBinding): ToggleButton =
            binding.incentivesAmbassadorButton

        override fun isChecked(settings: UserSettings): Boolean = settings.routeAmbassador

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.routeAmbassador = isChecked
        }
    },
    HOSTAGE {
        override fun getButton(binding: SettingsRoutingBinding): ToggleButton =
            binding.incentivesHostageButton

        override fun isChecked(settings: UserSettings): Boolean = settings.routeHostage

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.routeHostage = isChecked
        }
    },
    COMMANDEERED {
        override fun getButton(binding: SettingsRoutingBinding): ToggleButton =
            binding.incentivesCommandeeredButton

        override fun isChecked(settings: UserSettings): Boolean = settings.routeCommandeered

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.routeCommandeered = isChecked
        }
    };

    abstract fun getButton(binding: SettingsRoutingBinding): ToggleButton

    abstract fun isChecked(settings: UserSettings): Boolean

    abstract fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean)
}
