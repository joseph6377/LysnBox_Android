package com.example.lysnboxandroid

import java.util.UUID

enum class SourceFormat { EPUB, PDF, WEB, TEXT }

data class PlaybackCursor(
    var chapterIndex: Int = 0,
    var paragraphIndex: Int = 0
)

data class ChapterText(
    val index: Int,
    val title: String,
    val paragraphs: List<String>
)

data class SavedDocument(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val author: String? = null,
    val coverImageData: String? = null, // Base64 representation of a small cover image
    val importedAt: Long = System.currentTimeMillis(),
    var lastOpenedAt: Long = System.currentTimeMillis(),
    val chapters: List<ChapterText>,
    var cursor: PlaybackCursor = PlaybackCursor(),
    val sourceFormat: SourceFormat = SourceFormat.EPUB,
    val pageCount: Int? = null,
    val sourceUrl: String? = null,
    var favorite: Boolean = false
) {
    val paragraphCount: Int get() = chapters.sumOf { it.paragraphs.size }
}
