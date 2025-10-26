package artemis.agent

import android.content.Context
import android.text.Spanned
import android.view.ContextThemeWrapper
import androidx.appcompat.app.AlertDialog
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.noties.markwon.Markwon
import java.io.File
import java.util.stream.IntStream

class UpdateCheckTest :
    DescribeSpec({
        describe("UpdateCheck") {
            val version = "Version text here"
            val changelog = "Changelog text here"
            val noUpdates = "No updates"

            val titleId = slot<Int>()
            val messageId = slot<Int>()
            val messageString = slot<CharSequence>()
            val cancellable = slot<Boolean>()

            val tempDir = tempdir()
            val versionFileName = "app_version.dat"

            class MockSpanned(val text: CharSequence) : Spanned, CharSequence by text {
                override fun chars(): IntStream = text.chars()

                override fun codePoints(): IntStream = text.codePoints()

                override fun isEmpty(): Boolean = text.isEmpty()

                override fun getSpanEnd(tag: Any?): Int = 0

                override fun getSpanFlags(tag: Any?): Int = 0

                override fun getSpanStart(tag: Any?): Int = 0

                override fun <T : Any?> getSpans(
                    start: Int,
                    end: Int,
                    type: Class<T?>?,
                ): Array<out T?>? = null

                override fun nextSpanTransition(start: Int, limit: Int, type: Class<*>?): Int = 0

                override fun toString(): String = text.toString()
            }

            val mockContext =
                mockk<Context> {
                    every { getString(R.string.changelog) } returns changelog

                    every { openFileInput(any()) } answers
                        {
                            File(tempDir, firstArg<String>()).inputStream()
                        }
                    every { openFileOutput(any(), any()) } answers
                        {
                            File(tempDir, firstArg<String>()).outputStream()
                        }

                    every { theme } returns
                        mockk { every { resolveAttribute(any(), any(), any()) } returns true }
                }

            mockkConstructor(ContextThemeWrapper::class)
            every { anyConstructed<ContextThemeWrapper>().getSystemService(any()) } returns null
            every { anyConstructed<ContextThemeWrapper>().getText(R.string.app_version) } returns
                version
            every { anyConstructed<ContextThemeWrapper>().getText(R.string.no_updates) } returns
                noUpdates

            mockkStatic(Markwon::create)
            every { Markwon.create(mockContext) } returns
                mockk { every { toMarkdown(any()) } answers { MockSpanned(firstArg<String>()) } }

            mockkConstructor(AlertDialog.Builder::class)
            every { anyConstructed<AlertDialog.Builder>().setTitle(capture(titleId)) } answers
                {
                    self as AlertDialog.Builder
                }
            every { anyConstructed<AlertDialog.Builder>().setMessage(capture(messageId)) } answers
                {
                    self as AlertDialog.Builder
                }
            every {
                anyConstructed<AlertDialog.Builder>().setMessage(capture(messageString))
            } answers { self as AlertDialog.Builder }
            every {
                anyConstructed<AlertDialog.Builder>().setCancelable(capture(cancellable))
            } answers { self as AlertDialog.Builder }

            beforeAny {
                titleId.clear()
                messageId.clear()
                messageString.clear()
                cancellable.clear()
            }

            afterSpec {
                clearAllMocks()
                unmockkAll()
            }

            describe("Startup") {
                it("First time: dialog displayed") {
                    UpdateCheck.STARTUP.createAlert(mockContext).shouldNotBeNull()

                    val versionFile = File(tempDir, versionFileName)
                    versionFile.shouldExist()

                    versionFile.inputStream().use { stream ->
                        stream.readBytes().decodeToString() shouldBeEqual BuildConfig.VERSION_NAME
                    }

                    titleId.captured shouldBeEqual R.string.app_version
                    messageString.captured.toString() shouldBeEqual changelog
                    cancellable.captured.shouldBeTrue()
                }

                it("Second time: dialog not displayed") {
                    UpdateCheck.STARTUP.createAlert(mockContext).shouldBeNull()

                    titleId.isCaptured.shouldBeFalse()
                    messageString.isCaptured.shouldBeFalse()
                    cancellable.isCaptured.shouldBeFalse()
                }
            }

            it("Manual check: always display, no updates") {
                UpdateCheck.MANUAL.createAlert(mockContext).shouldNotBeNull()
                titleId.captured shouldBeEqual R.string.app_version
                messageId.captured shouldBeEqual R.string.no_updates
                cancellable.captured.shouldBeTrue()
            }

            it("Game end: never display") {
                UpdateCheck.GAME_END.createAlert(mockContext).shouldBeNull()
                titleId.isCaptured.shouldBeFalse()
                messageString.isCaptured.shouldBeFalse()
                messageId.isCaptured.shouldBeFalse()
                cancellable.isCaptured.shouldBeFalse()
            }
        }
    })
