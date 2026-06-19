package com.example.lysnboxandroid

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import com.example.lysnboxandroid.ui.components.CoverImage
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lysnboxandroid.ui.theme.LysnType
import com.example.lysnboxandroid.ui.theme.SerifFamily
import com.example.lysnboxandroid.ui.theme.ThemePalette

private enum class Sheet { NONE, CHAPTERS, SETTINGS, VOICE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(viewModel: ReaderViewModel) {
    val palette = viewModel.readingPalette
    val doc = viewModel.activeDocument ?: return
    val chapterIdx = viewModel.activeChapterIndex
    val currentChapter = doc.chapters.getOrNull(chapterIdx) ?: return
    val activeParaIdx = viewModel.activeParagraphIndex

    BackHandler(enabled = true) { viewModel.closeReader() }

    val lazyListState = rememberLazyListState()
    var sheet by remember { mutableStateOf(Sheet.NONE) }

    LaunchedEffect(chapterIdx, activeParaIdx) {
        if (currentChapter.paragraphs.isNotEmpty()) {
            lazyListState.animateScrollToItem(activeParaIdx.coerceIn(0, currentChapter.paragraphs.lastIndex))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.appBackground)
            .statusBarsPadding()
    ) {
        ReaderTopBar(
            title = doc.title,
            chapter = currentChapter.title,
            isPlaying = viewModel.isPlaying,
            palette = palette,
            onBack = { viewModel.closeReader() },
            onChapters = { sheet = Sheet.CHAPTERS },
            onSettings = { sheet = Sheet.SETTINGS },
        )

        Box(modifier = Modifier.weight(1f)) {
            if (viewModel.hideText) {
                HiddenTextView(viewModel = viewModel, palette = palette)
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
                ) {
                    itemsIndexed(currentChapter.paragraphs) { idx, paragraph ->
                        val isActive = idx == activeParaIdx
                        ParagraphView(
                            text = paragraph,
                            isActive = isActive,
                            wordRange = if (isActive) viewModel.activeWordRange else null,
                            fontSize = viewModel.fontSize,
                            fontFamily = viewModel.readingFont.fontFamily,
                            palette = palette,
                            onClick = { viewModel.seekTo(chapterIdx, idx) }
                        )
                    }
                }
            }
        }

        PlaybackProgressSection(
            viewModel = viewModel,
            palette = palette,
            currentChapter = currentChapter,
            chapterIdx = chapterIdx,
            activeParaIdx = activeParaIdx
        )

        TransportBar(
            isPlaying = viewModel.isPlaying,
            palette = palette,
            hasPrevChapter = chapterIdx > 0,
            hasNextChapter = chapterIdx + 1 < doc.chapters.size,
            onPrevChapter = { viewModel.seekTo(chapterIdx - 1, 0) },
            onNextChapter = { viewModel.seekTo(chapterIdx + 1, 0) },
            onPlayPause = { viewModel.playPause() },
            onBack30 = { viewModel.skipBackward() },
            onForward30 = { viewModel.skipForward() },
        )
    }

    when (sheet) {
        Sheet.CHAPTERS -> ChaptersSheet(doc, chapterIdx, palette, onSelect = {
            viewModel.seekTo(it, 0); sheet = Sheet.NONE
        }, onDismiss = { sheet = Sheet.NONE })
        Sheet.SETTINGS -> SettingsSheet(
            viewModel, palette,
            onPickVoice = { sheet = Sheet.VOICE },
            onDismiss = { sheet = Sheet.NONE }
        )
        Sheet.VOICE -> VoiceSheet(viewModel, palette, onDismiss = { sheet = Sheet.SETTINGS })
        Sheet.NONE -> {}
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    chapter: String,
    isPlaying: Boolean,
    palette: ThemePalette,
    onBack: () -> Unit,
    onChapters: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = palette.accent)
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                style = LysnType.bodyMedium,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(chapter, style = LysnType.caption, color = palette.accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onChapters) { Icon(Icons.Filled.List, "Chapters", tint = palette.accent) }
        IconButton(onClick = onSettings) { Icon(Icons.Filled.Tune, "Settings", tint = palette.accent) }
    }
}

@Composable
private fun ParagraphView(
    text: String,
    isActive: Boolean,
    wordRange: IntRange?,
    fontSize: Float,
    fontFamily: FontFamily,
    palette: ThemePalette,
    onClick: () -> Unit,
) {
    val annotated: AnnotatedString = remember(text, isActive, wordRange) {
        buildAnnotatedString {
            append(text)
            if (isActive && wordRange != null) {
                val start = wordRange.first.coerceIn(0, text.length)
                val end = (wordRange.last + 1).coerceIn(start, text.length)
                if (end > start) {
                    addStyle(SpanStyle(background = palette.activeWordBg, color = palette.activeWordFg), start, end)
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) palette.activeParagraphBg else Color.Transparent)
            .clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = annotated,
            fontFamily = fontFamily,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.55f).sp,
            color = if (isActive) palette.textPrimary else palette.textPrimary.copy(alpha = 0.82f),
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun HiddenTextView(viewModel: ReaderViewModel, palette: ThemePalette) {
    val doc = viewModel.activeDocument ?: return
    val currentChapter = doc.chapters.getOrNull(viewModel.activeChapterIndex)
    val isPlaying = viewModel.isPlaying

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Cover art with soft glow card look
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(palette.surface)
                .border(1.dp, palette.border, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            CoverImage(
                document = doc,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 20
            )
        }

        Spacer(Modifier.height(36.dp))

        // Book Title
        Text(
            text = doc.title,
            style = LysnType.title3Serif.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
            color = palette.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        // Author
        Text(
            text = doc.author ?: "Unknown Author",
            style = LysnType.body,
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        // Chapter Title
        Text(
            text = currentChapter?.title ?: "",
            style = LysnType.bodyMedium.copy(color = palette.accent),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        // Spotify-like bouncing waveform animation
        SpotifyLikeWaveform(
            isPlaying = isPlaying,
            color = palette.accent,
            modifier = Modifier
                .width(140.dp)
                .height(40.dp)
        )
    }
}

@Composable
fun SpotifyLikeWaveform(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val barCount = 13
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    val heights = (0 until barCount).map { index ->
        if (isPlaying) {
            infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 350 + (index * 60) % 350,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
        } else {
            remember { mutableStateOf(0.15f) }
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { heightState ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightState.value)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun TransportBar(
    isPlaying: Boolean,
    palette: ThemePalette,
    hasPrevChapter: Boolean,
    hasNextChapter: Boolean,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onPlayPause: () -> Unit,
    onBack30: () -> Unit,
    onForward30: () -> Unit,
) {
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Chapter
            IconButton(onClick = onPrevChapter, enabled = hasPrevChapter, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous Chapter",
                    tint = if (hasPrevChapter) palette.textPrimary.copy(alpha = 0.7f) else palette.textPrimary.copy(alpha = 0.25f),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))

            // Replay 30s
            IconButton(onClick = onBack30, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Replay30, "Back", tint = palette.textPrimary.copy(alpha = 0.8f), modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(16.dp))

            // Play/Pause Center Action
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(palette.accent)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.width(16.dp))

            // Forward 30s
            IconButton(onClick = onForward30, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Forward30, "Forward", tint = palette.textPrimary.copy(alpha = 0.8f), modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))

            // Next Chapter
            IconButton(onClick = onNextChapter, enabled = hasNextChapter, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next Chapter",
                    tint = if (hasNextChapter) palette.textPrimary.copy(alpha = 0.7f) else palette.textPrimary.copy(alpha = 0.25f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChaptersSheet(
    doc: SavedDocument,
    currentChapter: Int,
    palette: ThemePalette,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.surface) {
        Text(
            "Chapters",
            style = LysnType.title3,
            color = palette.textPrimary,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
            itemsIndexed(doc.chapters) { idx, ch ->
                val active = idx == currentChapter
                Text(
                    text = ch.title,
                    style = LysnType.body,
                    color = if (active) palette.accent else palette.textPrimary,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(idx) }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    viewModel: ReaderViewModel,
    palette: ThemePalette,
    onPickVoice: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Reading theme
            SectionLabel("Reading theme", palette)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemePalette.all.forEach { p ->
                    val active = p.id == palette.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { viewModel.setReadingTheme(p) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(p.appBackground)
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(p.accent)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            p.displayName.substringBefore(" "),
                            style = LysnType.caption,
                            color = if (active) palette.accent else palette.textSecondary,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Font style
            SectionLabel("Font style", palette)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReadingFont.values().forEach { font ->
                    val active = viewModel.readingFont == font
                    Text(
                        text = font.displayName,
                        style = LysnType.subheadlineSemibold,
                        color = if (active) Color.White else palette.textPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) palette.accent else palette.accent.copy(alpha = 0.08f))
                            .clickable { viewModel.updateReadingFont(font) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }

            // Font size
            SectionLabel("Font size — ${viewModel.fontSize.toInt()}sp", palette)
            Slider(
                value = viewModel.fontSize,
                onValueChange = { viewModel.updateFontSize(it) },
                valueRange = 14f..28f,
                steps = 13,
                colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
            )

            // Speed
            SectionLabel("Speed — ${"%.1fx".format(viewModel.speed)}", palette)
            Slider(
                value = viewModel.speed,
                onValueChange = { viewModel.setPlaybackSpeed(it) },
                valueRange = 0.5f..2.0f,
                steps = 14,
                colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
            )

            // Voice
            SectionLabel("Narration voice", palette)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.accent.copy(alpha = 0.08f))
                    .clickable(enabled = viewModel.availableVoices.isNotEmpty(), onClick = onPickVoice)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewModel.selectedVoice?.let { displayName(it) }
                        ?: if (viewModel.availableVoices.isEmpty()) "Loading voices…" else "Default",
                    style = LysnType.body,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Filled.Tune, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
            }

            // Hide text
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Listening mode (hide text)", style = LysnType.body, color = palette.textPrimary, modifier = Modifier.weight(1f))
                Switch(
                    checked = viewModel.hideText,
                    onCheckedChange = { viewModel.updateHideText(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = palette.accent)
                )
            }

            // Sleep timer
            SectionLabel("Sleep timer", palette)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val options = listOf<Pair<String, Int?>>("Off" to null, "5m" to 5, "15m" to 15, "30m" to 30, "60m" to 60)
                options.forEach { (label, minutes) ->
                    val active = viewModel.sleepTimerMinutes == minutes
                    Text(
                        label,
                        style = LysnType.subheadlineSemibold,
                        color = if (active) Color.White else palette.textPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) palette.accent else palette.accent.copy(alpha = 0.08f))
                            .clickable { viewModel.setSleepTimer(minutes) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceSheet(viewModel: ReaderViewModel, palette: ThemePalette, onDismiss: () -> Unit) {
    val voices = viewModel.availableVoices
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.surface) {
        Text(
            "Narration voice",
            style = LysnType.title3,
            color = palette.textPrimary,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
        )
        if (voices.isEmpty()) {
            Text(
                "No on-device voices available yet.",
                style = LysnType.body,
                color = palette.textSecondary,
                modifier = Modifier.padding(20.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                itemsIndexed(voices) { _, voice ->
                    val selected = voice.name == viewModel.selectedVoice?.name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setVoice(voice) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayName(voice),
                            style = LysnType.body,
                            color = if (selected) palette.accent else palette.textPrimary,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { viewModel.previewVoice(voice) }) {
                            Icon(Icons.Filled.PlayArrow, "Preview", tint = palette.accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, palette: ThemePalette) {
    Text(text, style = LysnType.subheadlineSemibold, color = palette.textSecondary)
}

private fun estimateRemainingTimeMinutes(paragraphs: List<String>, activeParaIdx: Int, speed: Float): Float {
    if (activeParaIdx >= paragraphs.size) return 0f
    var remainingWords = 0
    for (i in activeParaIdx until paragraphs.size) {
        val para = paragraphs[i]
        remainingWords += para.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
    val wordsPerMinute = 150f * speed
    return remainingWords / wordsPerMinute
}

private fun formatRemainingTime(minutes: Float): String {
    val totalSeconds = (minutes * 60).toInt()
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return if (mins > 0) "${mins}m ${secs}s left" else "${secs}s left"
}

@Composable
private fun PlaybackProgressSection(
    viewModel: ReaderViewModel,
    palette: ThemePalette,
    currentChapter: ChapterText,
    chapterIdx: Int,
    activeParaIdx: Int
) {
    val totalParas = currentChapter.paragraphs.size
    val progress = if (totalParas > 0) activeParaIdx.toFloat() / totalParas.coerceAtLeast(1) else 0f
    val totalChapters = viewModel.activeDocument?.chapters?.size ?: 1

    val remainingMinutes = estimateRemainingTimeMinutes(currentChapter.paragraphs, activeParaIdx, viewModel.speed)
    val remainingTimeStr = formatRemainingTime(remainingMinutes)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Scrubber Slider
        Slider(
            value = activeParaIdx.toFloat(),
            onValueChange = { viewModel.seekTo(chapterIdx, it.toInt()) },
            valueRange = 0f..maxOf(1f, (totalParas - 1).toFloat()),
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.accent.copy(alpha = 0.24f)
            ),
            modifier = Modifier.fillMaxWidth().height(24.dp)
        )

        Spacer(Modifier.height(4.dp))

        // Progress labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pct = (progress * 100).toInt()
            Text(
                text = "Para ${activeParaIdx + 1} of $totalParas ($pct%) • $remainingTimeStr",
                style = LysnType.caption,
                color = palette.textSecondary
            )

            Text(
                text = "Chapter ${chapterIdx + 1} of $totalChapters",
                style = LysnType.caption,
                color = palette.textSecondary
            )
        }
    }
}
