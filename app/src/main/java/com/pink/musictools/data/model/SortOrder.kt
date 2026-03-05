package com.pink.musictools.data.model

/**
 * 排序顺序枚举
 */
enum class SortOrder {
    /**
     * 按标题升序排序
     */
    TITLE_ASC,
    
    /**
     * 按标题降序排序
     */
    TITLE_DESC,
    
    /**
     * 按艺术家升序排序
     */
    ARTIST_ASC,
    
    /**
     * 按艺术家降序排序
     */
    ARTIST_DESC,
    
    /**
     * 按添加日期升序排序（最旧的在前）
     */
    DATE_ADDED_ASC,
    
    /**
     * 按添加日期降序排序（最新的在前）
     */
    DATE_ADDED_DESC,
    
    /**
     * 按时长升序排序（最短的在前）
     */
    DURATION_ASC,
    
    /**
     * 按时长降序排序（最长的在前）
     */
    DURATION_DESC
}
