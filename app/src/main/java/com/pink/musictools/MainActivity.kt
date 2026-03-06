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
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pink.musictools.data.local.AppPreferences
import com.pink.musictools.data.local.MusicDatabase
import com.pink.musictools.data.repository.MusicRepositoryImpl
import com.pink.musictools.domain.ArtworkStore
import com.pink.musictools.domain.FileScanner
import com.pink.musictools.domain.MetadataReader
import com.pink.musictools.domain.MetadataWriter
import com.pink.musictools.domain.MusicExporter
import com.pink.musictools.domain.PlaybackController
import com.pink.musictools.ui.components.FloatingNavBar
import com.pink.musictools.ui.screens.ImportScreen
import com.pink.musictools.ui.screens.MusicEditorScreen
import com.pink.musictools.ui.screens.MusicListScreen
import com.pink.musictools.ui.screens.MusicPlayerScreen
import com.pink.musictools.ui.screens.SettingsScreen
import com.pink.musictools.ui.theme.MusicToolsTheme
import com.pink.musictools.viewmodel.ImportViewModel
import com.pink.musictools.viewmodel.MusicEditorViewModel
import com.pink.musictools.viewmodel.MusicLibraryViewModel
import com.pink.musictools.viewmodel.MusicPlayerViewModel
import com.pink.musictools.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

class MainActivity : ComponentActivity() {

    private lateinit var musicPlayerViewModel: MusicPlayerViewModel
    private lateinit var musicLibraryViewModel: MusicLibraryViewModel
    private lateinit var importViewModel: ImportViewModel
    private lateinit var settingsViewModel: SettingsViewModel

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
        val appPreferences = AppPreferences(applicationContext)

        musicPlayerViewModel = MusicPlayerViewModel(playbackController, musicRepository)
        musicLibraryViewModel = MusicLibraryViewModel(musicRepository, musicExporter)
        importViewModel = ImportViewModel(fileScanner, musicRepository, metadataReader)
        settingsViewModel = SettingsViewModel(appPreferences)

        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val colorTheme by settingsViewModel.colorTheme.collectAsStateWithLifecycle()
            val dynamicColor by settingsViewModel.dynamicColor.collectAsStateWithLifecycle()

            MusicToolsTheme(
                themeMode = themeMode,
                colorTheme = colorTheme,
                dynamicColor = dynamicColor
            ) {
                MusicPlayerApp(
                    musicPlayerViewModel = musicPlayerViewModel,
                    musicLibraryViewModel = musicLibraryViewModel,
                    importViewModel = importViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

sealed class Screen {
    object Player : Screen()
    object Library : Screen()
    object Import : Screen()
    object Settings : Screen()
    data class Editor(val musicId: String) : Screen()
}

@Composable
fun MusicPlayerApp(
    musicPlayerViewModel: MusicPlayerViewModel,
    musicLibraryViewModel: MusicLibraryViewModel,
    importViewModel: ImportViewModel,
    settingsViewModel: SettingsViewModel
) {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Player) }

    // System back: always return to Player main screen
    BackHandler(enabled = selectedScreen != Screen.Player) {
        selectedScreen = Screen.Player
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember(context) { MusicDatabase.getInstance(context) }
    val musicRepository = remember(database) { MusicRepositoryImpl(database.musicDao()) }
    val playbackController = remember { PlaybackController(context.applicationContext) }
    val artworkStore = remember { ArtworkStore(context.applicationContext) }

    // Shared haze state: screen content is haze source, nav bar is hazeChild consumer
    val hazeState = remember { HazeState() }

    // Nav items definition
    val navItems = remember {
        listOf(
            Triple(Screen.Player,   Icons.Default.MusicNote,    "播放器"),
            Triple(Screen.Library,  Icons.Default.LibraryMusic, "音乐库"),
            Triple(Screen.Import,   Icons.Default.CloudDownload,"导入"),
            Triple(Screen.Settings, Icons.Default.Settings,     "设置")
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = selectedScreen !is Screen.Editor,
                enter = slideInVertically(tween(320)) { it } + fadeIn(tween(240)),
                exit  = slideOutVertically(tween(220)) { it } + fadeOut(tween(160))
            ) {
                FloatingNavBar(
                    items     = navItems,
                    selected  = selectedScreen,
                    hazeState = hazeState,
                    onSelect  = { selectedScreen = it }
                )
            }
        }
    ) { paddingValues ->
        // Wrap entire screen content as the haze source so the nav bar blurs it
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState)
        ) {
            AnimatedContent(
                targetState = selectedScreen,
                transitionSpec = {
                    val enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 14 }
                    val exit  = fadeOut(tween(160))
                    enter togetherWith exit
                },
                label = "screenTransition"
            ) { screen ->
                when (screen) {
                    Screen.Player -> MusicPlayerScreen(
                        viewModel = musicPlayerViewModel,
                        modifier  = Modifier.padding(paddingValues)
                    )

                    Screen.Library -> MusicListScreen(
                        libraryViewModel  = musicLibraryViewModel,
                        playerViewModel   = musicPlayerViewModel,
                        onNavigateToEditor = { selectedScreen = Screen.Editor(it) },
                        onNavigateToPlayer = { selectedScreen = Screen.Player },
                        modifier          = Modifier.padding(paddingValues)
                    )

                    Screen.Import -> ImportScreen(
                        viewModel = importViewModel,
                        modifier  = Modifier.padding(paddingValues)
                    )

                    Screen.Settings -> SettingsScreen(
                        viewModel = settingsViewModel,
                        modifier  = Modifier.padding(paddingValues)
                    )

                    is Screen.Editor -> {
                        val editorViewModel = remember(screen.musicId) {
                            MusicEditorViewModel(
                                musicRepository  = musicRepository,
                                playbackController = playbackController,
                                artworkStore     = artworkStore
                            )
                        }
                        MusicEditorScreen(
                            musicId        = screen.musicId,
                            viewModel      = editorViewModel,
                            onNavigateBack = { selectedScreen = Screen.Library }
                        )
                    }
                }
            }
        }
    }
}
