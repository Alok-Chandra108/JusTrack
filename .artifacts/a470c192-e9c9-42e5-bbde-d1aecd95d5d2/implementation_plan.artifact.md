# Fix Errors and Warnings in PremiumComponents.kt

This plan addresses several issues in `PremiumComponents.kt` and the project configuration:
- **API Level Issues**: `java.time` APIs require API level 26, but the project's `minSdk` is 24. We will enable API desugaring to support these APIs on older versions.
- **Unused Imports**: Remove three unused import directives in `PremiumComponents.kt`.
- **Modifier Parameter Order**: Reorder parameters in the `PremiumEmptyState` composable to ensure the `Modifier` is the first optional parameter, following Jetpack Compose best practices.
- **Typo Fix**: Rename the symbol `isWatchlisted` to `isInWatchlist` across the project to resolve a typo warning and align with common naming conventions.

## User Review Required

> [!IMPORTANT]
> Enabling API desugaring requires adding a new dependency and updating the Gradle configuration. This is the standard approach for using modern Java APIs on older Android versions.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/Alok%20Chandra/AndroidStudioProjects/JusTrack/gradle/libs.versions.toml)
- Add `desugar_jdk_libs` dependency.

#### [MODIFY] [build.gradle.kts (app module)](file:///C:/Users/Alok%20Chandra/AndroidStudioProjects/JusTrack/app/build.gradle.kts)
- Enable `isCoreLibraryDesugaringEnabled`.
- Add `coreLibraryDesugaring` dependency.

### UI Components

#### [MODIFY] [PremiumComponents.kt](file:///C:/Users/Alok%20Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/components/PremiumComponents.kt)
- Remove unused imports: `ExpandLess`, `ExpandMore`, `RatingSource`.
- Rename `isWatchlisted` to `isInWatchlist` in `ActionButtons`.
- Reorder `modifier` parameter in `PremiumEmptyState`.

#### [MODIFY] [MovieDetailsScreen.kt](file:///C:/Users/Alok%20Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/screens/MovieDetailsScreen.kt)
- Update usages of `isWatchlisted` to `isInWatchlist`.

### View Model

#### [MODIFY] [DetailViewModel.kt](file:///C:/Users/Alok%20Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/viewmodel/DetailViewModel.kt)
- Rename `isWatchlisted` and `_isWatchlisted` to `isInWatchlist` and `_isInWatchlist`.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure the project builds correctly with desugaring.

### Manual Verification
- Verify that the app runs on a device/emulator (API 24+) and that the date-based logic in `ActionButtons` works correctly without crashing.
