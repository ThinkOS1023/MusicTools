package com.pink.musictools.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pink.musictools.data.model.PlaybackState
import com.pink.musictools.data.model.MusicItem
import com.pink.musictools.data.model.SortOrder
import com.pink.musictools.ui.components.MusicItemCard
import com.pink.musictools.viewmodel.MusicLibraryViewModel
import com.pink.musictools.viewmodel.MusicPlayerViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicListScreen(
    libraryViewModel: MusicLibraryViewModel,
    playerViewModel: MusicPlayerViewModel,
    onNavigateToEditor: (String) -> Unit = {},
    onNavigateToPlayer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val musicList     by libraryViewModel.musicList.collectAsState()
    val isLoading     by libraryViewModel.isLoading.collectAsState()
    val sortOrder     by libraryViewModel.sortOrder.collectAsState()
    val searchQuery   by libraryViewModel.searchQuery.collectAsState()
    val exportStatus  by libraryViewModel.exportStatus.collectAsState()
    val playbackState by playerViewModel.playbackState.collectAsState()
    val currentMusic  by playerViewModel.currentMusic.collectAsState()

    var showSortMenu        by remember { mutableStateOf(false) }
    var showDeleteDialog    by remember { mutableStateOf<MusicItem?>(null) }
    var selectedMusicForMenu by remember { mutableStateOf<MusicItem?>(null) }

    // Local haze state — list content is source, top bar is hazeChild consumer
    val hazeState = remember { HazeState() }
    val surface   = MaterialTheme.colorScheme.surface
    val outline   = MaterialTheme.colorScheme.outlineVariant

    LaunchedEffect(Unit) {
        libraryViewModel.onMusicItemClicked.collect { musicItem ->
            if (musicItem != null) {
                playerViewModel.setPlaylist(musicList)
                playerViewModel.playMusic(musicItem)
                libraryViewModel.clearMusicItemClickedEvent()
                onNavigateToPlayer()
            }
        }
    }

    // ── Root Box — no inner Scaffold ──────────────────────────────────────────
    Box(modifier = modifier.fillMaxSize()) {

        // ── Scrollable content (haze source) ──────────────────────────────────
        // contentPadding top = 64 dp (TopAppBar height) so first item sits below the bar
        // but the LazyColumn itself starts at y=0, overlapping the transparent bar → blur works
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(top = 64.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(strokeWidth = 2.dp) }
            }

            musicList.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(top = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MusicNote, null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "未找到匹配的音乐"
                                   else "音乐库为空\n请前往导入页面添加音乐",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().haze(hazeState),
                    contentPadding = PaddingValues(top = 64.dp, bottom = 16.dp)
                ) {
                    // ── Search bar (scrolls with list) ────────────────────────
                    item(key = "searchBar") {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { libraryViewModel.onSearchQueryChanged(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            placeholder = { Text("搜索音乐、艺术家或专辑") },
                            leadingIcon  = { Icon(Icons.Default.Search, null) },
                            trailingIcon = {
                                AnimatedVisibility(
                                    visible = searchQuery.isNotEmpty(),
                                    enter = scaleIn(spring()), exit = scaleOut(spring())
                                ) {
                                    IconButton(onClick = { libraryViewModel.onSearchQueryChanged("") }) {
                                        Icon(Icons.Default.Clear, null)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp)
                        )
                    }

                    // ── Track count ───────────────────────────────────────────
                    item(key = "count") {
                        Text(
                            text = "${musicList.size} 首音乐",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        HorizontalDivider(color = outline.copy(alpha = 0.18f))
                    }

                    // ── Music items ───────────────────────────────────────────
                    items(musicList, key = { it.id }) { musicItem ->
                        MusicItemCard(
                            musicItem          = musicItem,
                            isCurrentlyPlaying = currentMusic?.id == musicItem.id &&
                                                 playbackState is PlaybackState.Playing,
                            onClick    = { libraryViewModel.onMusicItemClicked(musicItem) },
                            onMoreClick = { selectedMusicForMenu = musicItem },
                            onLongPress = {
                                selectedMusicForMenu = musicItem
                            },
                            modifier = Modifier
                                .padding(vertical = 3.dp)
                                .animateItem(
                                    fadeInSpec     = tween(300),
                                    fadeOutSpec    = tween(200),
                                    placementSpec  = spring()
                                )
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 74.dp),
                            color    = outline.copy(alpha = 0.12f)
                        )

                        // Context menu (long press or MoreVert)
                        if (selectedMusicForMenu == musicItem) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { selectedMusicForMenu = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("播放") },
                                    onClick = {
                                        libraryViewModel.onMusicItemClicked(musicItem)
                                        selectedMusicForMenu = null
                                    },
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("编辑信息") },
                                    onClick = {
                                        onNavigateToEditor(musicItem.id)
                                        selectedMusicForMenu = null
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("导出") },
                                    onClick = {
                                        libraryViewModel.exportMusic(musicItem)
                                        selectedMusicForMenu = null
                                    },
                                    leadingIcon = { Icon(Icons.Default.FileDownload, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showDeleteDialog = musicItem
                                        selectedMusicForMenu = null
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, null,
                                            tint = MaterialTheme.colorScheme.error)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Frosted-glass TopAppBar (hazeChild) ───────────────────────────────
        TopAppBar(
            title = {
                Text("音乐库", style = MaterialTheme.typography.titleLarge)
            },
            actions = {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, "排序")
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    listOf(
                        SortOrder.TITLE_ASC      to "标题 (A-Z)",
                        SortOrder.TITLE_DESC     to "标题 (Z-A)",
                        SortOrder.ARTIST_ASC     to "艺术家 (A-Z)",
                        SortOrder.ARTIST_DESC    to "艺术家 (Z-A)",
                        SortOrder.DATE_ADDED_DESC to "最新添加",
                        SortOrder.DATE_ADDED_ASC  to "最早添加"
                    ).forEach { (order, label) ->
                        DropdownMenuItem(
                            text  = { Text(label) },
                            onClick = {
                                libraryViewModel.onSortOrderChanged(order)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (sortOrder == order) Icon(Icons.Default.Check, null)
                            }
                        )
                    }
                }
                IconButton(onClick = { libraryViewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, "刷新")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            modifier = Modifier.hazeChild(
                state = hazeState,
                style = HazeStyle(blurRadius = 22.dp, tint = surface.copy(alpha = 0.72f))
            )
        )

        // ── Export snackbar ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = exportStatus != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            enter    = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
            exit     = slideOutVertically(tween(300)) { it } + fadeOut(tween(300))
        ) {
            Snackbar { Text(exportStatus.orEmpty()) }
        }
    }

    // ── Delete confirmation dialog ─────────────────────────────────────────────
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除音乐") },
            text  = { Text("确定要删除 \"${showDeleteDialog!!.title}\" 吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        libraryViewModel.deleteMusicItem(showDeleteDialog!!)
                        showDeleteDialog = null
                    }
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            }
        )
    }
}
