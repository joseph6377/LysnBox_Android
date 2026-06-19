package com.example.lysnboxandroid.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lysnboxandroid.SavedDocument
import com.example.lysnboxandroid.SourceFormat

/** Renders a book cover from embedded Base64 data, or a tasteful gradient placeholder. */
@Composable
fun CoverImage(
    document: SavedDocument,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 8,
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val bitmap = remember(document.id, document.coverImageData) {
        document.coverImageData?.let {
            try {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(modifier = modifier.clip(shape)) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = document.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            PlaceholderCover(document)
        }
    }
}

@Composable
private fun PlaceholderCover(document: SavedDocument) {
    val accent = MaterialTheme.colorScheme.primary
    val gradient = Brush.linearGradient(
        listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.45f))
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = document.title.trim().firstOrNull()?.uppercase() ?: "?",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = badge(document.sourceFormat),
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
        )
    }
}

private fun badge(format: SourceFormat): String = when (format) {
    SourceFormat.EPUB -> "EPUB"
    SourceFormat.PDF -> "PDF"
    SourceFormat.WEB -> "WEB"
    SourceFormat.TEXT -> "TEXT"
}
