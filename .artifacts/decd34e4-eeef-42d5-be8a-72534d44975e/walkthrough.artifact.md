# Walkthrough - Fix Show Tab Card Layout

I have refactored the show tracking cards in the Watchlist screen to improve their layout, consistency, and visual polish.

## Changes Made

### 1. Improved `EpisodeTrackingCard`
- **Larger Image**: Increased the episode still image size to **120x68** (16:9 ratio) to better fit content and avoid aggressive vertical cropping when falling back to posters.
- **Better Spacing**: Added vertical padding to the card content to prevent neumorphic shadows from being clipped.
- **Refined Hierarchy**: Improved the text layout, using a clearer `S01 | E01` format and ensuring the episode title is prominent.
- **Larger Action Button**: Increased the size of the "Mark Watched" check button for better accessibility and shadow rendering.

### 2. Standardized `UpcomingEpisodeCard`
- **Visual Consistency**: Updated the upcoming card to share the same text styles and layout patterns as the tracking card.
- **Improved Countdown**: Enhanced the "Available Today" and countdown text visibility.

## Verification Results

### Automated Tests
- Ran `analyze_file` on `WatchlistShowsScreen.kt`. No syntax errors were found; only minor layout-related warnings.

### Manual Verification Required
- Please check the **Shows** tab on your device:
    - Verify that the **Watchlist** cards look balanced and that the check buttons aren't clipped at the top/bottom.
    - Verify that the **Upcoming** cards match the style of the watchlist cards.
    - Test with long episode names to ensure they truncate correctly without pushing other elements off-screen.
