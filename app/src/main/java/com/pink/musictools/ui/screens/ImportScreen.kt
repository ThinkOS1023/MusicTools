package com.pink.musictools.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pink.musictools.data.model.AudioFile
import com.pink.musictools.ui.components.PermissionHandler
import com.pink.musictools.viewmodel.ImportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    modifier: Modifier = Modifier
) {
    val scanProgress by viewModel.scanProgress.collectAsState()
    val discoveredFiles by viewModel.discoveredFiles.collectAsState()
    val importStatus by viewModel.importStatus.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    val currentStage by viewModel.currentStage.collectAsState()
    val stageLogs by viewModel.stageLogs.collectAsState()
    
    val selectedCount = discoveredFiles.count { it.isSelected }
    val allSelected = discoveredFiles.isNotEmpty() && discoveredFiles.all { it.isSelected }

    var searchQuery by remember { mutableStateOf("") }
    val filteredFiles = remember(discoveredFiles, searchQuery) {
        if (searchQuery.isBlank()) discoveredFiles
        else discoveredFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    var hasPermission by remember { mutableStateOf(false) }
    var showPermissionDeniedMessage by remember { mutableStateOf(false) }
    
    // 文件夹选择器
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.startScan(it)
        }
    }
    
    PermissionHandler(
        onPermissionGranted = { hasPermission = true },
        onPermissionDenied = { showPermissionDeniedMessage = true }
    ) { requestPermission ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "导入音乐",
                            modifier = Modifier.animateContentSize()
                        ) 
                    },
                    actions = {
                        AnimatedVisibility(
                            visible = discoveredFiles.isNotEmpty() && importStatus !is ImportViewModel.ImportStatus.Scanning,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            IconButton(onClick = { viewModel.selectAll(!allSelected) }) {
                                Icon(
                                    imageVector = if (allSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = if (allSelected) "取消全选" else "全选"
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = selectedCount > 0 && importStatus !is ImportViewModel.ImportStatus.Importing,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.importSelectedFiles() },
                        icon = { Icon(Icons.Default.Download, "导入") },
                        text = { Text("导入 ($selectedCount)") }
                    )
                }
            },
            modifier = modifier
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 权限被拒绝提示
                AnimatedVisibility(
                    visible = showPermissionDeniedMessage && !hasPermission,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    "警告",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "需要存储权限",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "此应用需要访问您的音乐文件。请授予存储权限以继续。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = {
                                showPermissionDeniedMessage = false
                                requestPermission()
                            }) {
                                Text("授予权限")
                            }
                        }
                    }
                }
                
                // 选择文件夹按钮
                AnimatedVisibility(
                    visible = importStatus !is ImportViewModel.ImportStatus.Scanning && 
                              importStatus !is ImportViewModel.ImportStatus.Importing,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                if (hasPermission) {
                                    folderPickerLauncher.launch(null)
                                } else {
                                    requestPermission()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FolderOpen, "选择文件夹", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("选择文件夹")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = {
                                if (hasPermission) viewModel.startScan()
                                else requestPermission()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, "扫描全部", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("扫描所有音乐")
                        }
                    }
                }

                
                // 扫描进度
                AnimatedVisibility(
                    visible = importStatus is ImportViewModel.ImportStatus.Scanning,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val infiniteTransition = rememberInfiniteTransition(label = "scan")
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 0.8f,
                                    targetValue = 1.2f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "scale"
                                )
                                Icon(
                                    Icons.Default.Search,
                                    "扫描中",
                                    modifier = Modifier.scale(scale),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("正在扫描...", style = MaterialTheme.typography.titleMedium)
                                    if (currentStage.isNotEmpty()) {
                                        Text(
                                            text = currentStage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (scanProgress.totalFiles > 0) {
                                    Text(
                                        text = "${scanProgress.scannedFiles}/${scanProgress.totalFiles}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            if (scanProgress.totalFiles > 0) {
                                LinearProgressIndicator(
                                    progress = { scanProgress.progressPercentage },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            if (scanProgress.currentFile != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = scanProgress.currentFile!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // 日志区域
                            if (stageLogs.isNotEmpty()) {
                                val scanLogState = rememberLazyListState()
                                LaunchedEffect(stageLogs.size) {
                                    if (stageLogs.isNotEmpty()) scanLogState.animateScrollToItem(stageLogs.lastIndex)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            shape = MaterialTheme.shapes.small
                                        )
                                ) {
                                    LazyColumn(
                                        state = scanLogState,
                                        modifier = Modifier.fillMaxSize().padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        items(stageLogs) { log ->
                                            Text(
                                                text = log,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 导入进度
                AnimatedVisibility(
                    visible = importStatus is ImportViewModel.ImportStatus.Importing,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val logListState = rememberLazyListState()
                    LaunchedEffect(stageLogs.size) {
                        if (stageLogs.isNotEmpty()) logListState.animateScrollToItem(stageLogs.lastIndex)
                    }
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // 标题行
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    progress = { importProgress },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("正在导入...", style = MaterialTheme.typography.titleMedium)
                                    if (currentStage.isNotEmpty()) {
                                        Text(
                                            text = currentStage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Text(
                                    text = "${(importProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { importProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            // 日志区域
                            if (stageLogs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            shape = MaterialTheme.shapes.small
                                        )
                                ) {
                                    LazyColumn(
                                        state = logListState,
                                        modifier = Modifier.fillMaxSize().padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        items(stageLogs) { log ->
                                            Text(
                                                text = log,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 导入成功提示
                AnimatedVisibility(
                    visible = importStatus is ImportViewModel.ImportStatus.Success,
                    enter = expandVertically() + fadeIn() + scaleIn(),
                    exit = shrinkVertically() + fadeOut() + scaleOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, "成功", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "导入成功",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (importStatus is ImportViewModel.ImportStatus.Success) {
                                    Text(
                                        "已导入 ${(importStatus as ImportViewModel.ImportStatus.Success).count} 首音乐",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 导入错误提示
                AnimatedVisibility(
                    visible = importStatus is ImportViewModel.ImportStatus.Error,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, "错误", tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(12.dp))
                            if (importStatus is ImportViewModel.ImportStatus.Error) {
                                Text(
                                    (importStatus as ImportViewModel.ImportStatus.Error).message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                // 发现的文件列表
                AnimatedVisibility(
                    visible = discoveredFiles.isNotEmpty() &&
                              importStatus !is ImportViewModel.ImportStatus.Scanning &&
                              importStatus !is ImportViewModel.ImportStatus.Importing,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        // 搜索栏
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            placeholder = { Text("搜索文件名...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, "清除")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        val countText = if (searchQuery.isBlank())
                            "发现 ${discoveredFiles.size} 个音频文件"
                        else
                            "找到 ${filteredFiles.size} / ${discoveredFiles.size} 个"
                        Text(
                            countText,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = filteredFiles,
                                key = { it.uri.toString() }
                            ) { audioFile ->
                                AudioFileItem(
                                    audioFile = audioFile,
                                    onSelectionChanged = { viewModel.toggleFileSelection(audioFile) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
                
                // 空状态
                AnimatedVisibility(
                    visible = discoveredFiles.isEmpty() && 
                              importStatus !is ImportViewModel.ImportStatus.Scanning &&
                              importStatus !is ImportViewModel.ImportStatus.Importing &&
                              importStatus !is ImportViewModel.ImportStatus.Success,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "empty")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1500),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alpha"
                            )
                            Icon(
                                Icons.Default.MusicNote,
                                null,
                                modifier = Modifier.size(64.dp).scale(alpha),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "选择文件夹或扫描所有音乐",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioFileItem(
    audioFile: AudioFile,
    onSelectionChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (audioFile.isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    Card(
        modifier = modifier.fillMaxWidth().scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = if (audioFile.isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = audioFile.isSelected,
                onCheckedChange = { onSelectionChanged() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.MusicNote, "音乐", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = audioFile.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatFileSize(audioFile.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
