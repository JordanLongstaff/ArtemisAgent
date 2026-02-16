package artemis.agent.setup.settings

import android.widget.ToggleButton
import artemis.agent.UserSettingsKt
import artemis.agent.UserSettingsOuterClass.UserSettings
import artemis.agent.databinding.SettingsEnemiesBinding

enum class EnemySettingsToggle {
    SORT_SURRENDER {
        override fun getButton(binding: SettingsEnemiesBinding): ToggleButton =
            binding.enemySortingSurrenderButton

        override fun isChecked(settings: UserSettings): Boolean = settings.enemySortSurrendered

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.enemySortSurrendered = isChecked
        }
    },
    SORT_RACE {
        override fun getButton(binding: SettingsEnemiesBinding): ToggleButton =
            binding.enemySortingRaceButton

        override fun isChecked(settings: UserSettings): Boolean = settings.enemySortFaction

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.enemySortFaction = isChecked
        }
    },
    SORT_NAME {
        override fun getButton(binding: SettingsEnemiesBinding): ToggleButton =
            binding.enemySortingNameButton

        override fun isChecked(settings: UserSettings): Boolean = settings.enemySortName

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.enemySortName = isChecked
            if (isChecked && settings.enemySortDistance) settings.enemySortDistance = false
        }
    },
    SORT_RANGE {
        override fun getButton(binding: SettingsEnemiesBinding): ToggleButton =
            binding.enemySortingRangeButton

        override fun isChecked(settings: UserSettings): Boolean = settings.enemySortDistance

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.enemySortDistance = isChecked
            if (isChecked && settings.enemySortName) settings.enemySortName = false
        }
    },
    TOGGLE_REVERSE_RACE {
        override fun getButton(binding: SettingsEnemiesBinding): ToggleButton =
            binding.reverseRaceSortButton

        override fun isChecked(settings: UserSettings): Boolean = settings.enemySortFactionReversed

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.enemySortFactionReversed = isChecked
        }
    },
    TOGGLE_INTEL {
        override fun getButton(binding: SettingsEnemiesBinding): ToggleButton =
            binding.showIntelButton

        override fun isChecked(settings: UserSettings): Boolean = settings.showEnemyIntel

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.showEnemyIntel = isChecked
        }
    },
    TOGGLE_TAUNT_STATUSES {
        override fun getButton(binding: SettingsEnemiesBinding): ToggleButton =
            binding.showTauntStatusButton

        override fun isChecked(settings: UserSettings): Boolean = settings.showTauntStatuses

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.showTauntStatuses = isChecked
        }
    },
    TOGGLE_DISABLE_INEFFECTIVE {
        override fun getButton(binding: SettingsEnemiesBinding): ToggleButton =
            binding.disableIneffectiveButton

        override fun isChecked(settings: UserSettings): Boolean = settings.disableIneffectiveTaunts

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.disableIneffectiveTaunts = isChecked
        }
    },
    TOGGLE_SURRENDER_RANGE {
        override fun getButton(binding: SettingsEnemiesBinding): ToggleButton =
            binding.surrenderRangeEnableButton

        override fun isChecked(settings: UserSettings): Boolean = settings.surrenderRangeEnabled

        override fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean) {
            settings.surrenderRangeEnabled = isChecked
        }
    };

    val isSort: Boolean by lazy { this < TOGGLE_REVERSE_RACE }

    abstract fun getButton(binding: SettingsEnemiesBinding): ToggleButton

    abstract fun isChecked(settings: UserSettings): Boolean

    abstract fun onCheckedChanged(settings: UserSettingsKt.Dsl, isChecked: Boolean)

    companion object {
        val sortEntries by lazy { entries.takeWhile { it.isSort } }
    }
}
