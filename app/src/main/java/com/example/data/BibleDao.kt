package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BibleDao {
    // --- User Text Notes ---
    @Query("SELECT * FROM user_notes ORDER BY timestamp DESC")
    fun getAllUserNotes(): Flow<List<UserNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserNote(note: UserNote): Long

    @Delete
    suspend fun deleteUserNote(note: UserNote)

    // --- Voice Notes ---
    @Query("SELECT * FROM voice_notes ORDER BY timestamp DESC")
    fun getAllVoiceNotes(): Flow<List<VoiceNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceNote(voiceNote: VoiceNote): Long

    @Delete
    suspend fun deleteVoiceNote(voiceNote: VoiceNote)

    // --- Saved Quotes ---
    @Query("SELECT * FROM saved_quotes ORDER BY timestamp DESC")
    fun getAllSavedQuotes(): Flow<List<SavedQuote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedQuote(quote: SavedQuote): Long

    @Delete
    suspend fun deleteSavedQuote(quote: SavedQuote)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_quotes WHERE bookName = :bookName AND chapter = :chapter AND verseRange = :verseRange LIMIT 1)")
    suspend fun isQuoteSaved(bookName: String, chapter: Int, verseRange: String): Boolean

    @Query("DELETE FROM saved_quotes WHERE bookName = :bookName AND chapter = :chapter AND verseRange = :verseRange")
    suspend fun deleteQuoteByReference(bookName: String, chapter: Int, verseRange: String)
}
