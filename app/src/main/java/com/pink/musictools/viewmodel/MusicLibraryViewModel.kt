package com.pink.musictools.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pink.musictools.data.model.MusicItem
import com.pink.musictools.data.model.SortOrder
import com.pink.musictools.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 音乐库ViewModel
 * 
 * 管理音乐库列表展示和筛选
 * 
 * 职责：
 * - 加载和展示音乐库列表
 * - 处理音乐项选择和播放
 * - 支持排序和搜索功能
 * - 管理音乐项删除操作
 * 
 * @param musicRepository 音乐数据仓库
 */
class MusicLibraryViewModel(
    private val musicRepository: MusicRepository,
    private val musicExporter: com.pink.musictools.domain.MusicExporter
) : ViewModel() {
    
    /**
     * 音乐列表流
     */
    private val _musicList = MutableStateFlow<List<MusicItem>>(emptyList())
    val musicList: StateFlow<List<MusicItem>> = _musicList.asStateFlow()
    
    /**
     * 加载状态流
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * 排序顺序流
     */
    private val _sortOrder = MutableStateFlow(SortOrder.DATE_ADDED_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()
    
    /**
     * 搜索查询流
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    /**
     * 音乐项点击回调
     */
    private val _onMusicItemClicked = MutableStateFlow<MusicItem?>(null)
    val onMusicItemClicked: StateFlow<MusicItem?> = _onMusicItemClicked.asStateFlow()
    
    /**
     * 导出状态流
     */
    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus.asStateFlow()
    
    init {
        // 初始化时加载音乐库
        loadMusicLibrary()
    }
    
    /**
     * 加载音乐库
     * 
     * 根据当前的排序顺序和搜索查询加载音乐列表
     */
    fun loadMusicLibrary() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val query = _searchQuery.value
                val order = _sortOrder.value
                
                // 根据搜索查询决定使用哪个数据源
                val musicFlow = if (query.isNotEmpty()) {
                    musicRepository.searchMusic(query)
                } else {
                    musicRepository.getMusicSorted(order)
                }
                
                // 收集音乐列表
                musicFlow.collect { musicItems ->
                    _musicList.value = musicItems
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                // 处理错误
                _isLoading.value = false
                _musicList.value = emptyList()
            }
        }
    }
    
    /**
     * 处理音乐项点击
     * 
     * @param item 被点击的音乐项
     */
    fun onMusicItemClicked(item: MusicItem) {
        _onMusicItemClicked.value = item
    }
    
    /**
     * 清除音乐项点击事件
     */
    fun clearMusicItemClickedEvent() {
        _onMusicItemClicked.value = null
    }
    
    /**
     * 处理排序顺序变更
     * 
     * @param order 新的排序顺序
     */
    fun onSortOrderChanged(order: SortOrder) {
        if (_sortOrder.value != order) {
            _sortOrder.value = order
            // 重新加载音乐库
            loadMusicLibrary()
        }
    }
    
    /**
     * 处理搜索查询变更
     * 
     * @param query 搜索关键词
     */
    fun onSearchQueryChanged(query: String) {
        if (_searchQuery.value != query) {
            _searchQuery.value = query
            // 重新加载音乐库
            loadMusicLibrary()
        }
    }
    
    /**
     * 删除音乐项
     * 
     * @param item 要删除的音乐项
     */
    fun deleteMusicItem(item: MusicItem) {
        viewModelScope.launch {
            try {
                musicRepository.deleteMusic(item.id)
                // 删除成功后，音乐列表会自动更新（通过Flow）
            } catch (e: Exception) {
                // 处理删除错误
                // 可以通过添加错误状态流来通知UI
            }
        }
    }
    
    /**
     * 刷新音乐库
     * 
     * 重新加载当前的音乐列表
     */
    fun refresh() {
        loadMusicLibrary()
    }
    
    /**
     * 导出音乐
     * 
     * @param item 要导出的音乐项
     */
    fun exportMusic(item: MusicItem) {
        viewModelScope.launch {
            try {
                _exportStatus.value = "正在导出..."
                when (val result = musicExporter.exportMusic(item)) {
                    is com.pink.musictools.domain.MusicExporter.ExportResult.Success -> {
                        _exportStatus.value = "导出成功: ${result.path}"
                        kotlinx.coroutines.delay(3000)
                        _exportStatus.value = null
                    }
                    is com.pink.musictools.domain.MusicExporter.ExportResult.Error -> {
                        _exportStatus.value = result.message
                        kotlinx.coroutines.delay(3000)
                        _exportStatus.value = null
                    }
                }
            } catch (e: Exception) {
                _exportStatus.value = "导出失败: ${e.message}"
                kotlinx.coroutines.delay(3000)
                _exportStatus.value = null
            }
        }
    }
}
