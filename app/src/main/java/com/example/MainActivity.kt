package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.local.AppDatabase
import com.example.data.repository.VideoRepository
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VideoViewModel
import com.example.ui.viewmodel.VideoViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure edge-to-edge content is rendered correctly under the system status/navigation bars
        enableEdgeToEdge()

        // Core data dependencies
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = VideoRepository(
            context = applicationContext,
            downloadDao = database.downloadDao(),
            bookmarkDao = database.bookmarkDao()
        )

        // Initialize the MVVM VideoViewModel with its custom repository factory
        val viewModel: VideoViewModel by viewModels {
            VideoViewModelFactory(repository)
        }

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
