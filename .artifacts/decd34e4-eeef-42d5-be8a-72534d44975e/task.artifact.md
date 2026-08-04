# Task List: Fix Watched Status and TV Show Wishlist Bug

- [ ] Data Layer Improvements
    - [ ] Update `SupabaseWatchlistItem` with `in_watchlist`
    - [ ] Update `TmdbMediaRepository` with robust TV show detection
    - [ ] Update `TmdbMediaRepository.setWatched` with auto-removal from wishlist
    - [ ] Implement `SupabaseMediaRepository` sync logic
- [ ] ViewModel Updates
    - [ ] Update `DetailViewModel` (case-insensitive mediaType, toggleWatched)
    - [ ] Update `ExploreViewModel` (toggleWatched, robust detection)
    - [ ] Update `WatchlistViewModel` (sorting)
- [ ] UI Adjustments
    - [ ] Update `ExploreScreen` (Long-press Mark Watched)
- [ ] Verification
    - [ ] Verify TV Show Wishlist sorting
    - [ ] Verify Mark Watched behavior (Search -> Profile)
    - [ ] Verify Undo Watched behavior (Delete vs Wishlist)
