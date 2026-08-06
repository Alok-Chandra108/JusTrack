# Implementation Plan - Move Tabs Below Backdrop

Adjust the TV Show details layout so that the "ABOUT" and "EPISODES" tabs are positioned immediately below the backdrop image.

## Proposed Changes

### UI Components

#### [MODIFY] [MovieDetailsScreen.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/screens/MovieDetailsScreen.kt)
- **Restructure Layout**:
    - Move `TabRow` to be the first element after `BackdropHeader` for TV Shows.
    - Move `PosterInfoRow` and `ActionButtons` into `TvShowAboutSection`.
    - Ensure `TvShowEpisodesSection` only displays the episodes-related content (Seasons).
    - Maintain the original layout for Movies (Poster/Title at the top).
- **Refine Styling**:
    - Ensure the `TabRow` is full-width (outside the horizontal padding).
    - Add appropriate spacing between the Backdrop, Tabs, and Content.

## Verification Plan

### Manual Verification
- Deploy to the device and verify:
    - On a TV Show page:
        - Tabs appear directly under the backdrop.
        - Selecting "ABOUT" shows the Poster, Title, Action Buttons, and Description.
        - Selecting "EPISODES" shows only the Seasons list (All episodes).
    - On a Movie page:
        - Layout remains unchanged (Poster, Title, and Action Buttons are at the top).
