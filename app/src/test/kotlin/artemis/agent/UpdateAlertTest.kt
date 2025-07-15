package artemis.agent

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.walkertribe.ian.util.Version
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.checkAll
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll

class UpdateAlertTest :
    DescribeSpec({
        describe("UpdateAlert") {
            val mockRestart = "Restart to support Artemis "
            val mockUpdate = "Update to support Artemis "
            val versionUpdate = "Artemis "
            val immediateTitle = "Update Required"
            val flexibleTitle = "Update Available"
            val immediateMessage = "Immediate update"
            val flexibleMessage = "Flexible update"

            val mockContext =
                mockk<Context> {
                    every { getString(R.string.update_required_title) } returns immediateTitle
                    every { getString(R.string.update_available_title) } returns flexibleTitle
                    every { getString(R.string.update_required_message) } returns immediateMessage
                    every { getString(R.string.update_available_message) } returns flexibleMessage

                    every {
                        getString(R.string.new_version_title, *varargAny { nArgs == 1 })
                    } answers { "$versionUpdate${lastArg<Array<Any?>>().joinToString()}" }
                    every {
                        getString(R.string.new_version_restart_message, *varargAny { nArgs == 1 })
                    } answers { "$mockRestart${lastArg<Array<Any?>>().joinToString()}" }
                    every {
                        getString(R.string.new_version_update_message, *varargAny { nArgs == 1 })
                    } answers { "$mockUpdate${lastArg<Array<Any?>>().joinToString()}" }
                }

            val mockRemoteConfig =
                mockk<FirebaseRemoteConfig>(relaxed = true) {
                    every { getLong(any()) } returns 1L
                    every { getString(any()) } returns "1.0.0"
                }

            mockkStatic(FirebaseRemoteConfig::class)
            every { FirebaseRemoteConfig.getInstance() } returns mockRemoteConfig

            afterSpec {
                clearAllMocks()
                unmockkAll()
            }

            describe("ArtemisVersion") {
                data class ArtemisVersionTest(
                    val name: String,
                    val prefix: String,
                    val createFn: (String) -> UpdateAlert.ArtemisVersion,
                )

                listOf(
                        ArtemisVersionTest(
                            "Restart",
                            mockRestart,
                            UpdateAlert.ArtemisVersion::Restart,
                        ),
                        ArtemisVersionTest("Update", mockUpdate, UpdateAlert.ArtemisVersion::Update),
                    )
                    .forEach { (name, prefix, createFn) ->
                        describe(name) {
                            it("Title") {
                                checkAll<String> { version ->
                                    val alert = createFn(version)
                                    alert.getTitle(mockContext) shouldBeEqual
                                        "$versionUpdate$version"
                                }
                            }

                            it("Message") {
                                checkAll<String> { version ->
                                    val alert = createFn(version)
                                    alert.getMessage(mockContext) shouldBeEqual "$prefix$version"
                                }
                            }
                        }
                    }
            }

            data class NonVersionTest(
                val name: String,
                val alert: UpdateAlert,
                val title: String,
                val message: String,
            )

            listOf(
                    NonVersionTest(
                        "Immediate",
                        UpdateAlert.Immediate,
                        immediateTitle,
                        immediateMessage,
                    ),
                    NonVersionTest("Flexible", UpdateAlert.Flexible, flexibleTitle, flexibleMessage),
                )
                .forEach { (name, alert, title, message) ->
                    describe(name) {
                        it("Title") { alert.getTitle(mockContext) shouldBeEqual title }
                        it("Message") { alert.getMessage(mockContext) shouldBeEqual message }
                    }
                }

            describe("Check") {
                val maxVersion = Version(1, 0, 0)
                val newArtemisVersion = "3.0.0"
                val latestVersionCode = Int.MAX_VALUE shr 2

                it("No update alert") { UpdateAlert.check(maxVersion, 1).shouldBeNull() }

                it("Restart for Artemis version") {
                    every { mockRemoteConfig.getString(any()) } returns newArtemisVersion

                    val alert = UpdateAlert.check(maxVersion, 1)
                    alert.shouldBeInstanceOf<UpdateAlert.ArtemisVersion.Restart>()
                    alert.newVersion shouldBeEqual newArtemisVersion
                }

                it("Flexible update") {
                    val alert = UpdateAlert.check(maxVersion, latestVersionCode)
                    alert.shouldBeInstanceOf<UpdateAlert.Flexible>()
                }

                describe("Update for Artemis version") {
                    every {
                        mockRemoteConfig.getLong(RemoteConfigKey.RequiredVersion.ARTEMIS)
                    } returns latestVersionCode.toLong()

                    it("Triggers in range") {
                        val alert = UpdateAlert.check(maxVersion, latestVersionCode)
                        alert.shouldBeInstanceOf<UpdateAlert.ArtemisVersion.Update>()
                        alert.newVersion shouldBeEqual newArtemisVersion
                    }

                    it("Does not trigger out of range") {
                        val alert = UpdateAlert.check(maxVersion, latestVersionCode - 1)
                        alert.shouldBeInstanceOf<UpdateAlert.Flexible>()
                    }
                }

                describe("Immediate update") {
                    every {
                        mockRemoteConfig.getLong(RemoteConfigKey.RequiredVersion.SECURITY)
                    } returns latestVersionCode.toLong()

                    it("Triggers in range") {
                        val alert = UpdateAlert.check(maxVersion, latestVersionCode)
                        alert.shouldBeInstanceOf<UpdateAlert.Immediate>()
                    }

                    it("Does not trigger out of range") {
                        val alert = UpdateAlert.check(maxVersion, latestVersionCode - 1)
                        alert.shouldBeInstanceOf<UpdateAlert.Flexible>()
                    }
                }
            }
        }
    })
