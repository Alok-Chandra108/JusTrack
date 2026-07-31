# Fix Unresolved reference 'kotlinOptions'

The project is using Android Gradle Plugin (AGP) 9.3.1. Starting from AGP 9.0, Kotlin support is built-in, and the `org.jetbrains.kotlin.android` plugin is no longer required. Furthermore, the `android.kotlinOptions{}` DSL has been replaced by the `kotlin.compilerOptions{}` DSL.

This plan outlines the steps to migrate the project to use built-in Kotlin support, which will resolve the "Unresolved reference 'kotlinOptions'" error.

## User Review Required

> [!IMPORTANT]
> This change involves removing the explicit Kotlin Android plugin and updating how Kotlin is configured in the build scripts. This is the recommended approach for AGP 9.0+.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/Alok%20Chandra/AndroidStudioProjects/JusTrack/gradle/libs.versions.toml)
- Remove the `kotlin-android` plugin definition as it is no longer needed with AGP 9.0+.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/Alok%20Chandra/AndroidStudioProjects/JusTrack/build.gradle.kts) (root)
- Remove `alias(libs.plugins.kotlin.android) apply false` from the `plugins` block.

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/Alok%20Chandra/AndroidStudioProjects/JusTrack/app/build.gradle.kts)
- Remove `alias(libs.plugins.kotlin.android)` from the `plugins` block.
- Remove the `kotlinOptions` block. The `jvmTarget` will now automatically default to the value set in `compileOptions.targetCompatibility`.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to verify that the project builds successfully without the "Unresolved reference" error.
