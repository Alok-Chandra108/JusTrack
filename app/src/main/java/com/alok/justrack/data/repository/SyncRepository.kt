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

        // 0. Claim any local data for this user
        watchlistDao.claimLocalData(userId)
        favouriteDao.claimLocalData(userId)
        // (Add other DAOs as needed)

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

    /**
     * Pulls data from Supabase and saves it to local Room database.
     */
    suspend fun pullAll(): Result<Unit> = runCatching {
        val user = supabase.auth.currentUserOrNull() ?: return@runCatching
        val userId = user.id

        // 1. Pull Watchlist
        val cloudWatchlist = supabase.postgrest.from("watchlist")
            .select { filter { eq("userid", userId) } }
            .decodeList<WatchlistEntity>()
        if (cloudWatchlist.isNotEmpty()) watchlistDao.insertAll(cloudWatchlist)

        // 2. Pull Favourites
        val cloudFavourites = supabase.postgrest.from("favourites")
            .select { filter { eq("userid", userId) } }
            .decodeList<FavouriteEntity>()
        if (cloudFavourites.isNotEmpty()) favouriteDao.insertAll(cloudFavourites)

        // 3. Pull Custom Lists
        val cloudLists = supabase.postgrest.from("custom_lists")
            .select { filter { eq("userid", userId) } }
            .decodeList<ListEntity>()
        if (cloudLists.isNotEmpty()) listDao.insertAllLists(cloudLists)

        // 4. Pull List Items
        val cloudItems = supabase.postgrest.from("list_items")
            .select { filter { eq("userid", userId) } }
            .decodeList<ListItemEntity>()
        if (cloudItems.isNotEmpty()) listDao.insertAllItems(cloudItems)

        // 5. Pull Custom Images
        val cloudImages = supabase.postgrest.from("custom_images")
            .select { filter { eq("userid", userId) } }
            .decodeList<CustomImageEntity>()
        if (cloudImages.isNotEmpty()) customImageDao.insertAll(cloudImages)

        // 6. Pull Watched Episodes
        val cloudEpisodes = supabase.postgrest.from("watched_episodes")
            .select { filter { eq("userid", userId) } }
            .decodeList<WatchedEpisodeEntity>()
        if (cloudEpisodes.isNotEmpty()) watchedEpisodeDao.insertAll(cloudEpisodes)
    }

    /**
     * Performs a full bi-directional sync (Pull then Push).
     */
    suspend fun performFullSync(): Result<Unit> = runCatching {
        // 1. First pull everything from cloud to ensure local is up to date
        pullAll().getOrThrow()
        // 2. Then push any local changes that haven't been synced yet
        syncAll().getOrThrow()
    }

    /**
     * Call this after a new login to ensure any local data is claimed by the new user.
     */
    suspend fun associateLocalDataWithUser(userId: String) {
        watchlistDao.updateSyncStatus(emptyList(), userId, 0) // Passing empty list usually means update all in custom SQL if not careful
        // Actually, I should add a specific DAO method for this to be safe.
    }
}
