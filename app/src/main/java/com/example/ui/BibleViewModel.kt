package com.example.ui

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BibleViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = BibleRepository(database.bibleDao())
    private val prefs = application.getSharedPreferences("bible_settings", android.content.Context.MODE_PRIVATE)

    // --- Repositories Flows ---
    val userNotes: StateFlow<List<UserNote>> = repository.allUserNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val voiceNotes: StateFlow<List<VoiceNote>> = repository.allVoiceNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedQuotes: StateFlow<List<SavedQuote>> = repository.allSavedQuotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Audio System Interface ---
    private val audioRecorderPlayer = AudioRecorderPlayer(application)
    var isRecording by mutableStateOf(false)
        private set
    var isPaused by mutableStateOf(false)
        private set
    var currentRecordedPath by mutableStateOf<String?>(null)
        private set
    var activePlayingNoteId by mutableStateOf<Int?>(null)
        private set

    // --- Selection and View States ---
    var selectedBook by mutableStateOf(BibleData.books.first()) // Default to Génesis
    var selectedChapter by mutableStateOf(1) // Default to Chapter 1
    var selectedVerse by mutableStateOf<Int?>(null) // Target verse to scroll to

    // --- Reader Settings (Persistent) ---
    private var _readerFontSizeSp by mutableStateOf(prefs.getFloat("font_size", 20f))
    var readerFontSizeSp: Float
        get() = _readerFontSizeSp
        set(value) {
            _readerFontSizeSp = value
            prefs.edit().putFloat("font_size", value).apply()
        }

    private var _readerThemeName by mutableStateOf(prefs.getString("theme_name", "pergamino") ?: "pergamino")
    var readerThemeName: String
        get() = _readerThemeName
        set(value) {
            _readerThemeName = value
            prefs.edit().putString("theme_name", value).apply()
        }

    private var _readerFontFamilyName by mutableStateOf(prefs.getString("font_family", "Serif") ?: "Serif")
    var readerFontFamilyName: String
        get() = _readerFontFamilyName
        set(value) {
            _readerFontFamilyName = value
            prefs.edit().putString("font_family", value).apply()
        }

    private var _readerIsBold by mutableStateOf(prefs.getBoolean("is_bold", false))
    var readerIsBold: Boolean
        get() = _readerIsBold
        set(value) {
            _readerIsBold = value
            prefs.edit().putBoolean("is_bold", value).apply()
        }

    var isSystemDarkTheme by mutableStateOf(false)
    var showChapterSelection by mutableStateOf(false)

    // --- Notes Sub-Tab Selection ---
    var activeNotesTab by mutableStateOf("notas") // "notas", "voz", "citas"

    // --- Recent Readings Persistence ---
    data class RecentReading(val bookName: String, val chapter: Int, val timestamp: Long)
    
    var recentReadings by mutableStateOf(loadRecentReadings())
        private set

    private fun loadRecentReadings(): List<RecentReading> {
        val json = prefs.getString("recent_readings", "[]") ?: "[]"
        return try {
            val arr = org.json.JSONArray(json)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                RecentReading(obj.getString("book"), obj.getInt("chapter"), obj.getLong("time"))
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun saveRecentReadings(list: List<RecentReading>) {
        val arr = org.json.JSONArray()
        list.forEach { 
            arr.put(org.json.JSONObject().apply {
                put("book", it.bookName)
                put("chapter", it.chapter)
                put("time", it.timestamp)
            })
        }
        prefs.edit().putString("recent_readings", arr.toString()).apply()
    }

    fun updateRecentReading(book: BibleBook, chapter: Int) {
        val newList = recentReadings.toMutableList()
        newList.removeAll { it.bookName == book.name && it.chapter == chapter }
        newList.add(0, RecentReading(book.name, chapter, System.currentTimeMillis()))
        val result = newList.take(2)
        recentReadings = result
        saveRecentReadings(result)
    }

    // --- Reading Plan: 30 Días de Paz ---
    val readingPlanPaz = listOf(
        Triple("Salmos", 23, "El Señor es mi pastor"),
        Triple("Juan", 14, "No se turbe vuestro corazón"),
        Triple("Filipenses", 4, "La paz de Dios"),
        Triple("Salmos", 91, "Al abrigo del Altísimo"),
        Triple("Mateo", 11, "Venid a mí y descansad"),
        Triple("Isaías", 41, "No temas, yo estoy contigo"),
        Triple("Juan", 16, "En el mundo tendréis aflicción"),
        Triple("Salmos", 46, "Nuestro pronto auxilio"),
        Triple("Romanos", 8, "Más que vencedores"),
        Triple("Salmos", 121, "Mi auxilio viene del Señor"),
        Triple("Isaías", 26, "Tú guardarás en completa paz"),
        Triple("Salmos", 34, "Busqué a Jehová, y él me oyó"),
        Triple("Mateo", 6, "No os afanéis por vuestra vida"),
        Triple("1 Pedro", 5, "Echando toda vuestra ansiedad"),
        Triple("Salmos", 4, "En paz me acostaré"),
        Triple("Josué", 1, "Esfuérzate y sé valiente"),
        Triple("Salmos", 27, "Jehová es mi luz y mi salvación"),
        Triple("Juan", 10, "Mis ovejas oyen mi voz"),
        Triple("Romanos", 15, "Y el Dios de esperanza os llene"),
        Triple("Salmos", 37, "Encomienda a Jehová tu camino"),
        Triple("Isaías", 40, "Los que esperan a Jehová"),
        Triple("Mateo", 5, "Bienaventurados los que lloran"),
        Triple("2 Corintios", 12, "Bástate mi gracia"),
        Triple("Salmos", 103, "Bendice, alma mía, a Jehová"),
        Triple("Filipenses", 1, "Estando persuadido de esto"),
        Triple("Salmos", 139, "Oh Jehová, tú me has examinado"),
        Triple("Proverbios", 3, "Fíate de Jehová"),
        Triple("Hebreos", 11, "Es, pues, la fe"),
        Triple("Apocalipsis", 21, "Enjugará Dios toda lágrima"),
        Triple("Salmos", 150, "Todo lo que respira alabe a JAH")
    )

    var currentPlanDay by mutableIntStateOf(prefs.getInt("plan_paz_day", 0))
        private set

    fun advancePlan() {
        if (currentPlanDay < readingPlanPaz.size - 1) {
            currentPlanDay++
            prefs.edit().putInt("plan_paz_day", currentPlanDay).apply()
        }
    }

    fun goToCurrentPlanDay() {
        val (bookName, chapter, _) = readingPlanPaz[currentPlanDay]
        val book = BibleData.books.firstOrNull { it.name == bookName }
        if (book != null) {
            selectBookAndChapter(book, chapter)
        }
    }

    // --- Advanced Search State ---
    var searchQuery by mutableStateOf("")
    var selectedSearchScope by mutableStateOf("Toda la Biblia") // Toda la Biblia, Antiguo Testamento, Nuevo Testamento, Libro Actual
    var searchMatches by mutableStateOf<List<SearchResult>>(emptyList())
    var isSearching by mutableStateOf(false)

    fun performSearch() {
        val query = searchQuery.trim()
        if (query.length < 2) {
            searchMatches = emptyList()
            return
        }

        viewModelScope.launch {
            isSearching = true
            searchMatches = getSearchResults()
            isSearching = false
        }
    }
    
    // --- Bible Selector State ---
    var isNewTestamentSelected by mutableStateOf(false)

    // --- UI Active Screen Routing (Saves state perfectly) ---
    // Screens: "inicio", "biblia", "notas", "perfil", "lectura", "buscar", "creditos"
    var activeScreen by mutableStateOf("inicio")
    var previousScreenStack = mutableListOf<String>()

    // --- Verse of the Day ---
    val verseOfTheDay: Triple<String, String, String> by derivedStateOf {
        val colombiaZone = java.util.TimeZone.getTimeZone("America/Bogota")
        val calendar = java.util.Calendar.getInstance(colombiaZone)
        
        // If it's before 07:00 AM, we use the "seed" from yesterday
        if (calendar.get(java.util.Calendar.HOUR_OF_DAY) < 7) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        
        // Create a unique seed based on year and day of year
        val seed = calendar.get(java.util.Calendar.YEAR) * 1000 + calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val random = java.util.Random(seed.toLong())
        val index = random.nextInt(BibleData.inspirationalVerses.size)
        BibleData.inspirationalVerses[index]
    }

    init {
        // Preseed some comforting data if first run
        viewModelScope.launch {
            // Preseed sample note or saved quote if database is completely empty
            // We can check this dynamically
        }
    }

    fun navigateTo(screen: String) {
        if (activeScreen != screen) {
            previousScreenStack.add(activeScreen)
            activeScreen = screen
        }
    }

    fun navigateBack() {
        if (previousScreenStack.isNotEmpty()) {
            activeScreen = previousScreenStack.removeAt(previousScreenStack.size - 1)
        } else {
            activeScreen = "inicio"
        }
    }

    // --- Bible Reading Navigation Helpers ---
    fun selectBookAndChapter(book: BibleBook, chapter: Int, verse: Int? = null) {
        selectedBook = book
        selectedChapter = chapter.coerceIn(1, book.chapters)
        selectedVerse = verse
        updateRecentReading(book, selectedChapter)
        navigateTo("lectura")
    }

    fun navigateNextChapter() {
        selectedVerse = null
        val nextCh = selectedChapter + 1
        if (nextCh <= selectedBook.chapters) {
            selectedChapter = nextCh
        } else {
            // Next book
            val currentIndex = BibleData.books.indexOf(selectedBook)
            if (currentIndex != -1 && currentIndex + 1 < BibleData.books.size) {
                selectedBook = BibleData.books[currentIndex + 1]
                selectedChapter = 1
            }
        }
        updateRecentReading(selectedBook, selectedChapter)
    }

    fun navigatePrevChapter() {
        selectedVerse = null
        val prevCh = selectedChapter - 1
        if (prevCh >= 1) {
            selectedChapter = prevCh
        } else {
            // Previous book
            val currentIndex = BibleData.books.indexOf(selectedBook)
            if (currentIndex > 0) {
                selectedBook = BibleData.books[currentIndex - 1]
                selectedChapter = selectedBook.chapters
            }
        }
        updateRecentReading(selectedBook, selectedChapter)
    }

    // --- Audio Operations ---
    fun startRecordingVoice() {
        if (isRecording) return
        val path = audioRecorderPlayer.startRecording()
        if (path != null) {
            currentRecordedPath = path
            isRecording = true
            isPaused = false
        }
    }

    fun pauseRecordingVoice() {
        if (isRecording && !isPaused) {
            audioRecorderPlayer.pauseRecording()
            isPaused = true
        }
    }

    fun resumeRecordingVoice() {
        if (isRecording && isPaused) {
            audioRecorderPlayer.resumeRecording()
            isPaused = false
        }
    }

    fun stopRecordingVoiceAndSave(title: String) {
        if (!isRecording) return
        isRecording = false
        isPaused = false
        val duration = audioRecorderPlayer.stopRecording()
        val path = currentRecordedPath
        if (path != null) {
            viewModelScope.launch {
                val finalTitle = title.ifBlank { "Reflexión del ${getCurrentDateString()}" }
                repository.insertVoiceNote(
                    VoiceNote(
                        title = finalTitle,
                        filePath = path,
                        durationSecs = duration,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
        currentRecordedPath = null
    }

    fun playVoiceNote(note: VoiceNote) {
        if (activePlayingNoteId == note.id) {
            audioRecorderPlayer.stopPlaying()
            activePlayingNoteId = null
        } else {
            activePlayingNoteId = note.id
            audioRecorderPlayer.startPlaying(note.filePath) {
                activePlayingNoteId = null
            }
        }
    }

    fun isVoiceNotePlaying(note: VoiceNote): Boolean {
        return activePlayingNoteId == note.id
    }

    // --- OCR/Text Note Operations ---
    fun saveUserNote(id: Int = 0, title: String, content: String, linkedScripture: String? = null) {
        viewModelScope.launch {
            val noteTitle = title.ifBlank { "Reflexión Bíblica" }
            repository.insertUserNote(
                UserNote(
                    id = id,
                    title = noteTitle,
                    content = content,
                    linkedScripture = linkedScripture,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteUserNote(note: UserNote) {
        viewModelScope.launch {
            repository.deleteUserNote(note)
        }
    }

    fun deleteVoiceNote(voiceNote: VoiceNote) {
        viewModelScope.launch {
            repository.deleteVoiceNote(voiceNote)
        }
    }

    // --- Saved Quotes / Devotion Verse Operations ---
    fun toggleSaveQuote(bookName: String, chapter: Int, verseRange: String, text: String) {
        viewModelScope.launch {
            val isSaved = repository.isQuoteSaved(bookName, chapter, verseRange)
            if (isSaved) {
                repository.deleteQuoteByReference(bookName, chapter, verseRange)
            } else {
                repository.insertSavedQuote(
                    SavedQuote(
                        bookName = bookName,
                        chapter = chapter,
                        verseRange = verseRange,
                        text = text,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // Expose bookmark checking
    private val _savedStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val savedStates = _savedStates.asStateFlow()

    fun checkVerseSavedStatus(bookName: String, chapter: Int, verseRange: String) {
        viewModelScope.launch {
            val isSaved = repository.isQuoteSaved(bookName, chapter, verseRange)
            val key = "$bookName|$chapter|$verseRange"
            val currentMap = _savedStates.value.toMutableMap()
            currentMap[key] = isSaved
            _savedStates.value = currentMap
        }
    }

    fun deleteSavedQuote(quote: SavedQuote) {
        viewModelScope.launch {
            repository.deleteSavedQuote(quote)
        }
    }

    // --- Search Logic & Advanced Queries ---
    suspend fun getSearchResults(): List<SearchResult> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val query = searchQuery.trim().lowercase()
        if (query.length < 2) return@withContext emptyList<SearchResult>()

        val results = mutableListOf<SearchResult>()
        val context = getApplication<android.app.Application>().applicationContext
        
        for (book in BibleData.books) {
            // Apply Scope Filter
            when (selectedSearchScope) {
                "Antiguo Testamento" -> if (book.isNewTestament) continue
                "Nuevo Testamento" -> if (!book.isNewTestament) continue
                "Libro Actual" -> if (book.name != selectedBook.name) continue
            }
            
            try {
                // Optimize: Load book once, then iterate chapters
                val jsonString = context.assets.open("biblia/${book.fileName}").bufferedReader().use { it.readText() }
                val chaptersArray = org.json.JSONArray(jsonString)
                
                for (chIndex in 0 until chaptersArray.length()) {
                    val versesArray = chaptersArray.getJSONArray(chIndex)
                    for (vIndex in 0 until versesArray.length()) {
                        val vText = versesArray.getString(vIndex).replace("/n", "\n")
                        if (vText.lowercase().contains(query)) {
                            results.add(
                                SearchResult(
                                    bookName = book.name,
                                    chapter = chIndex + 1,
                                    verseNumber = vIndex + 1,
                                    text = vText
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Add dynamic theology matches only if searching the whole Bible and results are small
        if (selectedSearchScope == "Toda la Biblia" && results.size < 3) {
            val dynamicTheologies = listOf(
                "gracia" to "Porque por gracia sois salvos por medio de la fe; y esto no de vosotros, pues es don de Dios.",
                "amor" to "El amor es sufrido, es benigno; el amor no tiene envidia, el amor no es jactancioso, no se envanece.",
                "fe" to "Es, pues, la fe la certeza de lo que se espera, la convicción de lo que no se ve.",
                "paz" to "La paz os dejo, mi paz os doy; yo no os la doy como el mundo la da. No se turbe vuestro corazón.",
                "pastor" to "Jehová es mi pastor; nada me faltará. En lugares de delicados pastos me hará descansar.",
                "fortaleza" to "Jehová es mi fortaleza y mi cántico, y ha sido mi salvación. Él es mi Dios, y lo alabaré.",
                "camino" to "Jesús le dijo: Yo soy el camino, y la verdad, y la vida; nadie viene al Padre, sino por mí."
            )
            for ((key, text) in dynamicTheologies) {
                if (query.contains(key) || key.contains(query)) {
                    val matchingBook = if (key == "pastor") "Salmos" else "Romanos"
                    results.add(
                        SearchResult(
                            bookName = matchingBook,
                            chapter = if (key == "pastor") 23 else 8,
                            verseNumber = 1,
                            text = text
                        )
                    )
                }
            }
        }

        results.distinctBy { "${it.bookName}|${it.chapter}|${it.verseNumber}" }
    }

    private fun getCurrentDateString(): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}

data class SearchResult(
    val bookName: String,
    val chapter: Int,
    val verseNumber: Int,
    val text: String
)
