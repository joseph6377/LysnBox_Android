package com.example.lysnboxandroid

/** Shared helpers to turn raw extracted text into reader-friendly paragraphs. */
object TextChunker {

    /** Split a blob of text into paragraphs on blank lines, normalising whitespace. */
    fun toParagraphs(raw: String): List<String> {
        return raw
            .replace("\r\n", "\n")
            .split(Regex("\n[ \t]*\n+"))
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { it.length > 1 }
    }

    /**
     * Split a single paragraph into sentence-sized chunks. Used so very long
     * paragraphs still get reasonable look-ahead and highlight granularity.
     */
    fun toSentences(paragraph: String): List<String> {
        val sentences = Regex("(?<=[.!?])\\s+(?=[A-Z0-9\"'\\u201C])")
            .split(paragraph)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return if (sentences.isEmpty()) listOf(paragraph) else sentences
    }
}
