package com.example.lysnboxandroid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lysnboxandroid.ui.components.CoverImage
import com.example.lysnboxandroid.ui.theme.LysnType
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(viewModel: ReaderViewModel) {
    val documents by viewModel.documents.collectAsState()
    var filter by remember { mutableStateOf(LibraryFilter.SHELF) }
    var query by remember { mutableStateOf("") }

    val filtered = remember(documents, filter, query) {
        viewModel.documentsFor(filter).filter {
            query.isBlank() || it.title.contains(query, true) || (it.author?.contains(query, true) == true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        if (viewModel.isImporting) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Importing…", style = LysnType.body, color = MaterialTheme.colorScheme.primary)
            }
            return@Box
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 160.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Header(documents.size)
                    Spacer(Modifier.height(16.dp))
                    FilterPills(filter) { filter = it }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SearchField(query) { query = it }
                        }
                        IconButton(
                            onClick = { viewModel.toggleLayoutMode() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        ) {
                            Icon(
                                imageVector = if (viewModel.isGridView) Icons.Filled.FormatListBulleted else Icons.Filled.GridView,
                                contentDescription = if (viewModel.isGridView) "Switch to List View" else "Switch to Grid View",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            if (filtered.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(filter) { viewModel.selectedTab = Tab.IMPORT }
                }
            } else {
                items(
                    items = filtered,
                    key = { it.id },
                    span = { GridItemSpan(if (viewModel.isGridView) 1 else maxLineSpan) }
                ) { doc ->
                    if (viewModel.isGridView) {
                        BookCard(
                            document = doc,
                            onClick = { viewModel.openDocument(doc) },
                            onToggleFavorite = { viewModel.toggleFavorite(doc) },
                            onDelete = { viewModel.deleteDocument(doc) }
                        )
                    } else {
                        BookRow(
                            document = doc,
                            viewModel = viewModel,
                            onClick = { viewModel.openDocument(doc) },
                            onToggleFavorite = { viewModel.toggleFavorite(doc) },
                            onDelete = { viewModel.deleteDocument(doc) }
                        )
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    FooterSection()
                }
            }
        }
    }
}

@Composable
private fun Header(bookCount: Int) {
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
    Column {
        Text(greeting, style = LysnType.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Welcome to your sanctuary.",
            style = LysnType.body,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            StatCell("$bookCount", if (bookCount == 1) "Book on shelf" else "Books on shelf")
            StatCell("100%", "Offline • On-device")
        }
    }
}

@Composable
private fun StatCell(value: String, label: String) {
    Column {
        Text(value, style = LysnType.title1, color = MaterialTheme.colorScheme.primary)
        Text(label, style = LysnType.caption, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun FilterPills(selected: LibraryFilter, onSelect: (LibraryFilter) -> Unit) {
    val options = listOf(
        LibraryFilter.SHELF to "My Shelf",
        LibraryFilter.FAVORITES to "Favorites",
        LibraryFilter.HISTORY to "History",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val active = value == selected
            Text(
                text = label,
                style = LysnType.subheadlineSemibold,
                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .clickable { onSelect(value) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text("Search your library", style = LysnType.body) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCard(
    document: SavedDocument,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column {
        Box {
            CoverImage(
                document = document,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true }),
                cornerRadius = 12
            )
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
            ) {
                Icon(
                    imageVector = if (document.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (document.favorite) "Unfavorite" else "Favorite") },
                    onClick = { menuOpen = false; onToggleFavorite() }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Filled.Delete, null) },
                    onClick = { menuOpen = false; onDelete() }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = document.title,
            style = LysnType.subheadlineSerifBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = document.author ?: "Unknown",
            style = LysnType.caption,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyState(filter: LibraryFilter, onImport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when (filter) {
                LibraryFilter.FAVORITES -> "No favorites yet"
                LibraryFilter.HISTORY -> "Nothing here yet"
                else -> "Your library is empty"
            },
            style = LysnType.title3Serif,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Import an EPUB, PDF, article, or text to start listening.",
            style = LysnType.body,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onImport,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Import content", color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookRow(
    document: SavedDocument,
    viewModel: ReaderViewModel,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    val totalParas = document.paragraphCount
    val completedParas = document.chapters.take(document.cursor.chapterIndex).sumOf { it.paragraphs.size } + document.cursor.paragraphIndex
    val progress = if (totalParas > 0) completedParas.toFloat() / totalParas else 0f
    val pct = (progress * 100).toInt()
    
    val speed = viewModel.speed
    val remainingTimeStr = getRemainingTimeStr(document, speed)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(
            document = document,
            modifier = Modifier
                .width(48.dp)
                .aspectRatio(0.68f),
            cornerRadius = 8
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.title,
                style = LysnType.subheadlineSerifBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = document.author ?: "Unknown",
                style = LysnType.caption,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProgressRing(
                    progress = progress,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "$pct% • $remainingTimeStr",
                    style = LysnType.caption,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
        
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (document.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (document.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (document.favorite) "Unfavorite" else "Favorite") },
                    onClick = { menuOpen = false; onToggleFavorite() }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Filled.Delete, null) },
                    onClick = { menuOpen = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun ProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val innerSize = size.minDimension - strokeWidth
        val radius = innerSize / 2
        
        // Draw track
        drawCircle(
            color = color.copy(alpha = 0.15f),
            radius = radius,
            style = Stroke(width = strokeWidth)
        )
        
        // Draw progress arc
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = progress * 360f,
            useCenter = false,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )
    }
}

private fun getRemainingTimeStr(document: SavedDocument, speed: Float): String {
    val chapters = document.chapters
    val cursor = document.cursor
    var remainingWords = 0
    
    val currentChapter = chapters.getOrNull(cursor.chapterIndex)
    if (currentChapter != null) {
        val currentParaIdx = cursor.paragraphIndex
        for (i in currentParaIdx until currentChapter.paragraphs.size) {
            remainingWords += currentChapter.paragraphs[i].split(Regex("\\s+")).filter { it.isNotBlank() }.size
        }
    }
    
    for (c in (cursor.chapterIndex + 1) until chapters.size) {
        val ch = chapters[c]
        for (p in ch.paragraphs) {
            remainingWords += p.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        }
    }
    
    val wordsPerMinute = 150f * speed
    val totalMinutes = remainingWords / wordsPerMinute
    
    val hours = totalMinutes.toInt() / 60
    val minutes = totalMinutes.toInt() % 60
    
    return when {
        hours > 0 -> "${hours} hrs ${minutes} mins left"
        minutes > 0 -> "${minutes} mins left"
        else -> "Completed"
    }
}

@Composable
private fun FooterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Built by Joseph Thekkekara",
            style = LysnType.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "LysnBox • 100% on-device",
            style = LysnType.caption,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
