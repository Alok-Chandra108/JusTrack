# Implementation Plan - UI Pixel Perfection

Refine the UI of JusTrack to exactly match the provided reference screenshots for Shows, Movies, and Profile screens.

## User Review Required

> [!IMPORTANT]
> - **Tab Styling**: Moving to all-caps "WATCH LIST" and "UPCOMING" with white indicators.
> - **Buttons**: Standardizing on gold pill-shaped buttons for all empty states.
> - **Movies Screen**: Will be updated to match the tabbed layout seen in the screenshots.
> - **Profile Screen**: Adding social stats and specific "Favorite" sections with heart icons.

## Proposed Changes

### [Theme & Foundations]

#### [MODIFY] [Color.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/theme/Color.kt)
- Define `GoldAccent` (`#FFC107`) for buttons.
- Define `HeartRed` (`#E91E63`) for favorite icons.

### [Components]

#### [MODIFY] [PremiumComponents.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/components/PremiumComponents.kt)
- **PremiumEmptyState**: Update to use centered vertical layout, gold pill button, and improved spacing.
- **SectionHeader**: Add support for optional leading icons and white chevrons.
- **SocialStatsRow**: New component for Following/Followers/Comments.

### [Screens]

#### [MODIFY] [WatchlistShowsScreen.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/screens/WatchlistShowsScreen.kt)
- Update `TabRow` to match the white/black high-contrast look and all-caps labels.

#### [MODIFY] [PlaceholderScreens.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/screens/PlaceholderScreens.kt)
- **MoviesScreen**: Redesign to use a `WatchlistMoviesScreen` composable with tabs.
- **ProfileScreen**:
    - Implement TopBar with Notification Bell and More options.
    - Add `SocialStatsRow` at the top.
    - Reorder sections to match: Stats, Lists, Shows, Favorite Shows, Movies, Favorite Movies.

---

## Verification Plan

### Manual Verification
- Deploy and compare side-by-side with reference screenshots.
- Ensure "Shows" and "Movies" screens are consistent in their tabbed behavior.
- Verify the Profile screen sections and icons.
- Check accessibility of the new gold button (contrast ratio).
