package com.example.lysnboxandroid

import org.junit.Assert.assertEquals
import org.junit.Test

class TextChunkerTest {

    @Test
    fun testToParagraphs() {
        val rawText = "Hello world.\n\nThis is paragraph two.\n\n   \n\nAnd paragraph three."
        val paragraphs = TextChunker.toParagraphs(rawText)
        
        assertEquals(3, paragraphs.size)
        assertEquals("Hello world.", paragraphs[0])
        assertEquals("This is paragraph two.", paragraphs[1])
        assertEquals("And paragraph three.", paragraphs[2])
    }

    @Test
    fun testToSentences() {
        val paragraph = "This is sentence one. Sentence two! What about sentence three? Yes."
        val sentences = TextChunker.toSentences(paragraph)

        assertEquals(4, sentences.size)
        assertEquals("This is sentence one.", sentences[0])
        assertEquals("Sentence two!", sentences[1])
        assertEquals("What about sentence three?", sentences[2])
        assertEquals("Yes.", sentences[3])
    }

    @Test
    fun testReadingFontFallback() {
        assertEquals(ReadingFont.SERIF, ReadingFont.byId(null))
        assertEquals(ReadingFont.SERIF, ReadingFont.byId("invalid"))
        assertEquals(ReadingFont.MONOSPACE, ReadingFont.byId("monospace"))
        assertEquals(ReadingFont.SANS, ReadingFont.byId("sans"))
    }
}
