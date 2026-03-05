package com.pink.musictools.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room数据库类，管理音乐数据持久化
 */
@Database(
    entities = [MusicEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    
    /**
     * 获取音乐DAO
     */
    abstract fun musicDao(): MusicDao
    
    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null
        
        private const val DATABASE_NAME = "music_database"
        
        /**
         * 获取数据库单例实例
         */
        fun getInstance(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
