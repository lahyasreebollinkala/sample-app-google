package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DecisionDao {
    @Query("SELECT * FROM decisions ORDER BY timestamp DESC")
    fun getAllDecisions(): Flow<List<DecisionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(decision: DecisionEntity): Long

    @Query("DELETE FROM decisions WHERE id = :id")
    suspend fun deleteDecisionById(id: Int)

    @Query("UPDATE decisions SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFavorite: Boolean)

    @Query("SELECT * FROM decisions WHERE id = :id")
    suspend fun getDecisionById(id: Int): DecisionEntity?
}
