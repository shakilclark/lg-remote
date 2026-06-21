// Top-level build file — plugins declared here, applied in module build files.
// AGP 9 ships built-in Kotlin (the org.jetbrains.kotlin.android plugin is removed). To run a Kotlin
// newer than AGP's bundled default, pin the Kotlin Gradle plugin on the buildscript classpath.
// https://developer.android.com/build/releases/agp-9-0-0-release-notes#android-gradle-plugin-built-in-kotlin
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
