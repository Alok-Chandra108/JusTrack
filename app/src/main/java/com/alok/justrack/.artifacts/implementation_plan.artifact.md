# Implementation Plan - Season-level Watch Toggle & Clickable Checkmarks

Implement a feature to mark an entire season as watched/unwatched by clicking the checkmark icon on the season card.

## Proposed Changes

### Data Layer

#### [MODIFY] [WatchedEpisodeDao.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/data/db/WatchedEpisodeDao.kt)
- Add a `deleteSeason(showId: String, seasonNumber: Int)` method to remove all watched records for a specific season.

#### [MODIFY] [MediaRepository.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/data/repository/MediaRepository.kt)
- Add `markSeasonWatched(showId: String, seasonNumber: Int, watched: Boolean, episodes: List<Episode>)` method.

#### [MODIFY] [TmdbMediaRepository.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/data/repository/TmdbMediaRepository.kt)
- Implement `markSeasonWatched` using `insertAll` for watched status and the new `deleteSeason` for unwatched status.

### ViewModel

#### [MODIFY] [DetailViewModel.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/viewmodel/DetailViewModel.kt)
- Add `toggleSeasonWatched(season: Season)` function.
- It will determine the new status (if all are watched, set to unwatched; otherwise, set to watched) and call the repository.

### UI Components

#### [MODIFY] [MovieDetailsScreen.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/screens/MovieDetailsScreen.kt)
- **SeasonCard**:
    - Wrap the checkmark Icon in a clickable container (or make the icon itself clickable).
    - Update styling: When `isCompleted`, show a solid green circle background with a white checkmark.
    - Stop click propagation so clicking the checkmark doesn't expand/collapse the card.
- **DetailScreen**: Pass the `onSeasonWatchedToggle` callback from the ViewModel to `MovieDetailsScreen`.

## Verification Plan

### Manual Verification
- Deploy to the device.
- Navigate to a TV show "EPISODES" tab.
- Click the checkmark on a season card:
    - If some episodes were unwatched, all should now show as watched (green checkmark).
    - If all episodes were watched, all should now show as unwatched.
- Expand the season to verify that all individual episode checkmarks updated correctly.
