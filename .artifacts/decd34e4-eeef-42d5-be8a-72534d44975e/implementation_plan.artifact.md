# Implementation Plan - Fix Show Tab Card Layout

The user has reported that the "show tab card" (specifically the `EpisodeTrackingCard` in the Watchlist Shows screen) is not displayed properly. Based on the code analysis, the card layout is somewhat cramped, and there's a high risk of shadow clipping for the check button. Additionally, the fallback mechanism for episode stills (using show posters) can lead to poor visual results due to aspect ratio differences.

## Proposed Changes

### 1. Refactor `EpisodeTrackingCard`

I will update the `EpisodeTrackingCard` to be more consistent with the `UpcomingEpisodeCard`, which has a more robust layout.

#### [MODIFY] [WatchlistShowsScreen.kt](file:///C:/Users/Alok%20Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/screens/WatchlistShowsScreen.kt)
- Increase the `AsyncImage` size from `100x60` to `120x68` (16:9 ratio) to better fit episode stills.
- Add vertical padding to the `Row` inside `NeuCard` to prevent shadow clipping of the check button.
- Update the text hierarchy to be more readable, placing the show name more prominently if needed, or ensuring the episode name stands out.
- Ensure the "PREMIERE" badge is better integrated.

---

### 2. Consistency Improvements

I will also make slight adjustments to `UpcomingEpisodeCard` to ensure both cards feel part of the same design system.

#### [MODIFY] [WatchlistShowsScreen.kt](file:///C:/Users/Alok%20Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/screens/WatchlistShowsScreen.kt)
- Ensure the `NeuCard` in both components has consistent padding and corner radius.
- Standardize the text styles used for show titles and episode numbers across both cards.

## Verification Plan

### Manual Verification
- **Shows Tab**:
    1. Navigate to the "Shows" tab.
    2. Check the "WATCHLIST" sub-tab and verify the `EpisodeTrackingCard` layout. Ensure the check button shadow is fully visible and the image looks correct.
    3. Check the "UPCOMING" sub-tab and verify the `UpcomingEpisodeCard` layout.
    4. Verify that both tabs feel consistent in their visual style.
    5. Test with long show names and episode names to ensure ellipsis handling works correctly without breaking the layout.
