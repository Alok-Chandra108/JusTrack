# Fix: Watched Status & TV Show Wishlist Bug

The user reported two main issues:
1.  Movies/Shows marked as "Watched" from search are not appearing in the Profile unless added to the wishlist first.
2.  TV Shows added to the wishlist are appearing under the "Movies" tab instead of the "Shows" tab.

This plan addresses these issues and incorporates user feedback on specific behaviors.

## User Review Required

> [!IMPORTANT]
> **Behavioral Change**: Marking a movie/show as "Watched" will now automatically remove it from the "Wishlist" (inWatchlist = false).
> **Data Integrity**: TV Show detection will be improved to prevent them from being misclassified as Movies.

## Proposed Changes

### 1. Data Layer & Repository

#### [MODIFY] [WatchlistEntity.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/data/db/WatchlistEntity.kt)
- No changes needed here, but verify `addedAt` is used correctly for sorting.

#### [MODIFY] [SupabaseWatchlistItem.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/data/supabase/SupabaseWatchlistItem.kt)
- [NEW] Add `in_watchlist` (boolean) to the serializable data class to match Room's entity.

#### [MODIFY] [TmdbMediaRepository.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/data/repository/TmdbMediaRepository.kt)
- **setWatched**: Update to handle new items. If `watched` is true, insert the item with `isWatched = true` and `inWatchlist = false`. If `watched` is false, delete the item if `inWatchlist` is also false.
- **Improved Detection**: Enhance `TmdbMediaDto.toMediaItem` and `toMovieDetails` to more reliably detect TV shows (checking `name` and `first_air_date`) when `media_type` is missing.
- **Sorting**: Ensure `getWatchlistFlow` and `getWatchlist` return items sorted by `addedAt` DESC.

#### [MODIFY] [SupabaseMediaRepository.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/data/repository/SupabaseMediaRepository.kt)
- Implement `setWatched` and `addToWatchlist` to sync with Supabase using the `in_watchlist` and `is_watched` flags.

### 2. ViewModel Layer

#### [MODIFY] [DetailViewModel.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/viewmodel/DetailViewModel.kt)
- **loadDetail**: Make `mediaType` string parsing case-insensitive (use `.uppercase()`).
- **toggleWatched**: Update to call `repository.setWatched(item, true)` which now handles wishlist removal.

#### [MODIFY] [ExploreViewModel.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/viewmodel/ExploreViewModel.kt)
- [NEW] Add `toggleWatched(item: MediaItem)` function.
- **Improved Detection**: Update `search` and `fetchTrending` mapping logic to be more robust for TV shows.

### 3. UI Layer

#### [MODIFY] [ExploreScreen.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/screens/ExploreScreen.kt)
- **ExploreLongPressSheet**: Update "Mark as Watched" option to call `viewModel.toggleWatched(item)` and update its label/icon based on the current watched status.

#### [MODIFY] [WatchlistViewModel.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/viewmodel/WatchlistViewModel.kt)
- Ensure all filtered flows (`watchedMovies`, `watchedShows`, `explicitWatchlistItems`) are correctly derived from the base `watchlistItems`.

## Verification Plan

### Manual Verification
1.  **TV Show Wishlist Bug**:
    *   Search for a TV Show (e.g., "The Bear").
    *   Add to Watchlist from search results.
    *   Verify it appears in the **Shows** tab, NOT the Movies tab.
2.  **Mark Watched from Search**:
    *   Search for a new movie.
    *   Long-press and click "Mark as Watched".
    *   Navigate to **Profile** and verify it's in the **Movies** section.
    *   Verify it is NOT in the **Movies -> WATCHLIST** tab.
3.  **Undo Watched**:
    *   Navigate to a watched movie's detail screen.
    *   Toggle "Watched" off.
    *   Verify it is removed from the Profile section and the database (since it wasn't in the wishlist).
4.  **Sorting**:
    *   Mark multiple movies as watched and verify they are sorted by most recently added first in the Profile section.
5.  **Supabase Sync**:
    *   Verify that `is_watched` and `in_watchlist` flags are correctly sent to Supabase (if possible to check network/logs).
