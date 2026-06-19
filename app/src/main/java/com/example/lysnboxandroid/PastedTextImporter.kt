package com.example.lysnboxandroid

/** Converts raw pasted text into a single-chapter [SavedDocument]. */
object PastedTextImporter {

    fun import(text: String, providedTitle: String? = null): SavedDocument? {
        val paragraphs = TextChunker.toParagraphs(text)
        if (paragraphs.isEmpty()) return null

        val title = providedTitle?.takeIf { it.isNotBlank() }
            ?: paragraphs.first().take(60).let { if (it.length == 60) "$it…" else it }

        val chapter = ChapterText(index = 0, title = "Text", paragraphs = paragraphs)
        return SavedDocument(
            title = title,
            author = null,
            chapters = listOf(chapter),
            sourceFormat = SourceFormat.TEXT
        )
    }
}
