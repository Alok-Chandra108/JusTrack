# Movie Description Page Fixes

This plan addresses the UI and data issues in the Movie Description page as reported by the user.

## User Review Required

> [!IMPORTANT]
> The reference images provided show a **Light Theme** UI, while the current codebase seems to have hardcoded Dark Theme colors (e.g., `Background = Color(0xFF111315)`). I will adjust the components to be more adaptive or match the light theme styling if that is the primary target.

## Proposed Changes

### [Data Layer]

#### [MODIFY] [TmdbDto.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/data/api/TmdbDto.kt)
- Add `release_dates` and `content_ratings` to `TmdbMediaDto`.
- Add `created_by` to `TmdbMediaDto`.
- Add necessary response classes for `ReleaseDates` and `ContentRatings`.

#### [MODIFY] [TmdbApiService.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/data/api/TmdbApiService.kt)
- Update `getMovieDetails` and `getTvDetails` to append `release_dates` and `content_ratings` to the response.

#### [MODIFY] [TmdbMediaRepository.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/data/repository/TmdbMediaRepository.kt)
- Update `toMovieDetails` to extract certification:
    - For Movies: Look for "US" in `release_dates`.
    - For TV: Look for "US" in `content_ratings`.
- Update `toMovieDetails` to set the director/creator name correctly:
    - Use `created_by` for TV shows if available.
    - Fallback to "Executive Producer" for TV or "Director" for Movies.

---

### [UI Layer]

#### [MODIFY] [PremiumComponents.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/components/PremiumComponents.kt)
- **`PosterInfoRow`**:
    - Update the "Directed by" label to "Created by" for TV shows.
    - Ensure certification text is visible and correctly styled.
- **`CollapsibleDescription`**:
    - Reduce internal padding from `20.dp` to `16.dp` or `12.dp` to make it look less "big".
    - Remove the ripple effect (circle) on click by using `indication = null` in the `clickable` modifier.
    - Adjust the background color to match the light-gray container seen in the reference.
- **`ActionButtons`**:
    - Ensure the toggle behavior (filled vs. outlined) and colors match the reference images.

#### [MODIFY] [MovieDetailsScreen.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/ui/screens/MovieDetailsScreen.kt)
- Ensure the overall page background and text colors are appropriate (handling Light/Dark theme if necessary).

## Verification Plan

### Manual Verification
1.  Deploy the app to a device/emulator.
2.  Open the details page for a Movie and a TV Show.
3.  Verify:
    -   Certification (e.g., PG, TV-MA) is shown.
    -   Director/Creator name is correct.
    -   Description area is more compact and has no ripple on click.
    -   Watchlist/Watched buttons toggle colors correctly.
