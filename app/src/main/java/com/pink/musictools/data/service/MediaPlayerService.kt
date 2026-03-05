package com.pink.musictools.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pink.musictools.MainActivity
import com.pink.musictools.R
import java.io.IOException

/**
 * MediaPlayer服务
 * 
 * Android后台服务，管理MediaPlayer生命周期和播放控制
 * 
 * 职责：
 * - 管理Android MediaPlayer实例
 * - 在后台保持音乐播放
 * - 处理播放完成和错误事件
 * - 支持前台服务通知
 */
class MediaPlayerService : Service() {
    
    private val binder = MediaPlayerBinder()
    private var mediaPlayer: MediaPlayer? = null
    
    private var onCompletionListener: (() -> Unit)? = null
    private var onErrorListener: ((String) -> Unit)? = null
    
    // Notification related
    private lateinit var notificationManager: NotificationManager
    private var notificationReceiver: NotificationReceiver? = null
    private var currentMusicTitle: String = "未知歌曲"
    private var currentMusicArtist: String = "未知艺术家"
    
    companion object {
        private const val TAG = "MediaPlayerService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "music_playback_channel"
        
        // Notification actions
        const val ACTION_PLAY_PAUSE = "com.pink.musictools.ACTION_PLAY_PAUSE"
        const val ACTION_PREVIOUS = "com.pink.musictools.ACTION_PREVIOUS"
        const val ACTION_NEXT = "com.pink.musictools.ACTION_NEXT"
    }
    
    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "Service bound")
        return binder
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        // Initialize notification manager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel
        createNotificationChannel()
        
        // Register broadcast receiver for notification actions
        notificationReceiver = NotificationReceiver(this)
        val filter = IntentFilter().apply {
            addAction(ACTION_PLAY_PAUSE)
            addAction(ACTION_PREVIOUS)
            addAction(ACTION_NEXT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(notificationReceiver, filter)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        
        // Unregister broadcast receiver
        notificationReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Receiver not registered", e)
            }
        }
        
        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        
        release()
    }
    
    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }
    
    /**
     * 构建播放通知
     * 
     * @param isPlaying 是否正在播放
     * @return Notification对象
     */
    private fun buildNotification(isPlaying: Boolean): Notification {
        // Create intent to open the app when notification is clicked
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create pending intents for notification actions
        val playPauseIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val previousIntent = PendingIntent.getBroadcast(
            this,
            1,
            Intent(ACTION_PREVIOUS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val nextIntent = PendingIntent.getBroadcast(
            this,
            2,
            Intent(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(
                if (isPlaying) getString(R.string.notification_title_playing)
                else getString(R.string.notification_title_paused)
            )
            .setContentText("$currentMusicTitle - $currentMusicArtist")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            // Add playback controls
            .addAction(
                android.R.drawable.ic_media_previous,
                getString(R.string.notification_action_previous),
                previousIntent
            )
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play,
                if (isPlaying) getString(R.string.notification_action_pause)
                else getString(R.string.notification_action_play),
                playPauseIntent
            )
            .addAction(
                android.R.drawable.ic_media_next,
                getString(R.string.notification_action_next),
                nextIntent
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }
    
    /**
     * 更新通知
     * 
     * @param isPlaying 是否正在播放
     */
    private fun updateNotification(isPlaying: Boolean) {
        val notification = buildNotification(isPlaying)
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Notification updated: isPlaying=$isPlaying")
    }
    
    /**
     * 启动前台服务
     * 
     * @param isPlaying 是否正在播放
     */
    private fun startForegroundService(isPlaying: Boolean) {
        val notification = buildNotification(isPlaying)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        Log.d(TAG, "Foreground service started")
    }
    
    /**
     * 设置当前播放的音乐信息
     * 
     * @param title 歌曲标题
     * @param artist 艺术家名称
     */
    fun setMusicInfo(title: String, artist: String) {
        currentMusicTitle = title
        currentMusicArtist = artist
        Log.d(TAG, "Music info updated: $title - $artist")
        
        // Update notification if service is in foreground
        if (isPlaying()) {
            updateNotification(true)
        }
    }
    
    /**
     * 准备播放指定URI的音频文件
     * 
     * @param uri 音频文件URI
     * @return true表示准备成功，false表示失败
     */
    fun prepare(uri: Uri): Boolean {
        return try {
            // 释放现有的MediaPlayer实例
            release()
            
            // 创建新的MediaPlayer实例
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                
                // 设置完成监听器
                setOnCompletionListener {
                    Log.d(TAG, "Playback completed")
                    onCompletionListener?.invoke()
                }
                
                // 设置错误监听器
                setOnErrorListener { _, what, extra ->
                    val errorMsg = "MediaPlayer error: what=$what, extra=$extra"
                    Log.e(TAG, errorMsg)
                    onErrorListener?.invoke(errorMsg)
                    true // 返回true表示错误已处理
                }
                
                // 同步准备（在实际应用中可能需要使用prepareAsync）
                prepare()
            }
            
            Log.d(TAG, "MediaPlayer prepared successfully for URI: $uri")
            true
        } catch (e: IOException) {
            val errorMsg = "Failed to prepare MediaPlayer: ${e.message}"
            Log.e(TAG, errorMsg, e)
            onErrorListener?.invoke(errorMsg)
            false
        } catch (e: IllegalStateException) {
            val errorMsg = "MediaPlayer in illegal state: ${e.message}"
            Log.e(TAG, errorMsg, e)
            onErrorListener?.invoke(errorMsg)
            false
        }
    }
    
    /**
     * 开始播放
     * 
     * @return true表示成功，false表示失败
     */
    fun start(): Boolean {
        return try {
            mediaPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                    Log.d(TAG, "Playback started")
                    
                    // Start foreground service with notification
                    startForegroundService(true)
                }
                true
            } ?: run {
                Log.w(TAG, "Cannot start: MediaPlayer is null")
                false
            }
        } catch (e: IllegalStateException) {
            val errorMsg = "Failed to start playback: ${e.message}"
            Log.e(TAG, errorMsg, e)
            onErrorListener?.invoke(errorMsg)
            false
        }
    }
    
    /**
     * 暂停播放
     * 
     * @return true表示成功，false表示失败
     */
    fun pause(): Boolean {
        return try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    Log.d(TAG, "Playback paused")
                    
                    // Update notification to show paused state
                    updateNotification(false)
                }
                true
            } ?: run {
                Log.w(TAG, "Cannot pause: MediaPlayer is null")
                false
            }
        } catch (e: IllegalStateException) {
            val errorMsg = "Failed to pause playback: ${e.message}"
            Log.e(TAG, errorMsg, e)
            onErrorListener?.invoke(errorMsg)
            false
        }
    }
    
    /**
     * 停止播放
     * 
     * @return true表示成功，false表示失败
     */
    fun stop(): Boolean {
        return try {
            mediaPlayer?.let {
                // Call stop regardless of isPlaying — MediaPlayer may be in PlaybackCompleted state
                runCatching { it.stop() }
                Log.d(TAG, "Playback stopped")
                stopForeground(STOP_FOREGROUND_REMOVE)
                true
            } ?: run {
                Log.w(TAG, "Cannot stop: MediaPlayer is null")
                false
            }
        } catch (e: IllegalStateException) {
            val errorMsg = "Failed to stop playback: ${e.message}"
            Log.e(TAG, errorMsg, e)
            onErrorListener?.invoke(errorMsg)
            false
        }
    }
    
    /**
     * 跳转到指定位置
     * 
     * @param position 目标位置（毫秒）
     * @return true表示成功，false表示失败
     */
    fun seekTo(position: Long): Boolean {
        return try {
            mediaPlayer?.let {
                it.seekTo(position.toInt())
                Log.d(TAG, "Seeked to position: $position ms")
                true
            } ?: run {
                Log.w(TAG, "Cannot seek: MediaPlayer is null")
                false
            }
        } catch (e: IllegalStateException) {
            val errorMsg = "Failed to seek: ${e.message}"
            Log.e(TAG, errorMsg, e)
            onErrorListener?.invoke(errorMsg)
            false
        }
    }
    
    /**
     * 释放MediaPlayer资源
     */
    fun release() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                Log.d(TAG, "MediaPlayer released")
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error releasing MediaPlayer: ${e.message}", e)
            }
        }
        mediaPlayer = null
    }
    
    /**
     * 获取当前播放位置
     * 
     * @return 当前位置（毫秒），如果MediaPlayer为null则返回0
     */
    fun getCurrentPosition(): Long {
        return try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error getting current position: ${e.message}", e)
            0L
        }
    }
    
    /**
     * 获取音频总时长
     * 
     * @return 总时长（毫秒），如果MediaPlayer为null则返回0
     */
    fun getDuration(): Long {
        return try {
            mediaPlayer?.duration?.toLong() ?: 0L
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error getting duration: ${e.message}", e)
            0L
        }
    }
    
    /**
     * 检查是否正在播放
     * 
     * @return true表示正在播放，false表示未播放或MediaPlayer为null
     */
    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error checking playing state: ${e.message}", e)
            false
        }
    }
    
    /**
     * 设置播放完成监听器
     * 
     * @param listener 完成时调用的回调函数
     */
    fun setOnCompletionListener(listener: () -> Unit) {
        onCompletionListener = listener
    }
    
    /**
     * 设置错误监听器
     * 
     * @param listener 发生错误时调用的回调函数，参数为错误消息
     */
    fun setOnErrorListener(listener: (String) -> Unit) {
        onErrorListener = listener
    }
    
    /**
     * Binder类，用于客户端与服务通信
     */
    inner class MediaPlayerBinder : Binder() {
        /**
         * 获取服务实例
         * 
         * @return MediaPlayerService实例
         */
        fun getService(): MediaPlayerService = this@MediaPlayerService
    }
    
    /**
     * BroadcastReceiver用于处理通知按钮点击事件。
     * 静态嵌套类（无 inner 关键字）以满足 Android 可实例化要求。
     */
    class NotificationReceiver(private val service: MediaPlayerService) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PLAY_PAUSE -> {
                    Log.d(TAG, "Play/Pause action received")
                    if (service.isPlaying()) service.pause() else service.start()
                }
                ACTION_PREVIOUS -> Log.d(TAG, "Previous action received")
                ACTION_NEXT -> Log.d(TAG, "Next action received")
            }
        }
    }
}
