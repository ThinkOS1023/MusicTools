package com.pink.musictools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pink.musictools.data.local.MusicDatabase
import com.pink.musictools.data.repository.MusicRepositoryImpl
import com.pink.musictools.domain.ArtworkStore
import com.pink.musictools.domain.FileScanner
import com.pink.musictools.domain.MetadataReader
import com.pink.musictools.domain.MetadataWriter
import com.pink.musictools.domain.MusicExporter
import com.pink.musictools.domain.PlaybackController
import com.pink.musictools.ui.screens.ImportScreen
import com.pink.musictools.ui.screens.MusicEditorScreen
import com.pink.musictools.ui.screens.MusicListScreen
import com.pink.musictools.ui.screens.MusicPlayerScreen
import com.pink.musictools.ui.theme.MusicToolsTheme
import com.pink.musictools.viewmodel.ImportViewModel
import com.pink.musictools.viewmodel.MusicEditorViewModel
import com.pink.musictools.viewmodel.MusicLibraryViewModel
import com.pink.musictools.viewmodel.MusicPlayerViewModel

class MainActivity : ComponentActivity() {

    private lateinit var musicPlayerViewModel: MusicPlayerViewModel
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel
    private lateinit var importViewModel: ImportViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = MusicDatabase.getInstance(applicationContext)
        val musicRepository = MusicRepositoryImpl(database.musicDao())
        val playbackController = PlaybackController(applicationContext)
        val fileScanner = FileScanner(applicationContext)
        val artworkStore = ArtworkStore(applicationContext)
        val metadataReader = MetadataReader(applicationContext, artworkStore)
        val metadataWriter = MetadataWriter(applicationContext)
        val musicExporter = MusicExporter(applicationContext, metadataWriter)

        musicPlayerViewModel = MusicPlayerViewModel(playbackController, musicRepository)
        musicLibraryViewModel = MusicLibraryViewModel(musicRepository, musicExporter)
        importViewModel = ImportViewModel(fileScanner, musicRepository, metadataReader)

        setContent {
            MusicToolsTheme {
                MusicPlayerApp(
                    musicPlayerViewModel = musicPlayerViewModel,
                    musicLibraryViewModel = musicLibraryViewModel,
                    importViewModel = importViewModel
                )
            }
        }
    }
}

sealed class Screen {
    object Player : Screen()
    object Library : Screen()
    object Import : Screen()
    data class Editor(val musicId: String) : Screen()
}

@Composable
fun MusicPlayerApp(
    musicPlayerViewModel: MusicPlayerViewModel,
    musicLibraryViewModel: MusicLibraryViewModel,
    importViewModel: ImportViewModel
) {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Player) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember(context) { MusicDatabase.getInstance(context) }
    val musicRepository = remember(database) { MusicRepositoryImpl(database.musicDao()) }
    val playbackController = remember { PlaybackController(context.applicationContext) }
    val artworkStore = remember { ArtworkStore(context.applicationContext) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = selectedScreen !is Screen.Editor,
                enter = slideInVertically(tween(280)) { it },
                exit = slideOutVertically(tween(200)) { it }
            ) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = "播放器") },
                        label = { Text("播放器") },
                        selected = selectedScreen == Screen.Player,
                        onClick = { selectedScreen = Screen.Player }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "音乐库") },
                        label = { Text("音乐库") },
                        selected = selectedScreen == Screen.Library,
                        onClick = { selectedScreen = Screen.Library }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Download, contentDescription = "导入") },
                        label = { Text("导入") },
                        selected = selectedScreen == Screen.Import,
                        onClick = { selectedScreen = Screen.Import }
                    )
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = selectedScreen,
            transitionSpec = {
                val enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 14 }
                val exit = fadeOut(tween(160))
                enter togetherWith exit
            },
            label = "screenTransition"
        ) { screen ->
            when (screen) {
                Screen.Player -> {
                    MusicPlayerScreen(
                        viewModel = musicPlayerViewModel,
                        modifier = Modifier.padding(paddingValues)
                    )
                }

                Screen.Library -> {
                    MusicListScreen(
                        libraryViewModel = musicLibraryViewModel,
                        playerViewModel = musicPlayerViewModel,
                        onNavigateToEditor = { musicId ->
                            selectedScreen = Screen.Editor(musicId)
                        },
                        onNavigateToPlayer = { selectedScreen = Screen.Player },
                        modifier = Modifier.padding(paddingValues)
                    )
                }

                Screen.Import -> {
                    ImportScreen(
                        viewModel = importViewModel,
                        modifier = Modifier.padding(paddingValues)
                    )
                }

                is Screen.Editor -> {
                    val editorViewModel = remember(screen.musicId) {
                        MusicEditorViewModel(
                            musicRepository = musicRepository,
                            playbackController = playbackController,
                            artworkStore = artworkStore
                        )
                    }
                    MusicEditorScreen(
                        musicId = screen.musicId,
                        viewModel = editorViewModel,
                        onNavigateBack = { selectedScreen = Screen.Library }
                    )
                }
            }
        }
    }
}
