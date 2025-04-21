package artemis.agent.screens

import android.view.View
import androidx.annotation.StringRes
import artemis.agent.R
import artemis.agent.isDisplayedWithSize
import artemis.agent.isDisplayedWithText
import artemis.agent.setup.settings.AllySettingsFragment
import artemis.agent.setup.settings.BiomechSettingsFragment
import artemis.agent.setup.settings.ClientSettingsFragment
import artemis.agent.setup.settings.ConnectionSettingsFragment
import artemis.agent.setup.settings.EnemySettingsFragment
import artemis.agent.setup.settings.GroupedToggleButtonSetting
import artemis.agent.setup.settings.KTimeInputBinder
import artemis.agent.setup.settings.MissionSettingsFragment
import artemis.agent.setup.settings.PersonalSettingsFragment
import artemis.agent.setup.settings.RoutingSettingsFragment
import artemis.agent.setup.settings.SettingsFragment
import artemis.agent.setup.settings.SettingsMenuFragment
import artemis.agent.setup.settings.SingleToggleButtonSetting
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.check.KCheckBox
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.edit.KEditText
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.progress.KSeekBar
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.recycler.KRecyclerView
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher

object SettingsPageScreen : KScreen<SettingsPageScreen>() {
    override val layoutId: Int = R.layout.settings_fragment
    override val viewClass: Class<*> = SettingsFragment::class.java

    val settingsReset = KButton { withId(R.id.settingsReset) }
    val settingsPageTitle = KTextView { withId(R.id.settingsPageTitle) }
    val settingsBack = KImageView { withId(R.id.settingsBack) }
    val settingsOnOff = KCheckBox { withId(R.id.settingsOnOff) }

    private val pageTitles by lazy {
        intArrayOf(
            R.string.settings_menu_client,
            R.string.settings_menu_connection,
            R.string.settings_menu_missions,
            R.string.settings_menu_allies,
            R.string.settings_menu_enemies,
            R.string.settings_menu_biomechs,
            R.string.settings_menu_routing,
            R.string.settings_menu_personal,
        )
    }

    fun assertMainMenuDisplayed() {
        settingsPageTitle.isDisplayedWithText(R.string.settings)
        settingsBack.isNotDisplayed()
        Menu.settingsPageMenu {
            isDisplayedWithSize(this@SettingsPageScreen.pageTitles.size)
            this@SettingsPageScreen.pageTitles.forEachIndexed { index, title ->
                childAt<Menu.Entry>(index) { this.title.isDisplayedWithText(title) }
            }
        }
    }

    fun assertSubmenuDisplayed(index: Int) {
        settingsPageTitle.isDisplayedWithText(pageTitles[index])
        settingsBack.isDisplayed()
        Menu.settingsPageMenu.isNotDisplayed()
    }

    fun closeSubmenu(usingToggle: Boolean = false) {
        if (usingToggle) settingsOnOff.click() else settingsBack.click()
        assertMainMenuDisplayed()
    }

    fun deactivateSubmenu(index: Int) {
        Menu.settingsPageMenu.childAt<Menu.Entry>(index) { toggle.click() }
        assertMainMenuDisplayed()
    }

    fun backFromSubmenu() {
        pressBack()
        assertMainMenuDisplayed()
    }

    object Menu : KScreen<Menu>() {
        override val layoutId: Int = R.layout.settings_menu
        override val viewClass: Class<*> = SettingsMenuFragment::class.java

        val settingsPageMenu =
            KRecyclerView({ withId(R.id.settingsPageMenu) }, { itemType(::Entry) })

        fun testToggleState(index: Int, isOn: Boolean) {
            settingsPageMenu.childAt<Entry>(index) { if (isOn) isOn() else isOff() }
        }

        class Entry(parent: Matcher<View>) : KRecyclerItem<Entry>(parent) {
            val title = KTextView(parent) { withId(R.id.settingsEntryTitle) }
            val toggle = KCheckBox(parent) { withId(R.id.settingsEntryToggle) }

            fun isOn() {
                toggle {
                    isChecked()
                    hasText(R.string.on)
                }
            }

            fun isOff() {
                toggle {
                    isNotChecked()
                    hasText(R.string.off)
                }
            }
        }
    }

    object Client : KScreen<Client>() {
        override val layoutId: Int = R.layout.settings_client
        override val viewClass: Class<*> = ClientSettingsFragment::class.java

        val vesselDataTitle = KTextView { withId(R.id.vesselDataTitle) }
        val vesselDataDivider = KView { withId(R.id.vesselDataDivider) }
        val vesselDataDefaultButton = KCheckBox { withId(R.id.vesselDataDefault) }
        val vesselDataInternalButton = KCheckBox { withId(R.id.vesselDataInternalStorage) }
        val vesselDataExternalButton = KCheckBox { withId(R.id.vesselDataExternalStorage) }

        val showNetworkInfoTitle = KTextView { withId(R.id.showNetworkInfoTitle) }
        val showNetworkInfoButton = KCheckBox { withId(R.id.showNetworkInfoButton) }
        val showNetworkInfoDivider = KView { withId(R.id.showNetworkInfoDivider) }

        val serverPortTitle = KTextView { withId(R.id.serverPortTitle) }
        val serverPortField = KTextView { withId(R.id.serverPortField) }
        val serverPortDivider = KView { withId(R.id.serverPortDivider) }

        val addressLimitTitle = KTextView { withId(R.id.addressLimitTitle) }
        val addressLimitEnableButton = KCheckBox { withId(R.id.addressLimitEnableButton) }
        val addressLimitInfinity = KTextView { withId(R.id.addressLimitInfinity) }
        val addressLimitField = KEditText { withId(R.id.addressLimitField) }
        val addressLimitDivider = KView { withId(R.id.addressLimitDivider) }

        val updateIntervalTitle = KTextView { withId(R.id.updateIntervalTitle) }
        val updateIntervalField = KEditText { withId(R.id.updateIntervalField) }
        val updateIntervalMilliseconds = KTextView { withId(R.id.updateIntervalMilliseconds) }
        val updateIntervalDivider = KView { withId(R.id.updateIntervalDivider) }

        val showNetworkInfoToggleSetting by lazy {
            SingleToggleButtonSetting(
                divider = showNetworkInfoDivider,
                label = showNetworkInfoTitle,
                text = R.string.show_network_info,
                button = showNetworkInfoButton,
            )
        }
    }

    object Connection : KScreen<Connection>() {
        override val layoutId: Int = R.layout.settings_connection
        override val viewClass: Class<*> = ConnectionSettingsFragment::class.java

        val connectionTimeoutTitle = KTextView { withId(R.id.connectionTimeoutTitle) }
        val connectionTimeoutTimeInput = KTimeInputBinder(R.id.connectionTimeoutTimeInput)
        val connectionTimeoutSecondsLabel = KTextView { withId(R.id.connectionTimeoutSecondsLabel) }
        val connectionTimeoutDivider = KView { withId(R.id.connectionTimeoutDivider) }

        val heartbeatTimeoutTitle = KTextView { withId(R.id.heartbeatTimeoutTitle) }
        val heartbeatTimeoutTimeInput = KTimeInputBinder(R.id.heartbeatTimeoutTimeInput)
        val heartbeatTimeoutSecondsLabel = KTextView { withId(R.id.heartbeatTimeoutSecondsLabel) }
        val heartbeatTimeoutDivider = KView { withId(R.id.heartbeatTimeoutDivider) }

        val scanTimeoutTitle = KTextView { withId(R.id.scanTimeoutTitle) }
        val scanTimeoutTimeInput = KTimeInputBinder(R.id.scanTimeoutTimeInput)
        val scanTimeoutSecondsLabel = KTextView { withId(R.id.scanTimeoutSecondsLabel) }
        val scanTimeoutDivider = KView { withId(R.id.scanTimeoutDivider) }

        private val alwaysScanPublicTitle = KTextView { withId(R.id.alwaysScanPublicTitle) }
        private val alwaysScanPublicButton = KCheckBox { withId(R.id.alwaysScanPublicButton) }
        private val alwaysScanPublicDivider = KView { withId(R.id.alwaysScanPublicDivider) }

        val alwaysScanPublicToggleSetting by lazy {
            SingleToggleButtonSetting(
                divider = alwaysScanPublicDivider,
                label = alwaysScanPublicTitle,
                text = R.string.always_scan_publicly,
                button = alwaysScanPublicButton,
            )
        }
    }

    object Missions : KScreen<Missions>() {
        override val layoutId: Int = R.layout.settings_missions
        override val viewClass: Class<*> = MissionSettingsFragment::class.java

        val rewardsTitle = KTextView { withId(R.id.rewardsTitle) }
        val rewardsAllButton = KCheckBox { withId(R.id.rewardsAllButton) }
        val rewardsNoneButton = KCheckBox { withId(R.id.rewardsNoneButton) }
        val rewardsBatteryButton = KCheckBox { withId(R.id.rewardsBatteryButton) }
        val rewardsCoolantButton = KCheckBox { withId(R.id.rewardsCoolantButton) }
        val rewardsNukeButton = KCheckBox { withId(R.id.rewardsNukeButton) }
        val rewardsProductionButton = KCheckBox { withId(R.id.rewardsProductionButton) }
        val rewardsShieldButton = KCheckBox { withId(R.id.rewardsShieldButton) }
        val rewardsDivider = KView { withId(R.id.rewardsDivider) }

        val autoDismissalTitle = KTextView { withId(R.id.autoDismissalTitle) }
        val autoDismissalButton = KCheckBox { withId(R.id.autoDismissalButton) }
        val autoDismissalSecondsLabel = KTextView { withId(R.id.autoDismissalSecondsLabel) }
        val autoDismissalTimeInput = KTimeInputBinder(R.id.autoDismissalTimeInput)
        val autoDismissalDivider = KView { withId(R.id.autoDismissalDivider) }

        val rewardSettings by lazy {
            listOf(
                GroupedToggleButtonSetting(rewardsBatteryButton, R.string.mission_battery),
                GroupedToggleButtonSetting(rewardsCoolantButton, R.string.mission_coolant),
                GroupedToggleButtonSetting(rewardsNukeButton, R.string.mission_nuke),
                GroupedToggleButtonSetting(rewardsProductionButton, R.string.mission_production),
                GroupedToggleButtonSetting(rewardsShieldButton, R.string.mission_shield),
            )
        }
    }

    object Allies : KScreen<Allies>() {
        override val layoutId: Int = R.layout.settings_allies
        override val viewClass: Class<*> = AllySettingsFragment::class.java

        val sortTitle = KTextView { withId(R.id.allySortingTitle) }
        val sortDefaultButton = KCheckBox { withId(R.id.allySortingDefaultButton) }
        val sortClassButton1 = KCheckBox { withId(R.id.allySortingClassButton1) }
        val sortEnergyButton = KCheckBox { withId(R.id.allySortingEnergyButton) }
        val sortStatusButton = KCheckBox { withId(R.id.allySortingStatusButton) }
        val sortClassButton2 = KCheckBox { withId(R.id.allySortingClassButton2) }
        val sortNameButton = KCheckBox { withId(R.id.allySortingNameButton) }
        val sortDivider = KView { withId(R.id.allySortingDivider) }

        val showDestroyedTitle = KTextView { withId(R.id.showDestroyedAlliesTitle) }
        val showDestroyedButton = KCheckBox { withId(R.id.showDestroyedAlliesButton) }
        val showDestroyedDivider = KView { withId(R.id.showDestroyedAlliesDivider) }

        val manuallyReturnTitle = KTextView { withId(R.id.manuallyReturnTitle) }
        val manuallyReturnButton = KCheckBox { withId(R.id.manuallyReturnButton) }
        val manuallyReturnDivider = KView { withId(R.id.manuallyReturnDivider) }

        val sortMethodSettings by lazy {
            listOf(
                GroupedToggleButtonSetting(sortClassButton1, R.string.sort_by_class),
                GroupedToggleButtonSetting(sortEnergyButton, R.string.sort_by_energy),
                GroupedToggleButtonSetting(sortStatusButton, R.string.sort_by_status),
                GroupedToggleButtonSetting(sortClassButton2, R.string.sort_by_class),
                GroupedToggleButtonSetting(sortNameButton, R.string.sort_by_name),
            )
        }

        val singleToggleSettings by lazy {
            listOf(
                SingleToggleButtonSetting(
                    showDestroyedDivider,
                    showDestroyedTitle,
                    R.string.show_destroyed_allies,
                    showDestroyedButton,
                ),
                SingleToggleButtonSetting(
                    manuallyReturnDivider,
                    manuallyReturnTitle,
                    R.string.manually_return_from_commands,
                    manuallyReturnButton,
                ),
            )
        }
    }

    object Enemies : KScreen<Enemies>() {
        override val layoutId: Int = R.layout.settings_enemies
        override val viewClass: Class<*> = EnemySettingsFragment::class.java

        val sortTitle = KTextView { withId(R.id.enemySortingTitle) }
        val sortDefaultButton = KCheckBox { withId(R.id.enemySortingDefaultButton) }
        val sortSurrenderButton = KCheckBox { withId(R.id.enemySortingSurrenderButton) }
        val sortRaceButton = KCheckBox { withId(R.id.enemySortingRaceButton) }
        val sortNameButton = KCheckBox { withId(R.id.enemySortingNameButton) }
        val sortRangeButton = KCheckBox { withId(R.id.enemySortingRangeButton) }
        val sortDivider = KView { withId(R.id.enemySortingDivider) }

        val reverseRaceSortTitle = KTextView { withId(R.id.reverseRaceSortTitle) }
        val reverseRaceSortButton = KCheckBox { withId(R.id.reverseRaceSortButton) }

        val surrenderRangeTitle = KTextView { withId(R.id.surrenderRangeTitle) }
        val surrenderRangeField = KEditText { withId(R.id.surrenderRangeField) }
        val surrenderRangeKm = KTextView { withId(R.id.surrenderRangeKm) }
        val surrenderRangeEnableButton = KCheckBox { withId(R.id.surrenderRangeEnableButton) }
        val surrenderRangeInfinity = KTextView { withId(R.id.surrenderRangeInfinity) }
        val surrenderRangeDivider = KView { withId(R.id.surrenderRangeDivider) }

        val showIntelTitle = KTextView { withId(R.id.showIntelTitle) }
        val showIntelButton = KCheckBox { withId(R.id.showIntelButton) }
        val showIntelDivider = KView { withId(R.id.showIntelDivider) }

        val showTauntStatusTitle = KTextView { withId(R.id.showTauntStatusTitle) }
        val showTauntStatusButton = KCheckBox { withId(R.id.showTauntStatusButton) }
        val showTauntStatusDivider = KView { withId(R.id.showTauntStatusDivider) }

        val disableIneffectiveTitle = KTextView { withId(R.id.disableIneffectiveTitle) }
        val disableIneffectiveButton = KCheckBox { withId(R.id.disableIneffectiveButton) }
        val disableIneffectiveDivider = KView { withId(R.id.disableIneffectiveDivider) }

        val sortMethodSettings by lazy {
            listOf(
                GroupedToggleButtonSetting(sortSurrenderButton, R.string.surrender),
                GroupedToggleButtonSetting(sortRaceButton, R.string.sort_by_race),
                GroupedToggleButtonSetting(sortNameButton, R.string.sort_by_name),
                GroupedToggleButtonSetting(sortRangeButton, R.string.sort_by_range),
            )
        }

        val singleToggleSettings by lazy {
            listOf(
                SingleToggleButtonSetting(
                    showIntelDivider,
                    showIntelTitle,
                    R.string.show_intel,
                    showIntelButton,
                ),
                SingleToggleButtonSetting(
                    showTauntStatusDivider,
                    showTauntStatusTitle,
                    R.string.show_taunt_status,
                    showTauntStatusButton,
                ),
                SingleToggleButtonSetting(
                    disableIneffectiveDivider,
                    disableIneffectiveTitle,
                    R.string.disable_ineffective_taunts,
                    disableIneffectiveButton,
                ),
            )
        }

        val reverseRaceSortSingleToggle by lazy {
            SingleToggleButtonSetting(
                sortDivider,
                reverseRaceSortTitle,
                R.string.reverse_sorting_by_race,
                reverseRaceSortButton,
            )
        }
    }

    object Biomechs : KScreen<Biomechs>() {
        override val layoutId: Int = R.layout.settings_biomechs
        override val viewClass: Class<*> = BiomechSettingsFragment::class.java

        val sortTitle = KTextView { withId(R.id.biomechSortingTitle) }
        val sortDefaultButton = KCheckBox { withId(R.id.biomechSortingDefaultButton) }
        val sortClassButton1 = KCheckBox { withId(R.id.biomechSortingClassButton1) }
        val sortStatusButton = KCheckBox { withId(R.id.biomechSortingStatusButton) }
        val sortClassButton2 = KCheckBox { withId(R.id.biomechSortingClassButton2) }
        val sortNameButton = KCheckBox { withId(R.id.biomechSortingNameButton) }
        val sortDivider = KView { withId(R.id.biomechSortingDivider) }

        val freezeDurationTitle = KTextView { withId(R.id.freezeDurationTitle) }
        val freezeDurationTimeInput = KTimeInputBinder(R.id.freezeDurationTimeInput)
        val freezeDurationDivider = KView { withId(R.id.freezeDurationDivider) }

        val sortMethodSettings by lazy {
            listOf(
                GroupedToggleButtonSetting(sortClassButton1, R.string.sort_by_class),
                GroupedToggleButtonSetting(sortStatusButton, R.string.sort_by_status),
                GroupedToggleButtonSetting(sortClassButton2, R.string.sort_by_class),
                GroupedToggleButtonSetting(sortNameButton, R.string.sort_by_name),
            )
        }
    }

    object Routing : KScreen<Routing>() {
        override val layoutId: Int = R.layout.settings_routing
        override val viewClass: Class<*> = RoutingSettingsFragment::class.java

        val incentivesTitle = KTextView { withId(R.id.incentivesTitle) }
        val incentivesAllButton = KCheckBox { withId(R.id.incentivesAllButton) }
        val incentivesNoneButton = KCheckBox { withId(R.id.incentivesNoneButton) }
        val incentivesMissionsButton = KCheckBox { withId(R.id.incentivesMissionsButton) }
        val incentivesHasEnergyButton = KCheckBox { withId(R.id.incentivesHasEnergyButton) }
        val incentivesNeedsEnergyButton = KCheckBox { withId(R.id.incentivesNeedsEnergyButton) }
        val incentivesNeedsDamConButton = KCheckBox { withId(R.id.incentivesNeedsDamConButton) }
        val incentivesMalfunctionButton = KCheckBox { withId(R.id.incentivesMalfunctionButton) }
        val incentivesAmbassadorButton = KCheckBox { withId(R.id.incentivesAmbassadorButton) }
        val incentivesHostageButton = KCheckBox { withId(R.id.incentivesHostageButton) }
        val incentivesCommandeeredButton = KCheckBox { withId(R.id.incentivesCommandeeredButton) }
        val incentivesDivider = KView { withId(R.id.incentivesDivider) }

        val incentiveSettings by lazy {
            listOf(
                GroupedToggleButtonSetting(
                    incentivesNeedsEnergyButton,
                    R.string.route_incentive_needs_energy,
                ),
                GroupedToggleButtonSetting(
                    incentivesNeedsDamConButton,
                    R.string.route_incentive_needs_damcon,
                ),
                GroupedToggleButtonSetting(
                    incentivesMalfunctionButton,
                    R.string.route_incentive_malfunction,
                ),
                GroupedToggleButtonSetting(
                    incentivesAmbassadorButton,
                    R.string.route_incentive_ambassador,
                ),
                GroupedToggleButtonSetting(
                    incentivesHostageButton,
                    R.string.route_incentive_hostage,
                ),
                GroupedToggleButtonSetting(
                    incentivesCommandeeredButton,
                    R.string.route_incentive_commandeered,
                ),
                GroupedToggleButtonSetting(
                    incentivesHasEnergyButton,
                    R.string.route_incentive_has_energy,
                ),
                GroupedToggleButtonSetting(
                    incentivesMissionsButton,
                    R.string.route_incentive_missions,
                ),
            )
        }

        val avoidancesTitle = KTextView { withId(R.id.avoidancesTitle) }
        val avoidancesAllButton = KCheckBox { withId(R.id.avoidancesAllButton) }
        val avoidancesNoneButton = KCheckBox { withId(R.id.avoidancesNoneButton) }
        val avoidancesDivider = KView { withId(R.id.avoidancesDivider) }

        val blackHolesTitle = KTextView { withId(R.id.blackHolesTitle) }
        val blackHolesClearanceField = KEditText { withId(R.id.blackHolesClearanceField) }
        val blackHolesClearanceKm = KTextView { withId(R.id.blackHolesClearanceKm) }
        val blackHolesButton = KCheckBox { withId(R.id.blackHolesButton) }

        val minesTitle = KTextView { withId(R.id.minesTitle) }
        val minesClearanceField = KEditText { withId(R.id.minesClearanceField) }
        val minesClearanceKm = KTextView { withId(R.id.minesClearanceKm) }
        val minesButton = KCheckBox { withId(R.id.minesButton) }

        val typhonsTitle = KTextView { withId(R.id.typhonsTitle) }
        val typhonsClearanceField = KEditText { withId(R.id.typhonsClearanceField) }
        val typhonsClearanceKm = KTextView { withId(R.id.typhonsClearanceKm) }
        val typhonsButton = KCheckBox { withId(R.id.typhonsButton) }

        val avoidanceSettings by lazy {
            listOf(
                AvoidanceSetting(
                    blackHolesTitle,
                    R.string.avoidance_black_hole,
                    blackHolesClearanceField,
                    blackHolesClearanceKm,
                    blackHolesButton,
                ),
                AvoidanceSetting(
                    minesTitle,
                    R.string.avoidance_mine,
                    minesClearanceField,
                    minesClearanceKm,
                    minesButton,
                ),
                AvoidanceSetting(
                    typhonsTitle,
                    R.string.avoidance_typhon,
                    typhonsClearanceField,
                    typhonsClearanceKm,
                    typhonsButton,
                ),
            )
        }

        data class AvoidanceSetting(
            val label: KTextView,
            @StringRes val text: Int,
            val input: KEditText,
            val kmLabel: KTextView,
            val button: KCheckBox,
        ) {
            fun doesNotExist() {
                label.doesNotExist()
                input.doesNotExist()
                kmLabel.doesNotExist()
                button.doesNotExist()
            }
        }
    }

    object Personal : KScreen<Personal>() {
        override val layoutId: Int = R.layout.settings_personal
        override val viewClass: Class<*> = PersonalSettingsFragment::class.java

        val themeTitle = KTextView { withId(R.id.themeTitle) }
        val themeDefaultButton = KCheckBox { withId(R.id.themeDefaultButton) }
        val themeRedButton = KCheckBox { withId(R.id.themeRedButton) }
        val themeGreenButton = KCheckBox { withId(R.id.themeGreenButton) }
        val themeYellowButton = KCheckBox { withId(R.id.themeYellowButton) }
        val themeBlueButton = KCheckBox { withId(R.id.themeBlueButton) }
        val themePurpleButton = KCheckBox { withId(R.id.themePurpleButton) }

        val threeDigitDirectionsTitle = KTextView { withId(R.id.threeDigitDirectionsTitle) }
        val threeDigitDirectionsButton = KCheckBox { withId(R.id.threeDigitDirectionsButton) }
        val threeDigitDirectionsLabel = KTextView { withId(R.id.threeDigitDirectionsLabel) }

        val soundVolumeTitle = KTextView { withId(R.id.soundVolumeTitle) }
        val soundVolumeBar = KSeekBar { withId(R.id.soundVolumeBar) }
        val soundVolumeLabel = KTextView { withId(R.id.soundVolumeLabel) }

        val themeButtons by lazy {
            listOf(
                themeDefaultButton,
                themeRedButton,
                themeGreenButton,
                themeYellowButton,
                themeBlueButton,
                themePurpleButton,
            )
        }
    }
}
