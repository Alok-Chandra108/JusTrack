package com.alok.justrack.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {

    @Query("SELECT * FROM custom_lists ORDER BY position ASC, createdAt DESC")
    fun getAllListsFlow(): Flow<List<ListEntity>>

    @Query("SELECT * FROM custom_lists ORDER BY position ASC, createdAt DESC")
    suspend fun getAllLists(): List<ListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createList(entity: ListEntity)

    @Query("UPDATE custom_lists SET name = :name WHERE id = :listId")
    suspend fun updateListName(listId: String, name: String)

    @Query("UPDATE custom_lists SET position = :position WHERE id = :listId")
    suspend fun updateListPosition(listId: String, position: Int)

    @Query("DELETE FROM custom_lists WHERE id = :listId")
    suspend fun deleteList(listId: String)

    @Query("DELETE FROM list_items WHERE listId = :listId")
    suspend fun deleteListItems(listId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addItem(entity: ListItemEntity)

    @Query("DELETE FROM list_items WHERE listId = :listId AND mediaId = :mediaId AND mediaType = :mediaType")
    suspend fun removeItem(listId: String, mediaId: String, mediaType: String)

    @Query("SELECT * FROM list_items WHERE listId = :listId ORDER BY addedAt ASC")
    fun getListItemsFlow(listId: String): Flow<List<ListItemEntity>>

    @Query("SELECT * FROM list_items WHERE listId = :listId ORDER BY addedAt ASC")
    suspend fun getListItems(listId: String): List<ListItemEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM list_items WHERE listId = :listId AND mediaId = :mediaId AND mediaType = :mediaType LIMIT 1)")
    suspend fun isInList(listId: String, mediaId: String, mediaType: String): Boolean

    @Query("SELECT DISTINCT listId FROM list_items WHERE mediaId = :mediaId AND mediaType = :mediaType")
    suspend fun getListsForMedia(mediaId: String, mediaType: String): List<String>
}
