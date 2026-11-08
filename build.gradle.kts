plugins {
    alias(libs.plugins.android.application) apply false
    // Not applied anywhere: AGP 9 has built-in Kotlin. Declaring it pins the KGP version AGP uses.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
