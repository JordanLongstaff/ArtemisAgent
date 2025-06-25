package artemis.agent

object RemoteConfigKey {
    const val ARTEMIS_LATEST_VERSION = "artemis_latest_version"
    private const val REQUIRED_VERSION = "required_version"

    object RequiredVersion {
        const val ARTEMIS = "${REQUIRED_VERSION}_artemis"
        const val SECURITY = "${REQUIRED_VERSION}_security"
    }
}
