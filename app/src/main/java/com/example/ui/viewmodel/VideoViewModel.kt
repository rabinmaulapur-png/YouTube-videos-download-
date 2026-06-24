package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.BookmarkItem
import com.example.data.model.DownloadItem
import com.example.data.repository.VideoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VideoViewModel(private val repository: VideoRepository) : ViewModel() {

    // UI States
    val downloads: StateFlow<List<DownloadItem>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkItem>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisResult = MutableStateFlow<AnalysisResult?>(null)
    val analysisResult: StateFlow<AnalysisResult?> = _analysisResult.asStateFlow()

    private val _activePlayerPath = MutableStateFlow<String?>(null)
    val activePlayerPath: StateFlow<String?> = _activePlayerPath.asStateFlow()

    private val _activePlayerTitle = MutableStateFlow<String?>(null)
    val activePlayerTitle: StateFlow<String?> = _activePlayerTitle.asStateFlow()

    fun updateUrlInput(url: String) {
        _urlInput.value = url
        if (url.isBlank()) {
            _analysisResult.value = null
        }
    }

    fun clearAnalysis() {
        _analysisResult.value = null
    }

    fun analyzeUrl() {
        val url = _urlInput.value.trim()
        if (url.isBlank()) return

        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisResult.value = null
            
            // Artificial delay to simulate a real link resolver scraping headers and sizes
            delay(1500)

            // Parse file name to display
            val name = url.substringBefore("?").substringBefore("#").substringAfterLast("/")
            val title = if (name.isNotBlank() && name.contains(".")) {
                name.substringBeforeLast(".")
            } else {
                "Online Video Stream"
            }

            // Create customized quality streams based on standard presets
            val streams = listOf(
                StreamOption("1080p FHD", "MP4 (H.264)", "1080p", "High Quality", "approx. 45 MB"),
                StreamOption("720p HD", "MP4 (H.264)", "720p", "Standard", "approx. 22 MB"),
                StreamOption("480p SD", "MP4 (H.264)", "480p", "Data Saver", "approx. 12 MB"),
                StreamOption("Audio MP3", "MP3 Audio", "128kbps", "Audio Only", "approx. 4.2 MB")
            )

            _analysisResult.value = AnalysisResult(
                title = title.replace("-", " ").replace("_", " ").capitalizeWords(),
                url = url,
                streams = streams
            )
            _isAnalyzing.value = false
        }
    }

    fun startDownload(url: String, title: String) {
        repository.startDownload(url, title)
    }

    fun deleteDownload(download: DownloadItem) {
        viewModelScope.launch {
            repository.deleteDownload(download)
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            repository.clearAllDownloads()
        }
    }

    fun toggleBookmark(url: String, title: String) {
        viewModelScope.launch {
            if (repository.isBookmarked(url)) {
                // Find and delete
                val currentBookmarks = repository.allBookmarks.stateIn(viewModelScope).value
                val match = currentBookmarks.find { it.url == url }
                if (match != null) {
                    repository.deleteBookmark(match)
                }
            } else {
                repository.insertBookmark(url, title)
            }
        }
    }

    fun playVideo(filePath: String, title: String) {
        _activePlayerPath.value = filePath
        _activePlayerTitle.value = title
    }

    fun stopPlayback() {
        _activePlayerPath.value = null
        _activePlayerTitle.value = null
    }

    // Helper to capitalize words nicely
    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } }
    }
}

data class AnalysisResult(
    val title: String,
    val url: String,
    val streams: List<StreamOption>
)

data class StreamOption(
    val quality: String,
    val format: String,
    val resolution: String,
    val label: String,
    val size: String
)

class VideoViewModelFactory(private val repository: VideoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
