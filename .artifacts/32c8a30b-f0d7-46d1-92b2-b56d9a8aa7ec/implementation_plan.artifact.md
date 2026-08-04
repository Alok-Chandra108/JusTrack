# Fix missing methods in TmdbMediaRepository

The `TmdbMediaRepository` class is currently missing most of the methods required by the `MediaRepository` interface, which is causing a compilation error. This likely happened due to an incomplete file update in a previous session. I will reconstruct the class with all required implementations.

## Proposed Changes

### [Component Name]

#### [MODIFY] [TmdbMediaRepository.kt](file:///C:/Users/Alok Chandra/AndroidStudioProjects/JusTrack/app/src/main/java/com/alok/justrack/data/repository/TmdbMediaRepository.kt)

I will implement all methods from the `MediaRepository` interface:
- **Network calls**: `getTrending`, `getMediaDetail`, `searchMedia`, `getSeasonDetails`, `getMovieImages`, `getTvImages` using `TmdbApiService`.
- **Local persistence (Room)**: Watchlist, Favourites, Custom Lists, and Episode tracking using the respective DAOs.
- **Mappers**: Add private mapper functions to convert between TMDb DTOs, Room Entities, and Domain Models (`MediaItem`, `MovieDetails`, etc.).

## Verification Plan

### Automated Tests
- I will run `./gradlew :app:compileDebugKotlin` to ensure the compilation error is resolved.

### Manual Verification
- Once compiled, the app should be able to fetch trending media, search, view details, and manage the watchlist/favourites locally.
