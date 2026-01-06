package artemis.agent.cpu

import android.content.Context
import artemis.agent.util.AssetsResolver
import com.walkertribe.ian.util.FilePathResolver
import com.walkertribe.ian.vesseldata.VesselData
import java.io.File
import okio.Path.Companion.toOkioPath

class VesselDataManager(context: Context) {
    private val storageDirectories: List<File> = context.getExternalFilesDirs(null).toList()

    val externalCount by lazy { storageDirectories.size }

    val count by lazy { externalCount + 1 }

    private val assetsResolver = AssetsResolver(context.assets)

    private val defaultVesselData by lazy { VesselData.load(assetsResolver) }

    private val internalStorageVesselData: VesselData? by lazy {
        if (storageDirectories.isEmpty()) null
        else setupFilePathResolver(storageDirectories[0])?.let(VesselData.Companion::load)
    }

    private val externalStorageVesselData: VesselData? by lazy {
        if (externalCount <= 1) null
        else setupFilePathResolver(storageDirectories[1])?.let(VesselData.Companion::load)
    }

    var index: Int = 0
        set(index) {
            if (field != index) {
                field = index
                vesselData =
                    when (index) {
                        1 -> internalStorageVesselData
                        2 -> externalStorageVesselData
                        else -> null
                    } ?: defaultVesselData
            }
        }

    var vesselData: VesselData = defaultVesselData
        private set

    fun checkContext(index: Int, ifError: (String) -> Unit) {
        val vesselDataAtIndex =
            when (index) {
                0 -> defaultVesselData
                1 -> internalStorageVesselData
                2 -> externalStorageVesselData
                else -> require(false) { "Invalid index: $index" }
            }

        if (vesselDataAtIndex is VesselData.Error) {
            ifError(vesselDataAtIndex.message.toString())
        }
    }

    fun reconcileIndex(index: Int): Int = if (index in 0..externalCount) index else 0

    private fun setupFilePathResolver(storageDir: File): FilePathResolver? {
        val datDir = File(storageDir, "dat")

        return if (assetsResolver.copyVesselDataTo(datDir))
            FilePathResolver(storageDir.toOkioPath())
        else null
    }
}
