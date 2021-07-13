package artemis.agent.setup.settings

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.viewModelScope
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.SoundEffect
import artemis.agent.UserSettingsKt
import artemis.agent.UserSettingsOuterClass.UserSettings
import artemis.agent.UserSettingsSerializer
import artemis.agent.UserSettingsSerializer.defaultValue
import artemis.agent.UserSettingsSerializer.userSettings
import artemis.agent.collectLatestWhileStarted
import artemis.agent.copy
import artemis.agent.databinding.SettingsFragmentBinding
import artemis.agent.databinding.fragmentViewBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.settings_fragment) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SettingsFragmentBinding by fragmentViewBinding()

    enum class Page(
        @StringRes val titleRes: Int,
        val pageClass: Class<out Fragment>,
        val onToggle: (UserSettingsKt.Dsl.(Boolean) -> Unit)? = null
    ) {
        CLIENT(
            R.string.settings_menu_client,
            ClientSettingsFragment::class.java,
        ) {
            override fun reset(settings: UserSettings): UserSettings = settings.copy {
                vesselDataLocation = UserSettings.VesselDataLocation.VESSEL_DATA_LOCATION_DEFAULT
                serverPort = UserSettingsSerializer.DEFAULT_SERVER_PORT
                recentAddressLimit = UserSettingsSerializer.DEFAULT_ADDRESS_LIMIT
                recentAddressLimitEnabled = false
                updateInterval = UserSettingsSerializer.DEFAULT_UPDATE_INTERVAL
            }
        },
        CONNECTION(
            R.string.settings_menu_connection,
            ConnectionSettingsFragment::class.java,
        ) {
            override fun reset(settings: UserSettings): UserSettings = settings.copy {
                connectionTimeoutSeconds = UserSettingsSerializer.DEFAULT_CONNECTION_TIMEOUT
                serverTimeoutSeconds = UserSettingsSerializer.DEFAULT_HEARTBEAT_TIMEOUT
                scanTimeoutSeconds = UserSettingsSerializer.DEFAULT_SCAN_TIMEOUT
            }
        },
        MISSION(
            R.string.settings_menu_missions,
            MissionSettingsFragment::class.java,
            onToggle = { isChecked -> missionsEnabled = isChecked },
        ) {
            override fun reset(settings: UserSettings): UserSettings = settings.copy {
                displayRewardBattery = true
                displayRewardCoolant = true
                displayRewardNukes = true
                displayRewardProduction = true
                displayRewardShield = true

                completedMissionDismissalEnabled = true
                completedMissionDismissalSeconds =
                    UserSettingsSerializer.DEFAULT_AUTO_DISMISSAL_SECONDS
            }
        },
        ALLIES(
            R.string.settings_menu_allies,
            AllySettingsFragment::class.java,
            onToggle = { isChecked -> alliesEnabled = isChecked },
        ) {
            override fun reset(settings: UserSettings): UserSettings = settings.copy {
                allySortClassFirst = false
                allySortStatus = false
                allySortClassSecond = false
                allySortName = false
                allySortEnergyFirst = false

                allyCommandManualReturn = false
                showDestroyedAllies = true
            }
        },
        ENEMIES(
            R.string.settings_menu_enemies,
            EnemySettingsFragment::class.java,
            onToggle = { isChecked -> enemiesEnabled = isChecked },
        ) {
            override fun reset(settings: UserSettings): UserSettings = settings.copy {
                enemySortSurrendered = false
                enemySortFaction = false
                enemySortFactionReversed = false
                enemySortName = false
                enemySortDistance = false

                surrenderRange = UserSettingsSerializer.DEFAULT_SURRENDER_RANGE
                surrenderRangeEnabled = true

                showEnemyIntel = true
                showTauntStatuses = true
                disableIneffectiveTaunts = true
            }
        },
        BIOMECHS(
            R.string.settings_menu_biomechs,
            BiomechSettingsFragment::class.java,
            onToggle = { isChecked -> biomechsEnabled = isChecked },
        ) {
            override fun reset(settings: UserSettings): UserSettings = settings.copy {
                biomechSortClassFirst = false
                biomechSortStatus = false
                biomechSortClassSecond = false
                biomechSortName = false

                freezeDurationSeconds = UserSettingsSerializer.DEFAULT_FREEZE_DURATION
            }
        },
        ROUTING(
            R.string.settings_menu_routing,
            RoutingSettingsFragment::class.java,
            onToggle = { isChecked -> routingEnabled = isChecked },
        ) {
            override fun reset(settings: UserSettings): UserSettings = settings.copy {
                routeMissions = true
                routeNeedsDamcon = true
                routeNeedsEnergy = true
                routeHasEnergy = true
                routeMalfunction = true
                routeAmbassador = true
                routeHostage = true
                routeCommandeered = true

                avoidBlackHoles = true
                avoidMines = true
                avoidTyphon = true

                blackHoleClearance = UserSettingsSerializer.DEFAULT_BLACK_HOLE_CLEARANCE
                mineClearance = UserSettingsSerializer.DEFAULT_MINE_CLEARANCE
                typhonClearance = UserSettingsSerializer.DEFAULT_TYPHON_CLEARANCE
            }
        },
        PERSONAL(
            R.string.settings_menu_personal,
            PersonalSettingsFragment::class.java,
        ) {
            override fun reset(settings: UserSettings): UserSettings = settings.copy {
                theme = UserSettings.Theme.THEME_DEFAULT
                threeDigitDirections = true
                soundVolume = UserSettingsSerializer.DEFAULT_SOUND_VOLUME
            }
        };

        abstract fun reset(settings: UserSettings): UserSettings
    }

    private var currentPage: Page? = null
        set(page) {
            field = page

            val fragmentClass = if (page == null) {
                binding.settingsPageTitle.setText(R.string.settings)
                binding.settingsOnOff.visibility = View.INVISIBLE
                binding.settingsBack.visibility = View.GONE

                binding.settingsReset.setOnClickListener {
                    viewModel.viewModelScope.launch {
                        binding.root.context.userSettings.updateData {
                            defaultValue.copy {
                                recentServers.clear()
                                recentServers += it.recentServersList
                            }
                        }
                        viewModel.playSound(SoundEffect.BEEP_2)
                    }
                }

                SettingsMenuFragment::class.java
            } else {
                binding.settingsPageTitle.setText(page.titleRes)
                binding.settingsBack.visibility = View.VISIBLE
                binding.settingsOnOff.visibility = page.onToggle?.let {
                    binding.settingsOnOff.isChecked = true

                    View.VISIBLE
                } ?: View.INVISIBLE

                binding.settingsReset.setOnClickListener {
                    viewModel.settingsReset.apply { value = !value }
                    viewModel.viewModelScope.launch {
                        binding.root.context.userSettings.updateData(page::reset)
                        viewModel.playSound(SoundEffect.BEEP_2)
                    }
                }

                page.pageClass
            }

            childFragmentManager.commit {
                replace(R.id.settingsFragmentContainer, fragmentClass, null)
                addToBackStack(null)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val onBackPressedCallback = requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner
        ) {
            goBackToMenu()
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.settingsPage) {
            onBackPressedCallback.isEnabled = it != null
            currentPage = it
        }

        binding.settingsBack.setOnClickListener {
            goBackToMenu()
        }

        binding.settingsPageTitle.setOnClickListener {
            if (currentPage != null) {
                goBackToMenu()
            }
        }

        binding.settingsOnOff.setOnCheckedChangeListener { _, isChecked ->
            currentPage?.onToggle?.also { onToggle ->
                viewModel.viewModelScope.launch {
                    view.context.userSettings.updateData {
                        it.copy { onToggle(isChecked) }
                    }
                }
            }
            if (!isChecked) {
                goBackToMenu()
            }
        }
    }

    private fun goBackToMenu() {
        viewModel.playSound(SoundEffect.BEEP_2)
        viewModel.settingsPage.value = null
    }
}
