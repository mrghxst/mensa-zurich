import java.util.Properties

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// Shared public ZFV key (ANDROID_PORT_PROMPT.md §5.3). local.properties can override it.
val defaultZfvApiKey =
    "Y203MHVwaXU0OWFyeXM2MHRscWUyZncwcTpTQTJRRy83eXE5NmEzczNyRS91TjhBaysrYWl4aCs5SGhRUE9xOTk3ZzdDa1ZpdFVvQkJhK3hHN0Yyd1lLaTNu"

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val zfvApiKey: String by extra(
    localProperties.getProperty("ZFV_API_KEY")?.trim()?.takeIf { it.isNotEmpty() } ?: defaultZfvApiKey,
)
