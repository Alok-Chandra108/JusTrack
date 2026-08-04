# Tasks - Fix Watchlist Button and Remove Continue Watching

- [x] Fix Watchlist Button State
    - [x] Update `TmdbMediaRepository.kt` to check `inWatchlist` flag
    - [x] Update `SupabaseMediaRepository.kt` to check `inWatchlist` flag
- [x] Remove "Continue Watching" Section
    - [x] Update `ExploreViewModel.kt` to remove `continueWatching` logic
    - [x] Update `ExploreScreen.kt` to remove the UI section
- [x] Verification
    - [x] Build project
    - [x] Verify watchlist button behavior
    - [x] Verify Explore screen layout
