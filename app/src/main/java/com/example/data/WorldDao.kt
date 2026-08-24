package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldDao {
    @Query("SELECT * FROM worlds ORDER BY lastPlayedTime DESC")
    fun getAllWorlds(): Flow<List<WorldEntity>>

    @Query("SELECT * FROM worlds WHERE id = :id")
    suspend fun getWorldById(id: String): WorldEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorld(world: WorldEntity)

    @Delete
    suspend fun deleteWorld(world: WorldEntity)

    @Query("DELETE FROM worlds WHERE id = :id")
    suspend fun deleteWorldById(id: String)

    // Mods
    @Query("SELECT * FROM mods")
    fun getAllMods(): Flow<List<ModEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMod(mod: ModEntity)

    @Update
    suspend fun updateMod(mod: ModEntity)

    @Delete
    suspend fun deleteMod(mod: ModEntity)
}
