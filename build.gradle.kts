import java.util.Properties

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// Shared public ZFV key (docs/port-spec.md §5.3). local.properties can override it.
val defaultZfvApiKey =
    "Y203MHVwaXU0OWFyeXM2MHRscWUyZncwcTpTQTJRRy83eXE5NmEzczNyRS91TjhBaysrYWl4aCs5SGhRUE9xOTk3ZzdDa1ZpdFVvQkJhK3hHN0Yyd1lLaTNu"

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val zfvApiKey: String by extra(
    localProperties.getProperty("ZFV_API_KEY")?.trim()?.takeIf { it.isNotEmpty() } ?: defaultZfvApiKey,
)

// One version for the phone and watch apps. Release tags are v<versionName> (README, "Releasing").
val appVersionCode: Int by extra(1)
val appVersionName: String by extra("1.0.0")

// Release key from local.properties (RELEASE_STORE_FILE, …). Without it, release builds use the
// debug key so a fresh checkout still builds.
val releaseSigning: Map<String, String> by extra(
    listOf("RELEASE_STORE_FILE", "RELEASE_STORE_PASSWORD", "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD")
        .mapNotNull { key -> localProperties.getProperty(key)?.let { key to it } }
        .toMap()
        .takeIf { it.size == 4 }
        .orEmpty(),
)
