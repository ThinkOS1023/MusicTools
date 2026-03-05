package com.pink.musictools.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.abs
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pink.musictools.data.model.PlaybackProgress
import com.pink.musictools.domain.LyricsFormatter
import com.pink.musictools.viewmodel.MusicEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicEditorScreen(
    musicId: String,
    viewModel: MusicEditorViewModel,
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val currentMusic by viewModel.currentMusic.collectAsState()
    val editorStatus by viewModel.editorStatus.collectAsState()
    val title by viewModel.title.collectAsState()
    val artist by viewModel.artist.collectAsState()
    val album by viewModel.album.collectAsState()
    val albumArtUri by viewModel.albumArtUri.collectAsState()
    val lyricsText by viewModel.lyricsText.collectAsState()
    val currentLineIndex by viewModel.currentLineIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val hasNext by viewModel.hasNext.collectAsState()
    val hasPrevious by viewModel.hasPrevious.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showLyrics by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        viewModel.updateAlbumArt(uri)
    }

    val lyricsImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val content = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                }.getOrNull()
            }
            if (content.isNullOrBlank()) {
                snackbarHostState.showSnackbar("歌词导入失败")
            } else {
                viewModel.importLyrics(content)
                snackbarHostState.showSnackbar("导入成功")
            }
        }
    }

    val lyricsExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val content = viewModel.exportLyrics()
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)
                        ?.bufferedWriter(Charsets.UTF_8)?.use { it.write(content) }
                    true
                }.getOrDefault(false)
            }
            snackbarHostState.showSnackbar(if (ok) "LRC 已导出" else "导出失败")
        }
    }

    LaunchedEffect(musicId) { viewModel.loadMusic(musicId) }

    LaunchedEffect(editorStatus) {
        if (editorStatus is MusicEditorViewModel.EditorStatus.Success) {
            kotlinx.coroutines.delay(1200)
            viewModel.resetStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showLyrics) "歌词打轴" else currentMusic?.title.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // Tab toggle
                    TextButton(onClick = { showLyrics = !showLyrics }) {
                        Text(if (showLyrics) "信息" else "歌词")
                    }

                    if (showLyrics) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, "更多")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("导入歌词") },
                                    leadingIcon = { Icon(Icons.Default.FileUpload, null) },
                                    onClick = {
                                        showMenu = false
                                        lyricsImportLauncher.launch(
                                            arrayOf("text/plain", "application/octet-stream")
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("导出 LRC") },
                                    leadingIcon = { Icon(Icons.Default.Download, null) },
                                    onClick = {
                                        showMenu = false
                                        val safeTitle = title.ifBlank { "lyrics" }
                                            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                                        lyricsExportLauncher.launch("$safeTitle.lrc")
                                    }
                                )
                                if (hasPrevious || hasNext) {
                                    HorizontalDivider()
                                    if (hasPrevious) DropdownMenuItem(
                                        text = { Text("上一首") },
                                        leadingIcon = { Icon(Icons.Default.SkipPrevious, null) },
                                        onClick = { showMenu = false; viewModel.loadPreviousMusic() }
                                    )
                                    if (hasNext) DropdownMenuItem(
                                        text = { Text("下一首") },
                                        leadingIcon = { Icon(Icons.Default.SkipNext, null) },
                                        onClick = { showMenu = false; viewModel.loadNextMusic() }
                                    )
                                }
                            }
                        }
                    }

                    // Save
                    IconButton(
                        onClick = { viewModel.saveChanges() },
                        enabled = editorStatus !is MusicEditorViewModel.EditorStatus.Saving
                    ) {
                        if (editorStatus is MusicEditorViewModel.EditorStatus.Saving) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, "保存")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = showLyrics,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                },
                label = "editorMode"
            ) { lyrics ->
                if (lyrics) {
                    LyricsEditor(
                        lyricsText = lyricsText,
                        currentLineIndex = currentLineIndex,
                        currentPosition = currentPosition,
                        progress = progress,
                        isPlaying = isPlaying,
                        onLyricsTextChange = viewModel::updateLyricsText,
                        onStamp = viewModel::stampCurrentLine,
                        onAdjust = viewModel::adjustCurrentLineTimestamp,
                        onPreviousLine = viewModel::previousLine,
                        onNextLine = viewModel::nextLine,
                        onSetCurrentLine = viewModel::setCurrentLine,
                        onSeekToLineTimestamp = viewModel::seekToLineTimestamp,
                        onTogglePlay = viewModel::togglePlayPause,
                        onSeekTo = viewModel::seekTo
                    )
                } else {
                    InfoEditor(
                        title = title,
                        artist = artist,
                        album = album,
                        albumArtUri = albumArtUri,
                        onTitleChange = viewModel::updateTitle,
                        onArtistChange = viewModel::updateArtist,
                        onAlbumChange = viewModel::updateAlbum,
                        onAlbumArtClick = { imagePickerLauncher.launch(arrayOf("image/*")) }
                    )
                }
            }

            // Status toast
            AnimatedVisibility(
                visible = editorStatus is MusicEditorViewModel.EditorStatus.Success ||
                        editorStatus is MusicEditorViewModel.EditorStatus.Error,
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 2 },
                exit = fadeOut(tween(180))
            ) {
                val isError = editorStatus is MusicEditorViewModel.EditorStatus.Error
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (isError) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.primary,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (isError) Icons.Default.Error else Icons.Default.Check,
                            null,
                            tint = if (isError) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = if (isError)
                                (editorStatus as MusicEditorViewModel.EditorStatus.Error).message
                            else "已保存",
                            color = if (isError) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoEditor(
    title: String,
    artist: String,
    album: String,
    albumArtUri: Uri?,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onAlbumChange: (String) -> Unit,
    onAlbumArtClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cover art
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onAlbumArtClick),
            contentAlignment = Alignment.Center
        ) {
            if (albumArtUri != null) {
                AsyncImage(
                    model = albumArtUri,
                    contentDescription = "封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp))
            }
        }

        OutlinedTextField(
            value = title, onValueChange = onTitleChange,
            label = { Text("标题") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = artist, onValueChange = onArtistChange,
            label = { Text("艺术家") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = album, onValueChange = onAlbumChange,
            label = { Text("专辑") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun LyricsEditor(
    lyricsText: String,
    currentLineIndex: Int,
    currentPosition: Long,
    progress: PlaybackProgress,
    isPlaying: Boolean,
    onLyricsTextChange: (String) -> Unit,
    onStamp: () -> Unit,
    onAdjust: (Long) -> Unit,
    onPreviousLine: () -> Unit,
    onNextLine: () -> Unit,
    onSetCurrentLine: (Int) -> Unit,
    onSeekToLineTimestamp: (Int) -> Unit,
    onTogglePlay: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    val lines = remember(lyricsText) { lyricsText.lines() }
    val listState = rememberLazyListState()
    // 0 = stamp mode (line list), 1 = edit mode (text field)
    var tabIndex by remember { mutableStateOf(0) }


    LaunchedEffect(currentLineIndex, tabIndex) {
        if (tabIndex == 0 && lines.isNotEmpty()) {
            listState.animateScrollToItem(currentLineIndex.coerceIn(0, lines.lastIndex))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Playback strip ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = formatTime(currentPosition),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
                Slider(
                    value = progress.progressPercentage,
                    onValueChange = { pct ->
                        onSeekTo((pct * progress.duration).toLong())
                    },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatTimeShort(progress.duration),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onTogglePlay, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null, modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ── Tab row ─────────────────────────────────────────────────────
        TabRow(selectedTabIndex = tabIndex, containerColor = MaterialTheme.colorScheme.surface) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 },
                text = { Text("打轴") })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 },
                text = { Text("编辑文字") })
        }

        // ── Lyrics preview strip — follows playback time, shows all lines ──
        if (lines.any { it.isNotBlank() }) {
            // Find the last raw line whose timestamp <= currentPosition;
            // fall back to currentLineIndex when no timestamps exist yet.
            val previewIndex = run {
                var result = -1
                for (i in lines.indices) {
                    val ts = LyricsFormatter.parseFirstTimestampMillis(lines[i])
                    if (ts != null && ts <= currentPosition) result = i
                }
                if (result >= 0) result else currentLineIndex
            }
            LyricsPreviewStrip(lines = lines, currentIndex = previewIndex)
            HorizontalDivider()
        }

        // ── Content ─────────────────────────────────────────────────────
        when (tabIndex) {
            // Stamp mode: scrollable line list
            0 -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    itemsIndexed(lines) { index, line ->
                        val isCurrent = index == currentLineIndex
                        val bgAlpha by animateFloatAsState(
                            targetValue = if (isCurrent) 1f else 0f,
                            animationSpec = tween(180), label = "bg$index"
                        )
                        val timestamp = remember(line) {
                            LyricsFormatter.parseFirstTimestampMillis(line)
                        }
                        val lineText = remember(line) {
                            if (timestamp != null) LyricsFormatter.stripLeadingTimeTags(line)
                            else line
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = bgAlpha * 0.1f))
                                .clickable { onSetCurrentLine(index) }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Active-line indicator bar
                            Box(
                                modifier = Modifier
                                    .width(3.dp).height(16.dp)
                                    .background(
                                        if (isCurrent) MaterialTheme.colorScheme.primary
                                        else Color.Transparent,
                                        RoundedCornerShape(1.5.dp)
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            // Timestamp chip — tap to seek to this position
                            if (timestamp != null) {
                                Text(
                                    text = formatTimeShort(timestamp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            onSetCurrentLine(index)
                                            onSeekToLineTimestamp(index)
                                        }
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            // Lyric text
                            Text(
                                text = lineText.ifBlank { "—" },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = if (isCurrent) 14.sp else 13.sp
                                ),
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.weight(1f),
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Bottom stamp controls
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-500L to "-0.5s", -100L to "-0.1s",
                            100L to "+0.1s", 500L to "+0.5s").forEach { (ms, label) ->
                            OutlinedButton(
                                onClick = { onAdjust(ms) },
                                modifier = Modifier.weight(1f).height(34.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text(label, fontSize = 11.sp) }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPreviousLine, Modifier.size(40.dp)) {
                            Icon(Icons.Default.KeyboardArrowUp, null)
                        }
                        Button(
                            onClick = onStamp,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("打时间戳", fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = onNextLine, Modifier.size(40.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                    }
                }
            }

            // Edit mode: free-form text field
            1 -> {
                OutlinedTextField(
                    value = lyricsText,
                    onValueChange = onLyricsTextChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    ),
                    placeholder = { Text("在此输入歌词，每行一句\n打轴 Tab 中为每行添加时间戳") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
private fun LyricsPreviewStrip(
    lines: List<String>,
    currentIndex: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            (-2..2).forEach { offset ->
                // Strip timestamp prefix so plain display text is always clean
                val raw = lines.getOrNull(currentIndex + offset).orEmpty()
                val text = LyricsFormatter.stripLeadingTimeTags(raw)
                val distance = abs(offset)
                val targetAlpha = when (distance) {
                    0 -> 1f
                    1 -> 0.38f
                    else -> 0.14f
                }
                val alpha by animateFloatAsState(targetAlpha, tween(280), label = "lps$offset")
                // Always render current line slot; skip blank neighbour slots
                if (distance == 0 || text.isNotBlank()) {
                    Text(
                        text = text.ifBlank { "·" },
                        color = if (distance == 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        fontWeight = if (distance == 0) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = if (distance == 0) 14.sp else 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 4.dp,
                                vertical = if (distance == 0) 5.dp else 2.dp
                            )
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val m = millis / 60_000
    val s = (millis % 60_000) / 1_000
    val ms = millis % 1_000
    return "%02d:%02d.%03d".format(m, s, ms)
}

private fun formatTimeShort(millis: Long): String {
    val m = millis / 60_000
    val s = (millis % 60_000) / 1_000
    return "%d:%02d".format(m, s)
}
