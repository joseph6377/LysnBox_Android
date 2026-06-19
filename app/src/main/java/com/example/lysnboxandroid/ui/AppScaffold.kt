package com.example.lysnboxandroid.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.lysnboxandroid.ImportScreen
import com.example.lysnboxandroid.LibraryScreen
import com.example.lysnboxandroid.PlayerScreen
import com.example.lysnboxandroid.ReaderViewModel
import com.example.lysnboxandroid.Tab
import com.example.lysnboxandroid.VoicesScreen
import com.example.lysnboxandroid.ui.components.MiniPlayer
import com.example.lysnboxandroid.ui.components.PillDock

@Composable
fun AppScaffold(viewModel: ReaderViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Tab content (always present underneath the reader)
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(targetState = viewModel.selectedTab, label = "tab") { tab ->
                when (tab) {
                    Tab.LIBRARY -> LibraryScreen(viewModel)
                    Tab.IMPORT -> ImportScreen(viewModel)
                    Tab.VOICES -> VoicesScreen(viewModel)
                }
            }

            // Bottom gradient scrim to fade out scroll content behind PillDock
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            // Bottom dock + mini-player overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val activeDoc = viewModel.activeDocument
                if (activeDoc != null) {
                    val chapterTitle = activeDoc.chapters.getOrNull(viewModel.activeChapterIndex)?.title ?: ""
                    MiniPlayer(
                        document = activeDoc,
                        chapterTitle = chapterTitle,
                        isPlaying = viewModel.isPlaying,
                        onPlayPause = { viewModel.playPause() },
                        onClick = { viewModel.isReaderOpen = true }
                    )
                }
                PillDock(
                    selected = viewModel.selectedTab,
                    onSelect = { viewModel.selectedTab = it }
                )
            }
        }

        // Full-screen reader overlay
        AnimatedVisibility(
            visible = viewModel.isReaderOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { } // isolate the overlay for the slide animation
            ) {
                PlayerScreen(viewModel)
            }
        }
    }
}
