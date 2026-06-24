package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.BookmarkItem
import com.example.data.model.DownloadItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.VideoViewModel
import kotlinx.coroutines.delay
import java.io.File

// Predefined samples for testing and immediate downloading
data class VideoSample(val title: String, val url: String, val subtitle: String)

val SAMPLE_VIDEOS = listOf(
    VideoSample("Big Buck Bunny", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "Full HD (1080p) classic animation clip"),
    VideoSample("Elephants Dream", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", "Sci-Fi cinematic project open sample"),
    VideoSample("Sintel Trailer", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4", "Epic trailer for testing high-bandwidth download"),
    VideoSample("For Bigger Blazes", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", "Colorful demo landscape stream"),
    VideoSample("Subaru Gas Station", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutOfGas.mp4", "Standard compact test clip")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: VideoViewModel, modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(0) }
    val clipboardManager = LocalClipboardManager.current

    // Observe DB updates from ViewModel
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val analysisResult by viewModel.analysisResult.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()

    val activePlayerPath by viewModel.activePlayerPath.collectAsStateWithLifecycle()
    val activePlayerTitle by viewModel.activePlayerTitle.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = BgNav,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(if (selectedTab == 0) Icons.Filled.Link else Icons.Outlined.Link, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = Color(0xFFEADDFF)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        BadgedBox(badge = {
                            val activeCount = downloads.count { it.status == "DOWNLOADING" }
                            if (activeCount > 0) {
                                Badge(containerColor = GlowMagenta) {
                                    Text(activeCount.toString(), color = Color.White)
                                }
                            }
                        }) {
                            Icon(if (selectedTab == 1) Icons.Filled.Download else Icons.Outlined.Download, contentDescription = "Downloads")
                        }
                    },
                    label = { Text("Downloads") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = Color(0xFFEADDFF)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(if (selectedTab == 2) Icons.Filled.Bookmark else Icons.Outlined.Bookmark, contentDescription = "Bookmarks") },
                    label = { Text("Bookmarks") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = Color(0xFFEADDFF)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardTab(
                    viewModel = viewModel,
                    urlInput = urlInput,
                    isAnalyzing = isAnalyzing,
                    analysisResult = analysisResult,
                    downloads = downloads,
                    bookmarks = bookmarks,
                    onPasteClick = {
                        val clip = clipboardManager.getText()
                        if (clip != null) {
                            viewModel.updateUrlInput(clip.text)
                        }
                    }
                )
                1 -> DownloadsTab(
                    downloads = downloads,
                    onPlayClick = { download ->
                        viewModel.playVideo(download.filePath, download.title)
                    },
                    onDeleteClick = { download ->
                        viewModel.deleteDownload(download)
                    },
                    onClearAll = {
                        viewModel.clearAllDownloads()
                    }
                )
                2 -> BookmarksTab(
                    bookmarks = bookmarks,
                    onSelectBookmark = { bookmark ->
                        viewModel.updateUrlInput(bookmark.url)
                        selectedTab = 0
                    },
                    onDeleteBookmark = { bookmark ->
                        viewModel.toggleBookmark(bookmark.url, bookmark.title)
                    }
                )
            }

            // High-fidelity local video player overlay
            activePlayerPath?.let { path ->
                OfflineMediaPlayer(
                    filePath = path,
                    title = activePlayerTitle ?: "Offline Media Playback",
                    onClose = { viewModel.stopPlayback() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTab(
    viewModel: VideoViewModel,
    urlInput: String,
    isAnalyzing: Boolean,
    analysisResult: com.example.ui.viewmodel.AnalysisResult?,
    downloads: List<DownloadItem>,
    bookmarks: List<BookmarkItem>,
    onPasteClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bold Typography Header Title
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "FETCH",
                    color = ColorPrimary,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.displayLarge.copy(
                        letterSpacing = (-1.5).sp,
                        lineHeight = 44.sp
                    )
                )
                Text(
                    text = "Video downloader Pro",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Decorative Hero Visual Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_video_downloader_hero),
                    contentDescription = "Cosmic Stream Header Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                startY = 80f
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        "High-Speed Downloader",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    Text(
                        "Paste Link & Stream Locally",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Link Input Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SlateGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Paste Video Link",
                        color = ColorPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { viewModel.updateUrlInput(it) },
                        placeholder = { Text("https://example.com/video.mp4", color = TextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("url_input_field"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Search
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = SlateGray,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan
                        ),
                        trailingIcon = {
                            if (urlInput.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateUrlInput("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = TextSecondary)
                                }
                            } else {
                                IconButton(onClick = onPasteClick) {
                                    Icon(Icons.Filled.ContentPaste, contentDescription = "Paste Clipboard", tint = NeonCyan)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.analyzeUrl() },
                            enabled = urlInput.isNotBlank() && !isAnalyzing,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("analyze_url_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color.White,
                                disabledContainerColor = SlateGray.copy(alpha = 0.5f),
                                disabledContentColor = TextSecondary
                            )
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyze Link", fontWeight = FontWeight.Bold)
                            }
                        }

                        val isBookmarked = remember(bookmarks, urlInput) {
                            bookmarks.any { it.url == urlInput.trim() }
                        }
                        FilledIconButton(
                            onClick = {
                                if (urlInput.isNotBlank()) {
                                    viewModel.toggleBookmark(urlInput.trim(), "")
                                }
                            },
                            enabled = urlInput.isNotBlank(),
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isBookmarked) NeonPurple else SlateGray.copy(alpha = 0.4f),
                                contentColor = if (isBookmarked) ColorOnAccent else TextPrimary,
                                disabledContainerColor = SlateGray.copy(alpha = 0.2f)
                            )
                        ) {
                            Icon(
                                if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                                contentDescription = "Save video link"
                            )
                        }
                    }
                }
            }
        }

        // Analysis Streams Result Card
        analysisResult?.let { result ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.VideoSettings, contentDescription = null, tint = NeonCyan)
                            Text(
                                "Available Video Streams",
                                color = ColorPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Text(
                            result.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Divider(color = SlateGray, thickness = 1.dp)

                        result.streams.forEach { stream ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SlateGray.copy(alpha = 0.4f))
                                    .border(BorderStroke(1.dp, SlateGray.copy(alpha = 0.8f)), RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.startDownload(result.url, "${result.title} (${stream.resolution})")
                                        viewModel.clearAnalysis()
                                        viewModel.updateUrlInput("")
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            stream.quality,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan,
                                            fontSize = 14.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (stream.resolution == "1080p") GlowMagenta else NeonPurple,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                stream.format,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Text(
                                        stream.label,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        stream.size,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                    Icon(
                                        Icons.Filled.Download,
                                        contentDescription = "Download stream option",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Sample Videos
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Quick Test Videos",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Box(
                    modifier = Modifier
                        .background(GlowMagenta.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, GlowMagenta.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "DIRECT PIPELINE",
                        color = GlowMagenta,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Render predefined direct links
        items(SAMPLE_VIDEOS) { sample ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateGray.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.updateUrlInput(sample.url)
                        viewModel.analyzeUrl()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(NeonCyan.copy(alpha = 0.1f), CircleShape)
                                .border(BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Movie, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }

                        Column {
                            Text(
                                sample.title,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                sample.subtitle,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.startDownload(sample.url, sample.title)
                        },
                        modifier = Modifier.testTag("download_sample_${sample.title.replace(" ", "_")}")
                    ) {
                        Icon(Icons.Filled.ArrowCircleDown, contentDescription = "Download sample", tint = NeonCyan, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DownloadsTab(
    downloads: List<DownloadItem>,
    onPlayClick: (DownloadItem) -> Unit,
    onDeleteClick: (DownloadItem) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DOWNLOADS",
                    color = ColorPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.displayMedium.copy(
                        letterSpacing = (-0.5).sp
                    )
                )
                Text(
                    text = "Active and completed tasks",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (downloads.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = GlowMagenta)
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(SlateGray.copy(alpha = 0.4f), CircleShape)
                            .border(BorderStroke(1.dp, SlateGray), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Text(
                        "No Downloads",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        "Pasted streams and custom high-speed downloads will appear here with active real-time progress controls.",
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(downloads) { item ->
                    DownloadCard(
                        item = item,
                        onPlayClick = { onPlayClick(item) },
                        onDeleteClick = { onDeleteClick(item) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun DownloadCard(
    item: DownloadItem,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val progressPercent = (item.progress * 100).toInt()

    Card(
        colors = CardDefaults.cardColors(containerColor = DeepSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (item.status == "DOWNLOADING") NeonCyan.copy(alpha = 0.4f) else SlateGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                when (item.status) {
                                    "COMPLETED" -> NeonCyan.copy(alpha = 0.1f)
                                    "FAILED" -> GlowMagenta.copy(alpha = 0.1f)
                                    else -> NeonPurple.copy(alpha = 0.1f)
                                },
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (item.status) {
                                "COMPLETED" -> Icons.Filled.PlayCircle
                                "FAILED" -> Icons.Filled.ErrorOutline
                                else -> Icons.Filled.Downloading
                            },
                            contentDescription = null,
                            tint = when (item.status) {
                                "COMPLETED" -> NeonCyan
                                "FAILED" -> GlowMagenta
                                else -> NeonPurple
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            item.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                if (item.status == "COMPLETED") "Completed" else if (item.status == "FAILED") "Failed" else "Downloading",
                                color = when (item.status) {
                                    "COMPLETED" -> NeonCyan
                                    "FAILED" -> GlowMagenta
                                    else -> NeonPurple
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("•", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                formatBytes(item.fileSize),
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.status == "COMPLETED") {
                        IconButton(onClick = onPlayClick, modifier = Modifier.testTag("play_video_${item.id}")) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play video", tint = NeonCyan)
                        }
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.testTag("delete_video_${item.id}")) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete download", tint = TextSecondary)
                    }
                }
            }

            if (item.status == "DOWNLOADING") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Streaming packets...",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            "$progressPercent%",
                            fontSize = 11.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = item.progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .testTag("progress_bar_${item.id}"),
                        color = NeonCyan,
                        trackColor = SlateGray
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarksTab(
    bookmarks: List<BookmarkItem>,
    onSelectBookmark: (BookmarkItem) -> Unit,
    onDeleteBookmark: (BookmarkItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "BOOKMARKS",
                color = ColorPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.displayMedium.copy(
                    letterSpacing = (-0.5).sp
                )
            )
            Text(
                text = "Saved video links and streams",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(SlateGray.copy(alpha = 0.4f), CircleShape)
                            .border(BorderStroke(1.dp, SlateGray), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Text(
                        "No Bookmarks",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        "Save high-speed download bookmarks in the dashboard tab to retrieve and resolve them quickly anytime.",
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(bookmarks) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DeepSurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateGray),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectBookmark(item) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(NeonPurple.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Bookmark, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(18.dp))
                                }

                                Column {
                                    Text(
                                        item.title,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        item.url,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(onClick = { onSelectBookmark(item) }) {
                                    Icon(Icons.Filled.Search, contentDescription = "Query Link", tint = NeonCyan)
                                }
                                IconButton(onClick = { onDeleteBookmark(item) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete bookmark", tint = TextSecondary)
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun OfflineMediaPlayer(
    filePath: String,
    title: String,
    onClose: () -> Unit
) {
    val file = remember(filePath) { File(filePath) }
    var isPlaying by remember { mutableStateOf(true) }
    var isControlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            delay(3000)
            isControlsVisible = false
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("video_player_dialog")
        ) {
            if (!file.exists()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Filled.BrokenImage, contentDescription = null, tint = GlowMagenta, modifier = Modifier.size(48.dp))
                    Text("File Missing On Disk", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = SlateGray)) {
                        Text("Close Player")
                    }
                }
            } else {
                var videoViewRef: VideoView? by remember { mutableStateOf(null) }

                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            val videoView = VideoView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                ).apply {
                                    gravity = android.view.Gravity.CENTER
                                }
                                setVideoPath(file.absolutePath)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                            addView(videoView)
                            videoViewRef = videoView
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { isControlsVisible = !isControlsVisible }
                )

                AnimatedVisibility(
                    visible = isControlsVisible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .statusBarsPadding()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = NeonCyan)
                            Text(
                                title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = onClose, modifier = Modifier.testTag("close_player_button")) {
                            Icon(Icons.Filled.Close, contentDescription = "Close player overlay", tint = Color.White)
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isControlsVisible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                videoViewRef?.let { vv ->
                                    if (vv.isPlaying) {
                                        vv.pause()
                                        isPlaying = false
                                    } else {
                                        vv.start()
                                        isPlaying = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(54.dp)
                                .background(NeonCyan, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause controls",
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            "Cinematic Looping Activated",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "Unknown size"
    val kb = bytes / 1024f
    val mb = kb / 1024f
    return if (mb >= 1f) {
        String.format("%.1f MB", mb)
    } else {
        String.format("%.1f KB", kb)
    }
}
