package com.alok.justrack.data.repository

import com.alok.justrack.data.db.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val watchlistDao: WatchlistDao,
    private val favouriteDao: FavouriteDao,
    private val listDao: ListDao,
    private val customImageDao: CustomImageDao,
    private val watchedEpisodeDao: WatchedEpisodeDao
) {

    suspend fun syncAll(): Result<Unit> = runCatching {
        val user = supabase.auth.currentUserOrNull() ?: return@runCatching
        val userId = user.id
        val currentTime = System.currentTimeMillis()

        // 1. Sync Watchlist
        val unsyncedWatchlist = watchlistDao.getUnsynced()
        if (unsyncedWatchlist.isNotEmpty()) {
            val toSync = unsyncedWatchlist.map { it.copy(userId = userId, lastSyncAt = currentTime) }
            supabase.postgrest.from("watchlist").upsert(toSync)
            watchlistDao.updateSyncStatus(toSync.map { it.id }, userId, currentTime)
        }

        // 2. Sync Favourites
        val unsyncedFavourites = favouriteDao.getUnsynced()
        if (unsyncedFavourites.isNotEmpty()) {
            val toSync = unsyncedFavourites.map { it.copy(userId = userId, lastSyncAt = currentTime) }
            supabase.postgrest.from("favourites").upsert(toSync)
            favouriteDao.updateSyncStatus(toSync.map { it.id }, userId, currentTime)
        }

        // 3. Sync Custom Lists
        val unsyncedLists = listDao.getUnsyncedLists()
        if (unsyncedLists.isNotEmpty()) {
            val toSync = unsyncedLists.map { it.copy(userId = userId, lastSyncAt = currentTime) }
            supabase.postgrest.from("custom_lists").upsert(toSync)
            listDao.updateListSyncStatus(toSync.map { it.id }, userId, currentTime)
        }

        // 4. Sync List Items
        val unsyncedItems = listDao.getUnsyncedItems()
        if (unsyncedItems.isNotEmpty()) {
            val toSync = unsyncedItems.map { it.copy(userId = userId, lastSyncAt = currentTime) }
            supabase.postgrest.from("list_items").upsert(toSync)
            listDao.updateItemSyncStatus(toSync.map { it.id }, userId, currentTime)
        }

        // 5. Sync Custom Images
        val unsyncedImages = customImageDao.getUnsynced()
        if (unsyncedImages.isNotEmpty()) {
            val toSync = unsyncedImages.map { it.copy(userId = userId, lastSyncAt = currentTime) }
            supabase.postgrest.from("custom_images").upsert(toSync)
            customImageDao.updateSyncStatus(toSync.map { it.mediaId }, userId, currentTime)
        }

        // 6. Sync Watched Episodes
        val unsyncedEpisodes = watchedEpisodeDao.getUnsynced()
        if (unsyncedEpisodes.isNotEmpty()) {
            val toSync = unsyncedEpisodes.map { it.copy(userId = userId, lastSyncAt = currentTime) }
            // Note: localId is auto-generated, but in Supabase we might want a different PK or include it
            supabase.postgrest.from("watched_episodes").upsert(toSync)
            watchedEpisodeDao.updateSyncStatus(toSync.map { it.localId }, userId, currentTime)
        }
    }
}
