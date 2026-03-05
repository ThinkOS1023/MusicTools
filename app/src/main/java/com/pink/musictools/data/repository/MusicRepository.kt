package com.pink.musictools.data.repository

import com.pink.musictools.data.model.MusicItem
import com.pink.musictools.data.model.SortOrder
import kotlinx.coroutines.flow.Flow

/**
 * 音乐数据仓库接口
 * 提供统一的音乐数据访问层，协调本地数据库和MediaStore
 */
interface MusicRepository {
    
    /**
     * 获取所有音乐项
     * @return Flow发射音乐项列表
     */
    fun getAllMusic(): Flow<List<MusicItem>>
    
    /**
     * 根据ID获取音乐项
     * @param id 音乐项ID
     * @return 音乐项，如果不存在则返回null
     */
    suspend fun getMusicById(id: String): MusicItem?
    
    /**
     * 插入单个音乐项
     * @param music 要插入的音乐项
     */
    suspend fun insertMusic(music: MusicItem)
    
    /**
     * 批量插入音乐项
     * @param musicList 要插入的音乐项列表
     */
    suspend fun insertMusicBatch(musicList: List<MusicItem>)
    
    /**
     * 删除音乐项
     * @param id 要删除的音乐项ID
     */
    suspend fun deleteMusic(id: String)
    
    /**
     * 更新音乐项
     * @param music 要更新的音乐项
     */
    suspend fun updateMusic(music: MusicItem)
    
    /**
     * 搜索音乐
     * @param query 搜索关键词（匹配标题、艺术家或专辑）
     * @return Flow发射匹配的音乐项列表
     */
    fun searchMusic(query: String): Flow<List<MusicItem>>
    
    /**
     * 获取排序后的音乐列表
     * @param order 排序顺序
     * @return Flow发射排序后的音乐项列表
     */
    fun getMusicSorted(order: SortOrder): Flow<List<MusicItem>>
}
