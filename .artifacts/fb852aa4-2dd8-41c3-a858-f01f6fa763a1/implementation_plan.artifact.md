# Implementation Plan - Fix ProfileScreen Navigation Parameter

Add the missing `navController` parameter to `ProfileScreen` to resolve the compilation error and enable navigation from the profile screen.

## User Review Required

> [!NOTE]
> This change adds a new parameter to `ProfileScreen`. While this fixes the compilation error in `NavGraph.kt`, any other manual calls to `ProfileScreen` (e.g., in previews) might also need updating if they were relying on the old signature. I have checked and found no other usages in the project.

## Proposed Changes

### [app]

#### [MODIFY] [PlaceholderScreens.kt](file:///C:/Users/Alok%20Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/screens/PlaceholderScreens.kt)
- Update `ProfileScreen` function signature to include `navController: NavController`.
- Update the watchlist activity list items to be clickable and navigate to the `Detail` screen.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify that the compilation error is resolved.

### Manual Verification
- Deploy the app and navigate to the Profile screen.
- Verify that clicking a watchlist item in the Profile screen navigates to its detail view.
