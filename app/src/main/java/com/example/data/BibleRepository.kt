package com.example.data

import kotlinx.coroutines.flow.Flow

class BibleRepository(private val bibleDao: BibleDao) {

    val allUserNotes: Flow<List<UserNote>> = bibleDao.getAllUserNotes()
    val allVoiceNotes: Flow<List<VoiceNote>> = bibleDao.getAllVoiceNotes()
    val allSavedQuotes: Flow<List<SavedQuote>> = bibleDao.getAllSavedQuotes()

    suspend fun insertUserNote(note: UserNote) {
        bibleDao.insertUserNote(note)
    }

    suspend fun deleteUserNote(note: UserNote) {
        bibleDao.deleteUserNote(note)
    }

    suspend fun insertVoiceNote(voiceNote: VoiceNote) {
        bibleDao.insertVoiceNote(voiceNote)
    }

    suspend fun deleteVoiceNote(voiceNote: VoiceNote) {
        bibleDao.deleteVoiceNote(voiceNote)
    }

    suspend fun insertSavedQuote(quote: SavedQuote) {
        bibleDao.insertSavedQuote(quote)
    }

    suspend fun deleteSavedQuote(quote: SavedQuote) {
        bibleDao.deleteSavedQuote(quote)
    }

    suspend fun isQuoteSaved(bookName: String, chapter: Int, verseRange: String): Boolean {
        return bibleDao.isQuoteSaved(bookName, chapter, verseRange)
    }

    suspend fun deleteQuoteByReference(bookName: String, chapter: Int, verseRange: String) {
        bibleDao.deleteQuoteByReference(bookName, chapter, verseRange)
    }
}
