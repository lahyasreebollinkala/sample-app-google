package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decisions")
data class DecisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val question: String,
    val context: String,
    val analysisType: String, // "ALL", "PROS_CONS", "COMPARISON", "SWOT"
    val jsonResult: String,   // Store the JSON analysis string
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
