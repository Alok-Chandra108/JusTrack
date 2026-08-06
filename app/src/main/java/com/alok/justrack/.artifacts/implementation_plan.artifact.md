# Implementation Plan - Redesign Show Details Page

Redesign the TV Show details page to match the tabbed layout from the provided reference screenshot, focusing on the "ABOUT" and "EPISODES" separation.

## User Review Required

> [!IMPORTANT]
> The current app uses a **Dark Theme** (`#111315`), while the reference screenshot uses a **Light Theme**. I will implement the new layout using the existing Dark Theme colors to maintain consistency with the rest of the app.

## Proposed Changes

### UI Components

#### [MODIFY] [MovieDetailsScreen.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/screens/MovieDetailsScreen.kt)
- Introduce a `TabRow` for TV Shows with "ABOUT" and "EPISODES" tabs.
- Create a `TvShowAboutSection` to house existing overview, cast, and recommendations.
- Create a `TvShowEpisodesSection` to house the "All episodes" (Seasons) section.
- Redesign `SeasonsSection` and `SeasonCard` to match the cleaner look in the screenshot (including the checkmark for progress).
- Refactor `MovieDetailsScreen` to toggle between these sections based on the selected tab.

### Data & ViewModel

#### [MODIFY] [DetailViewModel.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/viewmodel/DetailViewModel.kt)
- Ensure season progress (watched/total count) is correctly calculated and exposed for the UI.

## Verification Plan

### Automated Tests
- Build the project to ensure no regressions in layout or logic.

### Manual Verification
- Deploy to the device and verify:
    - Tab switching between "ABOUT" and "EPISODES" works smoothly.
    - "ABOUT" tab shows general info, cast, and recommendations.
    - "EPISODES" tab shows the redesigned seasons list with progress indicators.
    - Movie details page remains unaffected (no tabs).
