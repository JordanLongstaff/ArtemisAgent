package artemis.agent

object RemoteConfigKey {
    const val artemisLatestVersion = "artemis_latest_version"
    private const val requiredVersion = "required_version"

    object RequiredVersion {
        const val artemis = "${requiredVersion}_artemis"
        const val security = "${requiredVersion}_security"
    }
}
