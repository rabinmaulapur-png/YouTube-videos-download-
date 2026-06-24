package com.example.data.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.local.BookmarkDao
import com.example.data.local.DownloadDao
import com.example.data.model.BookmarkItem
import com.example.data.model.DownloadItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLDecoder

class VideoRepository(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val bookmarkDao: BookmarkDao
) {
    private val client = OkHttpClient.Builder().build()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()
    val allBookmarks: Flow<List<BookmarkItem>> = bookmarkDao.getAllBookmarks()

    suspend fun getDownloadByUrl(url: String): DownloadItem? {
        return downloadDao.getDownloadByUrl(url)
    }

    suspend fun insertBookmark(url: String, title: String) {
        val cleanTitle = if (title.isBlank()) getFileNameFromUrl(url) else title
        bookmarkDao.insertBookmark(BookmarkItem(url = url, title = cleanTitle))
    }

    suspend fun deleteBookmark(bookmark: BookmarkItem) {
        bookmarkDao.deleteBookmark(bookmark)
    }

    suspend fun isBookmarked(url: String): Boolean {
        return bookmarkDao.isBookmarked(url)
    }

    suspend fun deleteDownload(download: DownloadItem) {
        // Delete the physical file from disk
        withContext(Dispatchers.IO) {
            try {
                val file = File(download.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e("VideoRepository", "Error deleting file: ${e.message}")
            }
            downloadDao.deleteDownload(download)
        }
    }

    suspend fun clearAllDownloads() {
        withContext(Dispatchers.IO) {
            try {
                // Delete all downloaded files physically
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                dir?.listFiles()?.forEach { file ->
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e("VideoRepository", "Error clearing files: ${e.message}")
            }
            downloadDao.deleteAllDownloads()
        }
    }

    fun startDownload(url: String, customTitle: String? = null) {
        repositoryScope.launch {
            val title = if (customTitle.isNullOrBlank()) getFileNameFromUrl(url) else customTitle
            val fileName = getFileNameFromUrl(url)

            // Setup file destination (Internal Storage Movies directory is private and doesn't require extra runtime storage permissions)
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            if (dir != null && !dir.exists()) {
                dir.mkdirs()
            }
            val destinationFile = File(dir, "${System.currentTimeMillis()}_$fileName")

            // Check if download already registered
            val existing = downloadDao.getDownloadByUrl(url)
            val downloadId = if (existing != null) {
                if (existing.status == "COMPLETED" && File(existing.filePath).exists()) {
                    // Already downloaded
                    return@launch
                }
                // Overwrite or restart existing download
                val updatedItem = existing.copy(
                    status = "DOWNLOADING",
                    progress = 0f,
                    filePath = destinationFile.absolutePath,
                    title = title,
                    timestamp = System.currentTimeMillis()
                )
                downloadDao.updateDownload(updatedItem)
                existing.id
            } else {
                val newItem = DownloadItem(
                    url = url,
                    title = title,
                    filePath = destinationFile.absolutePath,
                    fileSize = 0L,
                    progress = 0f,
                    status = "DOWNLOADING"
                )
                downloadDao.insertDownload(newItem).toInt()
            }

            performDownload(downloadId, url, destinationFile)
        }
    }

    private suspend fun performDownload(downloadId: Int, url: String, destinationFile: File) {
        withContext(Dispatchers.IO) {
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    updateDownloadStatus(downloadId, "FAILED", 0f, 0L)
                    return@withContext
                }

                val body = response.body
                if (body == null) {
                    updateDownloadStatus(downloadId, "FAILED", 0f, 0L)
                    return@withContext
                }

                val totalBytes = body.contentLength()
                inputStream = body.byteStream()
                outputStream = FileOutputStream(destinationFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var bytesWritten = 0L
                var lastProgressUpdate = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    bytesWritten += bytesRead

                    if (totalBytes > 0) {
                        val progressPercent = ((bytesWritten * 100) / totalBytes).toInt()
                        if (progressPercent > lastProgressUpdate) {
                            lastProgressUpdate = progressPercent
                            downloadDao.updateProgress(downloadId, progressPercent / 100f, "DOWNLOADING")
                        }
                    } else {
                        // Unknown length: stream progress by estimated packets
                        val estimatedProgress = (bytesWritten % 5_000_000) / 5_000_000f // cycling progress or steady
                        downloadDao.updateProgress(downloadId, estimatedProgress, "DOWNLOADING")
                    }
                }

                outputStream.flush()
                // Successfully completed
                val finalSize = if (totalBytes > 0) totalBytes else bytesWritten
                val currentItem = downloadDao.getDownloadById(downloadId)
                if (currentItem != null) {
                    downloadDao.updateDownload(
                        currentItem.copy(
                            progress = 1.0f,
                            status = "COMPLETED",
                            fileSize = finalSize
                        )
                    )
                }

            } catch (e: Exception) {
                Log.e("VideoRepository", "Download failed for ID: $downloadId", e)
                try {
                    if (destinationFile.exists()) {
                        destinationFile.delete()
                    }
                } catch (ex: Exception) {
                    // Ignore
                }
                updateDownloadStatus(downloadId, "FAILED", 0f, 0L)
            } finally {
                try {
                    inputStream?.close()
                } catch (e: Exception) {}
                try {
                    outputStream?.close()
                } catch (e: Exception) {}
            }
        }
    }

    private suspend fun updateDownloadStatus(id: Int, status: String, progress: Float, size: Long) {
        val item = downloadDao.getDownloadById(id)
        if (item != null) {
            downloadDao.updateDownload(
                item.copy(
                    status = status,
                    progress = progress,
                    fileSize = if (size > 0) size else item.fileSize
                )
            )
        }
    }

    private fun getFileNameFromUrl(url: String): String {
        return try {
            val decodedUrl = URLDecoder.decode(url, "UTF-8")
            val uriPath = decodedUrl.substringBefore("?").substringBefore("#")
            val name = uriPath.substringAfterLast("/")
            if (name.isNotBlank() && name.contains(".")) {
                name
            } else {
                "video_${System.currentTimeMillis().toString().takeLast(6)}.mp4"
            }
        } catch (e: Exception) {
            "video_${System.currentTimeMillis().toString().takeLast(6)}.mp4"
        }
    }
}
