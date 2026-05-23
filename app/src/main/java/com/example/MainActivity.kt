package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.BibleViewModel
import com.example.ui.SearchResult
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val app = context.applicationContext as Application
            val viewModel: BibleViewModel = viewModel(factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(app))

            MyApplicationTheme(
                darkTheme = when (viewModel.readerThemeName) {
                    "noche" -> true
                    "pergamino" -> false
                    "sepia" -> false
                    else -> isSystemInDarkTheme()
                }
            ) {
                BibleApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BibleApp(viewModel: BibleViewModel) {
    val context = LocalContext.current
    var inputNoteTitle by remember { mutableStateOf("") }
    var inputNoteContent by remember { mutableStateOf("") }
    var inputLinkedScripture by remember { mutableStateOf("") }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var editingNoteId by remember { mutableIntStateOf(0) }
    var showViewNoteDialog by remember { mutableStateOf(false) }
    var noteToView by remember { mutableStateOf<UserNote?>(null) }
    var showScripturePickerInNote by remember { mutableStateOf(false) }
    var expandableFabActive by remember { mutableStateOf(false) }

    // Audio recording popup controller
    var showRecordingPopup by remember { mutableStateOf(false) }
    var voiceNoteTitle by remember { mutableStateOf("") }
    var recordingDurationSecs by remember { mutableStateOf(0) }

    // Launcher for requesting standard audio capture permissions.
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showRecordingPopup = true
            recordingDurationSecs = 0
            voiceNoteTitle = ""
        } else {
            Toast.makeText(context, "Permiso de micrófono denegado para grabar notas.", Toast.LENGTH_LONG).show()
        }
    }

    // Effect for active recording duration counter
    LaunchedEffect(viewModel.isRecording, viewModel.isPaused) {
        if (viewModel.isRecording && !viewModel.isPaused) {
            while (viewModel.isRecording && !viewModel.isPaused) {
                delay(1000L)
                recordingDurationSecs += 1
            }
        }
    }

    // Outer surface reflecting reader customization color values
    val surfaceColor = when (viewModel.readerThemeName) {
        "pergamino" -> Color(0xF2FFFDF0) // Crema muy claro traslúcido
        "sepia" -> Color(0xFFEBD8B7)     // Sepia café tradicional
        "noche" -> Color(0xFF1B1C1C)
        else -> MaterialTheme.colorScheme.background
    }

    val onSurfaceColor = when (viewModel.readerThemeName) {
        "pergamino" -> Color(0xFF2C2417)
        "sepia" -> Color(0xFF4B3621)     // Texto café oscuro
        "noche" -> Color(0xFFE4E2E1)
        else -> MaterialTheme.colorScheme.onBackground
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = surfaceColor,
        topBar = {
            Column {
                BibleTopAppBar(viewModel)
                if (viewModel.activeScreen == "lectura") {
                    ChapterTabs(viewModel)
                }
            }
        },
        bottomBar = {
            // Respect navigation safe areas natively
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavBarItem(
                        icon = Icons.Default.Home,
                        label = "Inicio",
                        isSelected = viewModel.activeScreen == "inicio",
                        onClick = { viewModel.navigateTo("inicio") }
                    )
                    NavBarItem(
                        icon = Icons.Default.Book,
                        label = "Biblia",
                        isSelected = viewModel.activeScreen == "biblia",
                        onClick = { viewModel.navigateTo("biblia") }
                    )
                    NavBarItem(
                        icon = Icons.Default.Create,
                        label = "Notas",
                        isSelected = viewModel.activeScreen == "notas",
                        onClick = { viewModel.navigateTo("notas") }
                    )
                    NavBarItem(
                        icon = Icons.Default.Settings,
                        label = "Ajustes",
                        isSelected = viewModel.activeScreen == "perfil",
                        onClick = { viewModel.navigateTo("perfil") }
                    )
                    NavBarItem(
                        icon = Icons.Default.Info,
                        label = "Créditos",
                        isSelected = viewModel.activeScreen == "creditos",
                        onClick = { viewModel.navigateTo("creditos") }
                    )
                }
            }
        },
        floatingActionButton = {
            if (viewModel.activeScreen == "notas") {
                Box(contentAlignment = Alignment.BottomEnd) {
                    if (expandableFabActive) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 76.dp)
                        ) {
                            // Record audio sub-button
                            FloatingActionButton(
                                onClick = {
                                    expandableFabActive = false
                                    // Check permission
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPermission) {
                                        showRecordingPopup = true
                                        recordingDurationSecs = 0
                                        voiceNoteTitle = ""
                                    } else {
                                        recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("record_voice_note_sub_fab")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Grabar Nota de Voz")
                            }

                            // Write text sub-button
                            FloatingActionButton(
                                onClick = {
                                    expandableFabActive = false
                                    editingNoteId = 0
                                    inputNoteTitle = ""
                                    inputNoteContent = ""
                                    inputLinkedScripture = ""
                                    showAddNoteDialog = true
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("write_text_note_sub_fab")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Escribir Nota de Texto")
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = { expandableFabActive = !expandableFabActive },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("main_add_fab")
                    ) {
                        Icon(
                            imageVector = if (expandableFabActive) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Desplegar opciones de notas",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Handle different transitions using highly reactive state checks
            AnimatedContent(
                targetState = viewModel.activeScreen,
                label = "ScreenTransitions",
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 6 }).togetherWith(fadeOut())
                }
            ) { screen ->
                when (screen) {
                    "inicio" -> HomeScreen(viewModel)
                    "biblia" -> BibleSelectorScreen(viewModel)
                    "notas" -> NotesAndSavedQuotesScreen(
                        viewModel = viewModel,
                        onNoteClick = { note ->
                            noteToView = note
                            showViewNoteDialog = true
                        },
                        onEditNote = { note ->
                            editingNoteId = note.id
                            inputNoteTitle = note.title
                            inputNoteContent = note.content
                            inputLinkedScripture = note.linkedScripture ?: ""
                            showAddNoteDialog = true
                        }
                    )
                    "perfil" -> ProfileAndConfigScreen(viewModel)
                    "lectura" -> ReadingCanvasScreen(viewModel, onSurfaceColor)
                    "buscar" -> SearchAdvancedScreen(viewModel)
                    "creditos" -> CreditsScreen(viewModel)
                    else -> HomeScreen(viewModel)
                }
            }

            // --- Note Insertion dialog ---
            if (showAddNoteDialog) {
                AlertDialog(
                    onDismissRequest = { showAddNoteDialog = false },
                    title = {
                        Text(
                            text = "Nueva Nota De Devoción",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = inputNoteTitle,
                                onValueChange = { inputNoteTitle = it },
                                label = { Text("Título") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = inputNoteContent,
                                onValueChange = { inputNoteContent = it },
                                label = { Text("¿Qué te dice Dios hoy?") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            )
                            OutlinedTextField(
                                value = inputLinkedScripture,
                                onValueChange = { inputLinkedScripture = it },
                                label = { Text("Cita vinculada (ej. Juan 3:16)") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { showScripturePickerInNote = true }) {
                                        Icon(Icons.Default.MenuBook, contentDescription = "Buscar en la Biblia", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.saveUserNote(
                                    id = editingNoteId,
                                    title = inputNoteTitle,
                                    content = inputNoteContent,
                                    linkedScripture = inputLinkedScripture.ifBlank { null }
                                )
                                showAddNoteDialog = false
                            }
                        ) {
                            Text(if (editingNoteId == 0) "Guardar Nota" else "Actualizar Nota")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddNoteDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // --- Note Viewing Dialog ---
            if (showViewNoteDialog && noteToView != null) {
                val note = noteToView!!
                AlertDialog(
                    onDismissRequest = { showViewNoteDialog = false },
                    title = {
                        Text(
                            text = note.title,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatTimestamp(note.timestamp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = note.content,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!note.linkedScripture.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = note.linkedScripture,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showViewNoteDialog = false }) {
                            Text("Cerrar")
                        }
                    }
                )
            }

            // --- Scripture Picker for Notes ---
            if (showScripturePickerInNote) {
                ScripturePickerDialog(
                    onDismiss = { showScripturePickerInNote = false },
                    onScriptureSelected = { scripture ->
                        inputLinkedScripture = scripture
                        showScripturePickerInNote = false
                    }
                )
            }

            // --- Recording Active Modal ---
            if (showRecordingPopup) {
                AlertDialog(
                    onDismissRequest = { 
                        if (!viewModel.isRecording) showRecordingPopup = false 
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .drawBehind {
                                        drawCircle(if (viewModel.isRecording && !viewModel.isPaused) Color.Red else Color.Gray)
                                    }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    !viewModel.isRecording -> "Nueva Nota de Voz"
                                    viewModel.isPaused -> "Grabación Pausada"
                                    else -> "Grabando Nota de Voz"
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = voiceNoteTitle,
                                onValueChange = { voiceNoteTitle = it },
                                label = { Text("Título de esta reflexión de voz") },
                                placeholder = { Text("Reflexión matutina hoy...") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !viewModel.isRecording
                            )

                            // Simulated recording wave animation / timer
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Mic",
                                    tint = if (viewModel.isRecording && !viewModel.isPaused) Color.Red else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = String.format("%02d:%02d", recordingDurationSecs / 60, recordingDurationSecs % 60),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (!viewModel.isRecording) {
                                    Button(
                                        onClick = { viewModel.startRecordingVoice() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Iniciar")
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            if (viewModel.isPaused) viewModel.resumeRecordingVoice()
                                            else viewModel.pauseRecordingVoice()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (viewModel.isPaused) MaterialTheme.colorScheme.primary else Color(0xFFFF9800)
                                        )
                                    ) {
                                        Icon(
                                            if (viewModel.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            contentDescription = null
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (viewModel.isPaused) "Reanudar" else "Pausar")
                                    }
                                }
                            }

                            Text(
                                "El archivo de audio se guardará de forma privada en el almacenamiento seguro de tu devocional.",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            enabled = viewModel.isRecording,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                            onClick = {
                                viewModel.stopRecordingVoiceAndSave(voiceNoteTitle)
                                showRecordingPopup = false
                                Toast.makeText(context, "Nota de voz guardada con éxito", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Detener y Guardar")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { 
                                if (viewModel.isRecording) {
                                    // Optionally ask to cancel or just stop without saving
                                    viewModel.stopRecordingVoiceAndSave(voiceNoteTitle)
                                }
                                showRecordingPopup = false 
                            }
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // --- Chapter Selection Modal ---
            if (viewModel.showChapterSelection) {
                ChapterSelectionDialog(viewModel)
            }
        }
    }
}

@Composable
fun ChapterTabs(viewModel: BibleViewModel) {
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        ScrollableTabRow(
            selectedTabIndex = viewModel.selectedChapter - 1,
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[viewModel.selectedChapter - 1]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            for (i in 1..viewModel.selectedBook.chapters) {
                Tab(
                    selected = viewModel.selectedChapter == i,
                    onClick = { viewModel.selectedChapter = i },
                    text = {
                        Text(
                            text = "Cap $i",
                            fontSize = 13.sp,
                            fontWeight = if (viewModel.selectedChapter == i) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }
    }
}

// --- Navigation Item helpers ---
@Composable
fun RowScope.NavBarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val contentDescription = "$label Tab Button"
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                    else Color.Transparent
                )
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// --- Dynamic Top App Bar ---
@Composable
fun BibleTopAppBar(viewModel: BibleViewModel) {
    Surface(
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (viewModel.activeScreen == "lectura" || viewModel.activeScreen == "buscar") {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (viewModel.activeScreen == "lectura") {
                        IconButton(onClick = { viewModel.showChapterSelection = true }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Seleccionar Capítulo",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    IconButton(onClick = { viewModel.navigateTo("perfil") }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu de opciones",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (viewModel.activeScreen == "lectura") viewModel.selectedBook.name else "Reina Valera 1960",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row {
                IconButton(onClick = { viewModel.navigateTo("perfil") }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Ajustar Tamaño de Fuente",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.navigateTo("buscar") }) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Buscador de versículos",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1: HOME (INICIO) SCREEN
// ==========================================
@Composable
fun HomeScreen(viewModel: BibleViewModel) {
    val context = LocalContext.current
    val savedQuotesList by viewModel.savedQuotes.collectAsState()
    val verseOfTheDay = viewModel.verseOfTheDay

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // App Logo Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // We use a Box with background color or image
                // Assuming the user saves the image as app_logo in drawables
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.app_logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .height(160.dp) // Solo definimos la altura
                        .wrapContentWidth() // El ancho se ajusta solo para no achatar
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "REINA VALERA 1960",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
            }
        }

        // Comforting Verse of the Day Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("verse_of_the_day_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VERSÍCULO DEL DÍA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Aesthetic Sparkle",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "“${verseOfTheDay.third}”",
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 18.sp,
                        lineHeight = 28.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${verseOfTheDay.first} ${verseOfTheDay.second}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Versículo del Día (RV 1960) - ${verseOfTheDay.first} ${verseOfTheDay.second}: \"${verseOfTheDay.third}\"")
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartir con"))
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Compartir versículo", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                viewModel.toggleSaveQuote(
                                    verseOfTheDay.first,
                                    verseOfTheDay.second.split(":")[0].toInt(),
                                    verseOfTheDay.second.split(":")[1],
                                    verseOfTheDay.third
                                )
                                Toast.makeText(context, "Versículo Guardado como Favorito", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Favorite, contentDescription = "Guardar en citas bíblicas", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }

        // Bento-Style Navigation Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Antiguo Testamento Buttons
                    BentoCard(
                        title = "Antiguo Testamento",
                        subtitle = "",
                        icon = Icons.Default.Book,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.isNewTestamentSelected = false
                            viewModel.navigateTo("biblia")
                        }
                    )
                    // Nuevo Testamento
                    BentoCard(
                        title = "Nuevo Testamento",
                        subtitle = "",
                        icon = Icons.Default.AutoStories,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.isNewTestamentSelected = true
                            viewModel.navigateTo("biblia")
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mis notas
                    BentoCard(
                        title = "Mis Notas",
                        subtitle = "",
                        icon = Icons.Default.EditNote,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("notas") }
                    )
                    // Citas bíblicas guardadas
                    BentoCard(
                        title = "Citas Guardadas",
                        subtitle = "",
                        icon = Icons.Default.Favorite,
                        iconTint = Color.Red,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("notas") }
                    )
                }
            }
        }

        // Dedicated Recommended reading banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // High-quality simulated background image placeholder using text/styling
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.2f),
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = "RECOMENDADO PARA TI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Plan: 30 Días de Paz",
                            fontFamily = FontFamily.Serif,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                // Jump directly to Salmo 23
                                val book = BibleData.books.first { it.name == "Salmos" }
                                viewModel.selectBookAndChapter(book, 23)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text("Continuar Lectura", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick Recent Reading
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Lectura Reciente",
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                RecentReadingRow(
                    abbr = "GN",
                    title = "Génesis 1:1-10",
                    subtitle = "El principio del mundo",
                    onClick = {
                        val book = BibleData.books.first { it.name.lowercase() == "génesis" }
                        viewModel.selectBookAndChapter(book, 1)
                    }
                )
                RecentReadingRow(
                    abbr = "JN",
                    title = "Juan 3:16",
                    subtitle = "Porque de tal manera amó Dios al mundo...",
                    onClick = {
                        val book = BibleData.books.first { it.name.lowercase() == "juan" }
                        viewModel.selectBookAndChapter(book, 3)
                    }
                )
            }
        }
    }
}

@Composable
fun BentoCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .aspectRatio(1.2f)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun RecentReadingRow(abbr: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = abbr,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "Leer libro", tint = MaterialTheme.colorScheme.primary)
    }
}


// ==========================================
// SCREEN 2: BIBLIA SELECTOR
// ==========================================
@Composable
fun BibleSelectorScreen(viewModel: BibleViewModel) {
    val filteredBooks = remember(viewModel.isNewTestamentSelected) {
        BibleData.books.filter { it.isNewTestament == viewModel.isNewTestamentSelected }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Seleccionar Libro",
            fontFamily = FontFamily.Serif,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Explora las sagradas escrituras",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Tab switches
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(25.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            Button(
                onClick = { viewModel.isNewTestamentSelected = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!viewModel.isNewTestamentSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = if (!viewModel.isNewTestamentSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = null,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Antiguo Testamento", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { viewModel.isNewTestamentSelected = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.isNewTestamentSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = if (viewModel.isNewTestamentSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = null,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Nuevo Testamento", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid selection of books
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredBooks) { book ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectBookAndChapter(book, 1) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = book.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Serif,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, size = 12.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${book.chapters} Capítulos",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// Custom extension
fun Modifier.size(size: Int): Modifier = this.size(size.dp)

@Composable
fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    androidx.compose.material3.Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier.size(size),
        tint = tint
    )
}


// ==========================================
// SCREEN 3: NOTES & BOOKMARKS (CITAS)
// ==========================================
@Composable
fun NotesAndSavedQuotesScreen(
    viewModel: BibleViewModel,
    onNoteClick: (UserNote) -> Unit,
    onEditNote: (UserNote) -> Unit
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableStateOf("notas") } // notas, citas, voz
    val userNotesList by viewModel.userNotes.collectAsState()
    val savedQuotesList by viewModel.savedQuotes.collectAsState()
    val voiceNotesList by viewModel.voiceNotes.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Mis Notas y Citas",
            fontFamily = FontFamily.Serif,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Fixed pills Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SubTabPill("Notas", activeSubTab == "notas", Modifier.weight(1f)) { activeSubTab = "notas" }
            SubTabPill("Voz 🎙️", activeSubTab == "voz", Modifier.weight(1f)) { activeSubTab = "voz" }
            SubTabPill("Citas", activeSubTab == "citas", Modifier.weight(1f)) { activeSubTab = "citas" }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            when (activeSubTab) {
                "notas" -> {
                    if (userNotesList.isEmpty()) {
                        item {
                            EmptyPlaceholderState(
                                icon = Icons.Default.EditNote,
                                text = "No tienes notas escritas aún.\nCrea una usando el botón + abajo."
                            )
                        }
                    } else {
                        items(userNotesList) { note ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNoteClick(note) }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatTimestamp(note.timestamp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row {
                                            IconButton(onClick = { onEditNote(note) }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Editar nota", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            }
                                            IconButton(onClick = { viewModel.deleteUserNote(note) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Eliminar nota", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = note.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = note.content,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!note.linkedScripture.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                // Intelligent parsing for: "Book Chapter:Verse" or "Book Chapter"
                                                val text = note.linkedScripture.trim()
                                                // Find the last space which usually separates book name and chapter
                                                val lastSpaceIndex = text.lastIndexOf(' ')
                                                if (lastSpaceIndex != -1) {
                                                    val bookName = text.substring(0, lastSpaceIndex).trim()
                                                    val refPart = text.substring(lastSpaceIndex + 1).trim()
                                                    
                                                    val matchedBook = BibleData.books.firstOrNull {
                                                        it.name.lowercase() == bookName.lowercase()
                                                    }
                                                    
                                                    if (matchedBook != null) {
                                                        // Parse chapter and verse
                                                        val chapter: Int
                                                        var verse: Int? = null
                                                        
                                                        if (refPart.contains(':')) {
                                                            val subParts = refPart.split(':')
                                                            chapter = subParts[0].toIntOrNull() ?: 1
                                                            verse = subParts[1].toIntOrNull()
                                                        } else {
                                                            chapter = refPart.toIntOrNull() ?: 1
                                                        }
                                                        
                                                        viewModel.selectBookAndChapter(matchedBook, chapter, verse)
                                                    }
                                                } else {
                                                    // Try matching full text as book name if no space
                                                    val matchedBook = BibleData.books.firstOrNull {
                                                        it.name.lowercase() == text.lowercase()
                                                    }
                                                    if (matchedBook != null) {
                                                        viewModel.selectBookAndChapter(matchedBook, 1)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Link, contentDescription = "Scripture link", size = 14.dp, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = note.linkedScripture,
                                                fontSize = 12.sp,
                                                fontStyle = FontStyle.Italic,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "voz" -> {
                    if (voiceNotesList.isEmpty()) {
                        item {
                            EmptyPlaceholderState(
                                icon = Icons.Default.Mic,
                                text = "No tienes notas de voz.\nCrea una nota de voz tocando + y luego el micrófono."
                            )
                        }
                    } else {
                        items(voiceNotesList) { voice ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Mic,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = voice.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteVoiceNote(voice) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar nota de voz", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Interactive Play button + Track bar
                                    val isPlaying = viewModel.isVoiceNotePlaying(voice)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.playVoiceNote(voice) },
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                                .size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Reproducir nota de voz",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Progress bar tracking
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(if (isPlaying) 0.5f else 0.0f)
                                                    .fillMaxHeight()
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Text(
                                            text = formatDuration(voice.durationSecs),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Guardado: " + formatTimestamp(voice.timestamp),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                "citas" -> {
                    if (savedQuotesList.isEmpty()) {
                        item {
                            EmptyPlaceholderState(
                                icon = Icons.Default.FavoriteBorder,
                                text = "Aún no tienes pasajes guardados.\nVe a la Biblia y presiona un versículo para guardarlo."
                            )
                        }
                    } else {
                        items(savedQuotesList) { quote ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${quote.bookName.uppercase()} ${quote.chapter}:${quote.verseRange}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            letterSpacing = 1.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(onClick = { viewModel.deleteSavedQuote(quote) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar de favoritos", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "“" + quote.text.replace("/n", "\n") + "”",
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        IconButton(onClick = {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "RV 1960 - ${quote.bookName} ${quote.chapter}:${quote.verseRange}: \"${quote.text}\"")
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Compartir con"))
                                        }) {
                                            Icon(Icons.Default.Share, contentDescription = "Compartir cita", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubTabPill(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyPlaceholderState(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = text,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// ==========================================
// SCREEN 4: PROFILE & CONFIG (AJUSTES)
// ==========================================
@Composable
fun ProfileAndConfigScreen(viewModel: BibleViewModel) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Configuración",
            fontFamily = FontFamily.Serif,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ajustes de lectura y tema",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Typography adjustments
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tamaño de Fuente",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FormatSize, contentDescription = "Menor fuente", size = 18.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = viewModel.readerFontSizeSp,
                        onValueChange = { viewModel.readerFontSizeSp = it },
                        valueRange = 14f..32f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                    Icon(Icons.Default.FormatSize, contentDescription = "Mayor fuente", size = 26.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "Vista previa del texto (${viewModel.readerFontSizeSp.toInt()} sp)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Tipo de Fuente",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val fonts = listOf("Serif", "SansSerif", "Monospace")
                    fonts.forEach { fontName ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (viewModel.readerFontFamilyName == fontName) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    1.dp,
                                    if (viewModel.readerFontFamilyName == fontName) Color.Transparent else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.readerFontFamilyName = fontName }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fontName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (viewModel.readerFontFamilyName == fontName) MaterialTheme.colorScheme.onPrimary
                                       else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = viewModel.readerIsBold,
                        onCheckedChange = { viewModel.readerIsBold = it }
                    )
                    Text("Texto en Negrita", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Live preview panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Jehová es mi pastor; nada me faltará.",
                        fontFamily = when(viewModel.readerFontFamilyName) {
                            "SansSerif" -> FontFamily.SansSerif
                            "Monospace" -> FontFamily.Monospace
                            else -> FontFamily.Serif
                        },
                        fontWeight = if (viewModel.readerIsBold) FontWeight.Bold else FontWeight.Normal,
                        fontSize = viewModel.readerFontSizeSp.sp,
                        lineHeight = (viewModel.readerFontSizeSp * 1.5f).sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Themes panel
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Temas de Lectura",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeGridButton(
                        name = "PERGAMINO",
                        bgColor = Color(0xFFFFFDF0),
                        borderColor = if (viewModel.readerThemeName == "pergamino") MaterialTheme.colorScheme.primary else Color.Transparent,
                        textColor = Color(0xFF2C2417),
                        onClick = { viewModel.readerThemeName = "pergamino" }
                    )
                    ThemeGridButton(
                        name = "SEPIA",
                        bgColor = Color(0xFFEBD8B7),
                        borderColor = if (viewModel.readerThemeName == "sepia") MaterialTheme.colorScheme.primary else Color.Transparent,
                        textColor = Color(0xFF4B3621),
                        onClick = { viewModel.readerThemeName = "sepia" }
                    )
                    ThemeGridButton(
                        name = "NOCHE",
                        bgColor = Color(0xFF1B1C1C),
                        borderColor = if (viewModel.readerThemeName == "noche") MaterialTheme.colorScheme.primary else Color.Transparent,
                        textColor = Color.White,
                        onClick = { viewModel.readerThemeName = "noche" }
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.ThemeGridButton(name: String, bgColor: Color, borderColor: Color, textColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}


// ==========================================
// SCREEN 5: READING CANVAS SCREEN
// ==========================================
@Composable
fun ReadingCanvasScreen(viewModel: BibleViewModel, onSurfaceColor: Color) {
    val initialPage = remember { BibleData.getGlobalIndex(viewModel.selectedBook.name, viewModel.selectedChapter) }
    val pagerState = rememberPagerState(initialPage = initialPage) { BibleData.totalChapters }

    // Sync pager to ViewModel (for external changes like chapter selection dialog)
    LaunchedEffect(viewModel.selectedBook, viewModel.selectedChapter) {
        val targetPage = BibleData.getGlobalIndex(viewModel.selectedBook.name, viewModel.selectedChapter)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Sync ViewModel to Pager (for swipes)
    LaunchedEffect(pagerState.currentPage) {
        val (book, chapter) = BibleData.getBookAndChapterFromIndex(pagerState.currentPage)
        // Update without triggering the LaunchedEffect above again if possible, 
        // though animateScrollToPage handles it if we check the current page.
        viewModel.selectedBook = book
        viewModel.selectedChapter = chapter
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1
    ) { page ->
        val (book, chapter) = BibleData.getBookAndChapterFromIndex(page)
        ChapterContent(book, chapter, viewModel, onSurfaceColor)
    }
}

@Composable
fun ChapterContent(book: BibleBook, chapter: Int, viewModel: BibleViewModel, onSurfaceColor: Color) {
    val context = LocalContext.current
    val verses = remember(book, chapter) {
        BibleData.getVerses(context, book.name, chapter)
    }

    val savedStates by viewModel.savedStates.collectAsState()
    val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Key to observe is saved state
    LaunchedEffect(book, chapter) {
        // Query bookmark status for verses so view updates automatically
        viewModel.checkVerseSavedStatus(book.name, chapter, "16")
        
        // Scroll to verse if target is set
        viewModel.selectedVerse?.let { target ->
            // Simple mapping: Header is index 0, verse 1 is index 1, etc.
            // But we have Spacers and Box...
            // Let's adjust the indices in LazyColumn
            scrollState.scrollToItem(target) 
        }
    }

    LazyColumn(
        state = scrollState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Scripture header
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LIBRO DE " + book.name.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Capítulo $chapter",
                    fontFamily = FontFamily.Serif,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    color = onSurfaceColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        items(verses) { (verseNum, verseText) ->
            // Check if this verse is JUAN 3:16 for visual spotlight
            val isJuanThreeSixteen = (book.name.lowercase() == "juan" && chapter == 3 && verseNum == 16)

            if (isJuanThreeSixteen) {
                // Feature visual spotlight block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "“" + verseText + "”",
                            fontFamily = when(viewModel.readerFontFamilyName) {
                                "SansSerif" -> FontFamily.SansSerif
                                "Monospace" -> FontFamily.Monospace
                                else -> FontFamily.Serif
                            },
                            fontStyle = FontStyle.Italic,
                            fontWeight = if (viewModel.readerIsBold) FontWeight.Bold else FontWeight.Normal,
                            fontSize = (viewModel.readerFontSizeSp + 2).sp,
                            lineHeight = ((viewModel.readerFontSizeSp + 2) * 1.55f).sp,
                            textAlign = TextAlign.Center,
                            color = onSurfaceColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "JUAN 3:16",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            } else {
                // Standard inline verse
                val key = "${book.name}|$chapter|$verseNum"
                val isBookmarked = savedStates[key] == true

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.toggleSaveQuote(
                                book.name,
                                chapter,
                                verseNum.toString(),
                                verseText
                            )
                            viewModel.checkVerseSavedStatus(book.name, chapter, verseNum.toString())
                            Toast.makeText(
                                context,
                                if (!isBookmarked) "Versículo Guardado" else "Versículo Eliminado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Gold superscript verse number
                    Text(
                        text = "$verseNum ",
                        fontSize = (viewModel.readerFontSizeSp * 0.7f).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp, top = 2.dp)
                    )

                    Text(
                        text = verseText,
                        fontFamily = when(viewModel.readerFontFamilyName) {
                            "SansSerif" -> FontFamily.SansSerif
                            "Monospace" -> FontFamily.Monospace
                            else -> FontFamily.Serif
                        },
                        fontWeight = if (viewModel.readerIsBold) FontWeight.Bold else FontWeight.Normal,
                        fontSize = viewModel.readerFontSizeSp.sp,
                        lineHeight = (viewModel.readerFontSizeSp * 1.5f).sp,
                        color = onSurfaceColor,
                        modifier = Modifier.weight(1f)
                    )

                    if (isBookmarked) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Guardado",
                            tint = Color.Red,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        // Inter-chapter footer controls
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { viewModel.navigatePrevChapter() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Anterior", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = { viewModel.navigateNextChapter() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Siguiente", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }
}



// ==========================================
// SCREEN 6: ADVANCED SEARCH SCREEN
// ==========================================
@Composable
fun SearchAdvancedScreen(viewModel: BibleViewModel) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var inputQuery by remember { mutableStateOf("") }
    var filterShowingBook by remember { mutableStateOf<String?>(null) }
    var matches by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Recompute results when query changes
    LaunchedEffect(inputQuery, viewModel.selectedSearchScope) {
        if (inputQuery.trim().length >= 2) {
            isSearching = true
            matches = viewModel.getSearchResults()
            isSearching = false
        } else {
            matches = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Búsqueda Avanzada",
            fontFamily = FontFamily.Serif,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Busca pasajes por palabras clave u oraciones",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Search text-field
        OutlinedTextField(
            value = inputQuery,
            onValueChange = {
                inputQuery = it
                viewModel.searchQuery = it
            },
            placeholder = { Text("Escribe para buscar... (ej. pastor, amor)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (inputQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        inputQuery = ""
                        viewModel.searchQuery = ""
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Scope selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val scopes = listOf("Toda la Biblia", "Antiguo Testamento", "Nuevo Testamento", "Libro Actual")
            scopes.forEach { scope ->
                SubTabPill(
                    text = if (scope == "Libro Actual") "Libro: ${viewModel.selectedBook.name}" else scope,
                    isSelected = viewModel.selectedSearchScope == scope,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    viewModel.selectedSearchScope = scope
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Match counter
        if (inputQuery.trim().length >= 2) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isSearching) "Buscando coincidencias..." else "Se encontraron ${matches.size} concordancias",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (isSearching) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (!isSearching && matches.isEmpty() && inputQuery.length >= 2) {
                item {
                    EmptyPlaceholderState(
                        icon = Icons.Default.SearchOff,
                        text = "No se encontraron versículos que coincidan con la búsqueda.\nIntenta con otras palabras."
                    )
                }
            } else if (inputQuery.trim().length < 2) {
                item {
                    EmptyPlaceholderState(
                        icon = Icons.Default.AutoStories,
                        text = "Ingresa al menos 2 letras para empezar la búsqueda avanzada."
                    )
                }
            } else {
                items(matches) { match ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val book = BibleData.books.firstOrNull { it.name == match.bookName }
                                if (book != null) {
                                    viewModel.selectBookAndChapter(book, match.chapter)
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "${match.bookName.uppercase()} ${match.chapter}:${match.verseNumber}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = match.text,
                                fontFamily = FontFamily.Serif,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreditsScreen(viewModel: BibleViewModel) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Créditos",
            fontFamily = FontFamily.Serif,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Información del desarrollador",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Esta App fue desarrollada por Luis Santos en Colombia. Permite la navegación por los libros de la Biblia, guardado de notas, grabación de notas de audio para predicaciones y reflexión, marcador de pasajes bíblicos y búsqueda, completamente Offline.",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                Text(
                    text = "Contacto",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/573106324241"))
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "WhatsApp",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "+57-3106324241 (WhatsApp)", fontSize = 16.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:lasaro069@gmail.com"))
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = "Correo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "lasaro069@gmail.com", fontSize = 16.sp)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/17aFdl2WPNGrsYni5L9UOWhB-x544Pv5x/view?usp=drive_link"))
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = "Privacidad",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Política de Privacidad",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "Versión 1.1.3",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ==========================================
// DIALOGS
// ==========================================
@Composable
fun ChapterSelectionDialog(viewModel: BibleViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.showChapterSelection = false },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentIndex = BibleData.books.indexOf(viewModel.selectedBook)

                IconButton(
                    onClick = {
                        if (currentIndex > 0) {
                            viewModel.selectedBook = BibleData.books[currentIndex - 1]
                            viewModel.selectedChapter = 1
                        }
                    },
                    enabled = currentIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Libro anterior",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = viewModel.selectedBook.name,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (currentIndex < BibleData.books.size - 1) {
                            viewModel.selectedBook = BibleData.books[currentIndex + 1]
                            viewModel.selectedChapter = 1
                        }
                    },
                    enabled = currentIndex < BibleData.books.size - 1
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Libro siguiente",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(viewModel.selectedBook.chapters) { index ->
                        val chapterNum = index + 1
                        val isSelected = viewModel.selectedChapter == chapterNum
                        
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    viewModel.selectedChapter = chapterNum
                                    viewModel.showChapterSelection = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chapterNum.toString(),
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.showChapterSelection = false }) {
                Text("Cerrar")
            }
        }
    )
}


@Composable
fun ScripturePickerDialog(onDismiss: () -> Unit, onScriptureSelected: (String) -> Unit) {
    val context = LocalContext.current
    var selectedBook by remember { mutableStateOf<BibleBook?>(null) }
    var selectedChapter by remember { mutableStateOf<Int?>(null) }
    var selectedVerse by remember { mutableStateOf<Int?>(null) }

    val verseCount = remember(selectedBook, selectedChapter) {
        if (selectedBook != null && selectedChapter != null) {
            BibleData.getVerses(context, selectedBook!!.name, selectedChapter!!).size
        } else 0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when {
                    selectedBook == null -> "Seleccionar Libro"
                    selectedChapter == null -> "Capítulo de ${selectedBook?.name}"
                    selectedVerse == null -> "Versículo de ${selectedBook?.name} ${selectedChapter}"
                    else -> "Confirmar Cita"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                when {
                    selectedBook == null -> {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(BibleData.books) { book ->
                                Text(
                                    text = book.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedBook = book }
                                        .padding(16.dp),
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                    selectedChapter == null -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedBook?.chapters ?: 0) { index ->
                                val ch = index + 1
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { selectedChapter = ch },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(ch.toString(), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    selectedVerse == null -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(verseCount) { index ->
                                val v = index + 1
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { selectedVerse = v },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(v.toString(), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = "¿Vincular ${selectedBook?.name} ${selectedChapter}:${selectedVerse}?",
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (selectedBook != null && selectedChapter != null && selectedVerse != null) {
                Button(onClick = { onScriptureSelected("${selectedBook?.name} ${selectedChapter}:${selectedVerse}") }) {
                    Text("Seleccionar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                when {
                    selectedVerse != null -> selectedVerse = null
                    selectedChapter != null -> selectedChapter = null
                    selectedBook != null -> selectedBook = null
                    else -> onDismiss()
                }
            }) {
                Text(if (selectedBook == null) "Cancelar" else "Atrás")
            }
        }
    )
}

// ==========================================
// STRING / TIMING HELPERS
// ==========================================
private fun formatTimestamp(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(millis)).uppercase()
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
