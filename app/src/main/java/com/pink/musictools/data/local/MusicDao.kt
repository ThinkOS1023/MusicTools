package com.pink.musictools.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 音乐数据访问对象，提供数据库CRUD操作
 */
@Dao
interface MusicDao {
    
    /**
     * 获取所有音乐项
     */
    @Query("SELECT * FROM music")
    fun getAllMusic(): Flow<List<MusicEntity>>
    
    /**
     * 根据ID获取音乐项
     */
    @Query("SELECT * FROM music WHERE id = :id")
    suspend fun getMusicById(id: String): MusicEntity?
    
    /**
     * 插入单个音乐项
     * 如果ID已存在，则替换
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMusic(music: MusicEntity)
    
    /**
     * 批量插入音乐项
     * 如果ID已存在，则替换
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMusicBatch(musicList: List<MusicEntity>)
    
    /**
     * 更新音乐项
     */
    @Update
    suspend fun updateMusic(music: MusicEntity)
    
    /**
     * 根据ID删除音乐项
     */
    @Query("DELETE FROM music WHERE id = :id")
    suspend fun deleteMusic(id: String)
    
    /**
     * 删除所有音乐项
     */
    @Query("DELETE FROM music")
    suspend fun deleteAllMusic()
    
    /**
     * 搜索音乐（标题、艺术家或专辑包含查询字符串）
     */
    @Query("""
        SELECT * FROM music 
        WHERE title LIKE '%' || :query || '%' 
        OR artist LIKE '%' || :query || '%' 
        OR album LIKE '%' || :query || '%'
    """)
    fun searchMusic(query: String): Flow<List<MusicEntity>>
    
    /**
     * 按标题升序排序
     */
    @Query("SELECT * FROM music ORDER BY title ASC")
    fun getMusicSortedByTitleAsc(): Flow<List<MusicEntity>>
    
    /**
     * 按标题降序排序
     */
    @Query("SELECT * FROM music ORDER BY title DESC")
    fun getMusicSortedByTitleDesc(): Flow<List<MusicEntity>>
    
    /**
     * 按艺术家升序排序
     */
    @Query("SELECT * FROM music ORDER BY artist ASC")
    fun getMusicSortedByArtistAsc(): Flow<List<MusicEntity>>
    
    /**
     * 按艺术家降序排序
     */
    @Query("SELECT * FROM music ORDER BY artist DESC")
    fun getMusicSortedByArtistDesc(): Flow<List<MusicEntity>>
    
    /**
     * 按添加日期升序排序
     */
    @Query("SELECT * FROM music ORDER BY dateAdded ASC")
    fun getMusicSortedByDateAddedAsc(): Flow<List<MusicEntity>>
    
    /**
     * 按添加日期降序排序
     */
    @Query("SELECT * FROM music ORDER BY dateAdded DESC")
    fun getMusicSortedByDateAddedDesc(): Flow<List<MusicEntity>>
    
    /**
     * 按时长升序排序
     */
    @Query("SELECT * FROM music ORDER BY duration ASC")
    fun getMusicSortedByDurationAsc(): Flow<List<MusicEntity>>
    
    /**
     * 按时长降序排序
     */
    @Query("SELECT * FROM music ORDER BY duration DESC")
    fun getMusicSortedByDurationDesc(): Flow<List<MusicEntity>>
    
    /**
     * 获取音乐总数
     */
    @Query("SELECT COUNT(*) FROM music")
    suspend fun getMusicCount(): Int
}
