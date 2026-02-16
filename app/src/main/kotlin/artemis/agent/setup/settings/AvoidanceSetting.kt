package artemis.agent.setup.settings

import android.widget.EditText
import android.widget.TextView
import android.widget.ToggleButton
import artemis.agent.UserSettingsKt
import artemis.agent.UserSettingsOuterClass.UserSettings
import artemis.agent.databinding.SettingsRoutingBinding

enum class AvoidanceSetting {
    BLACK_HOLES {
        override fun isEnabled(settings: UserSettings): Boolean = settings.avoidBlackHoles

        override fun setEnabled(settings: UserSettingsKt.Dsl, isEnabled: Boolean) {
            settings.avoidBlackHoles = isEnabled
        }

        override fun getClearance(settings: UserSettings): Float = settings.blackHoleClearance

        override fun setClearance(settings: UserSettingsKt.Dsl, clearance: Float) {
            settings.blackHoleClearance = clearance
        }

        override fun getToggleButton(binding: SettingsRoutingBinding): ToggleButton =
            binding.blackHolesButton

        override fun getClearanceField(binding: SettingsRoutingBinding): EditText =
            binding.blackHolesClearanceField

        override fun getKmLabel(binding: SettingsRoutingBinding): TextView =
            binding.blackHolesClearanceKm
    },
    MINES {
        override fun isEnabled(settings: UserSettings): Boolean = settings.avoidMines

        override fun setEnabled(settings: UserSettingsKt.Dsl, isEnabled: Boolean) {
            settings.avoidMines = isEnabled
        }

        override fun getClearance(settings: UserSettings): Float = settings.mineClearance

        override fun setClearance(settings: UserSettingsKt.Dsl, clearance: Float) {
            settings.mineClearance = clearance
        }

        override fun getToggleButton(binding: SettingsRoutingBinding): ToggleButton =
            binding.minesButton

        override fun getClearanceField(binding: SettingsRoutingBinding): EditText =
            binding.minesClearanceField

        override fun getKmLabel(binding: SettingsRoutingBinding): TextView =
            binding.minesClearanceKm
    },
    TYPHON {
        override fun isEnabled(settings: UserSettings): Boolean = settings.avoidTyphon

        override fun setEnabled(settings: UserSettingsKt.Dsl, isEnabled: Boolean) {
            settings.avoidTyphon = isEnabled
        }

        override fun getClearance(settings: UserSettings): Float = settings.typhonClearance

        override fun setClearance(settings: UserSettingsKt.Dsl, clearance: Float) {
            settings.typhonClearance = clearance
        }

        override fun getToggleButton(binding: SettingsRoutingBinding): ToggleButton =
            binding.typhonsButton

        override fun getClearanceField(binding: SettingsRoutingBinding): EditText =
            binding.typhonsClearanceField

        override fun getKmLabel(binding: SettingsRoutingBinding): TextView =
            binding.typhonsClearanceKm
    };

    abstract fun isEnabled(settings: UserSettings): Boolean

    abstract fun setEnabled(settings: UserSettingsKt.Dsl, isEnabled: Boolean)

    abstract fun getClearance(settings: UserSettings): Float

    abstract fun setClearance(settings: UserSettingsKt.Dsl, clearance: Float)

    abstract fun getToggleButton(binding: SettingsRoutingBinding): ToggleButton

    abstract fun getClearanceField(binding: SettingsRoutingBinding): EditText

    abstract fun getKmLabel(binding: SettingsRoutingBinding): TextView
}
