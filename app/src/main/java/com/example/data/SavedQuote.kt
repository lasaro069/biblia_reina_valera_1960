package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_quotes")
data class SavedQuote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookName: String,
    val chapter: Int,
    val verseRange: String, // e.g. "1-2" or "16"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
