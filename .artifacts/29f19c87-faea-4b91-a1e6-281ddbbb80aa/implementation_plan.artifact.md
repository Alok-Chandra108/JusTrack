# Fix Gradle Sync Error: Cannot add extension with name 'kotlin'

The project is using Android Gradle Plugin (AGP) 9.3.1. Starting with AGP 9.0, Kotlin support is built-in and enabled by default. Manually applying the `org.jetbrains.kotlin.android` plugin causes a conflict because AGP already registers the `kotlin` extension.

## Proposed Changes

### Build Configuration

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/build.gradle.kts)
- Remove `alias(libs.plugins.kotlin.android)` from the `plugins` block.
- Uncomment `alias(libs.plugins.kotlin.compose)` to ensure Compose compiler support is active.
- Remove the deprecated `kotlinOptions` block as it's redundant (defaults are handled by AGP 9.0).

#### [MODIFY] [build.gradle.kts](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/build.gradle.kts)
- Remove `alias(libs.plugins.kotlin.android) apply false` from the root `plugins` block.

#### [MODIFY] [gradle/libs.versions.toml](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/gradle/libs.versions.toml)
- Remove the `kotlin-android` plugin definition from the `[plugins]` section.

## Verification Plan

### Automated Tests
- Run Gradle sync to verify the "Cannot add extension with name 'kotlin'" error is resolved.
- Run `./gradlew help` to ensure the project structure is valid.

### Manual Verification
- Verify that the IDE no longer shows sync errors in the build tool window.
