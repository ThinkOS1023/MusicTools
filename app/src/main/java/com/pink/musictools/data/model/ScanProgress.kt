package com.pink.musictools.data.model

/**
 * 扫描进度数据模型
 * 
 * @property scannedFiles 已扫描的文件数量，必须在0到totalFiles之间
 * @property totalFiles 总文件数量，必须大于等于0
 * @property currentFile 当前正在扫描的文件名，可选
 * @property isComplete 扫描是否完成
 */
data class ScanProgress(
    val scannedFiles: Int,
    val totalFiles: Int,
    val currentFile: String?,
    val isComplete: Boolean
) {
    /**
     * 扫描进度百分比（0.0到1.0）
     */
    val progressPercentage: Float
        get() = if (totalFiles > 0) {
            (scannedFiles.toFloat() / totalFiles).coerceIn(0f, 1f)
        } else {
            0f
        }
    
    init {
        require(totalFiles >= 0) { "Total files must be non-negative, got $totalFiles" }
        require(scannedFiles >= 0) { "Scanned files must be non-negative, got $scannedFiles" }
        require(scannedFiles <= totalFiles) { 
            "Scanned files ($scannedFiles) must not exceed total files ($totalFiles)" 
        }
        if (isComplete) {
            require(scannedFiles == totalFiles) { 
                "When complete, scanned files ($scannedFiles) must equal total files ($totalFiles)" 
            }
        }
    }
}
