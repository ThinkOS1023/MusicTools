package com.pink.musictools.data.repository

import com.pink.musictools.data.local.MusicDao
import com.pink.musictools.data.local.MusicEntity
import com.pink.musictools.data.model.MusicItem
import com.pink.musictools.data.model.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * MusicRepository的实现类
 * 使用Room数据库作为数据源
 * 
 * @property musicDao Room数据访问对象
 */
class MusicRepositoryImpl(
    private val musicDao: MusicDao
) : MusicRepository {
    
    override fun getAllMusic(): Flow<List<MusicItem>> {
        return musicDao.getAllMusic().map { entities ->
            entities.map { it.toMusicItem() }
        }
    }
    
    override suspend fun getMusicById(id: String): MusicItem? {
        return musicDao.getMusicById(id)?.toMusicItem()
    }
    
    override suspend fun insertMusic(music: MusicItem) {
        val entity = MusicEntity.fromMusicItem(music)
        musicDao.insertMusic(entity)
    }
    
    override suspend fun insertMusicBatch(musicList: List<MusicItem>) {
        val entities = musicList.map { MusicEntity.fromMusicItem(it) }
        musicDao.insertMusicBatch(entities)
    }
    
    override suspend fun deleteMusic(id: String) {
        musicDao.deleteMusic(id)
    }
    
    override suspend fun updateMusic(music: MusicItem) {
        val entity = MusicEntity.fromMusicItem(music)
        musicDao.updateMusic(entity)
    }
    
    override fun searchMusic(query: String): Flow<List<MusicItem>> {
        return musicDao.searchMusic(query).map { entities ->
            entities.map { it.toMusicItem() }
        }
    }
    
    override fun getMusicSorted(order: SortOrder): Flow<List<MusicItem>> {
        val flow = when (order) {
            SortOrder.TITLE_ASC -> musicDao.getMusicSortedByTitleAsc()
            SortOrder.TITLE_DESC -> musicDao.getMusicSortedByTitleDesc()
            SortOrder.ARTIST_ASC -> musicDao.getMusicSortedByArtistAsc()
            SortOrder.ARTIST_DESC -> musicDao.getMusicSortedByArtistDesc()
            SortOrder.DATE_ADDED_ASC -> musicDao.getMusicSortedByDateAddedAsc()
            SortOrder.DATE_ADDED_DESC -> musicDao.getMusicSortedByDateAddedDesc()
            SortOrder.DURATION_ASC -> musicDao.getMusicSortedByDurationAsc()
            SortOrder.DURATION_DESC -> musicDao.getMusicSortedByDurationDesc()
        }
        
        return flow.map { entities ->
            entities.map { it.toMusicItem() }
        }
    }
}
