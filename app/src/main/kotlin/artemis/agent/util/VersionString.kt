package artemis.agent.util

import com.walkertribe.ian.util.Version

@JvmInline
value class VersionString(private val version: String) {
    fun toVersion(): Version =
        version.split('.').let { parts ->
            Version(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }

    override fun toString(): String = version
}
