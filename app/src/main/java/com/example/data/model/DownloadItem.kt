package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String,
    val filePath: String,
    val fileSize: Long,
    val progress: Float, // 0.0 to 1.0
    val status: String, // PENDING, DOWNLOADING, COMPLETED, FAILED, PAUSED
    val timestamp: Long = System.currentTimeMillis(),
    val mimeType: String = "video/mp4",
    val thumbnailUrl: String? = null
)
