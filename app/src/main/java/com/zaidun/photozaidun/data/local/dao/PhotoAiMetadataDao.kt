package com.zaidun.photozaidun.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zaidun.photozaidun.data.local.entity.PhotoAiMetadataEntity

@Dao
interface PhotoAiMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PhotoAiMetadataEntity)

    @Query("SELECT * FROM photo_ai_metadata WHERE fileName = :fileName LIMIT 1")
    suspend fun getMetadata(fileName: String): PhotoAiMetadataEntity?

    @Query("DELETE FROM photo_ai_metadata")
    suspend fun clearAll()
}
