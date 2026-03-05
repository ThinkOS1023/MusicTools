package com.pink.musictools.ui.components

import android.Manifest
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.*

/**
 * 权限处理组件
 * 
 * 处理存储权限请求，支持Android 13+的新权限模型
 * 
 * @param onPermissionGranted 权限授予回调
 * @param onPermissionDenied 权限拒绝回调
 * @param content 内容组件
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {},
    content: @Composable (requestPermission: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    
    // 根据Android版本选择合适的权限
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Android 13+ 使用新的媒体权限
        listOf(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        // Android 12及以下使用旧的存储权限
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    
    val permissionState = rememberMultiplePermissionsState(permissions)
    
    var showRationale by remember { mutableStateOf(false) }
    
    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            onPermissionGranted()
        }
    }
    
    // 权限说明对话框
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("需要存储权限") },
            text = {
                Text(
                    "此应用需要访问您的音乐文件以便播放和管理。\n\n" +
                    "请在下一步中授予存储权限。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        permissionState.launchMultiplePermissionRequest()
                    }
                ) {
                    Text("继续")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        onPermissionDenied()
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
    
    content {
        when {
            permissionState.allPermissionsGranted -> {
                // 权限已授予
                onPermissionGranted()
            }
            
            permissionState.shouldShowRationale -> {
                // 需要显示权限说明
                showRationale = true
            }
            
            else -> {
                // 请求权限
                permissionState.launchMultiplePermissionRequest()
            }
        }
    }
}
