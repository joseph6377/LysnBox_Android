package com.example.lysnboxandroid

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

/** Extracts text from a PDF (one chapter per page) using PdfBox-Android. */
object PdfImporter {
    private const val TAG = "PdfImporter"

    fun import(context: Context, uri: Uri): SavedDocument? {
        PDFBoxResourceLoader.init(context.applicationContext)

        val pdDoc = try {
            context.contentResolver.openInputStream(uri)?.use { PDDocument.load(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load PDF", e)
            null
        } ?: return null

        return pdDoc.use { document ->
            val stripper = PDFTextStripper()
            val pageCount = document.numberOfPages
            val chapters = mutableListOf<ChapterText>()

            for (page in 1..pageCount) {
                stripper.startPage = page
                stripper.endPage = page
                val pageText = try {
                    stripper.getText(document)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed page $page", e)
                    ""
                }
                val paragraphs = TextChunker.toParagraphs(pageText)
                if (paragraphs.isNotEmpty()) {
                    chapters.add(
                        ChapterText(index = chapters.size, title = "Page $page", paragraphs = paragraphs)
                    )
                }
            }

            if (chapters.isEmpty()) return@use null

            val info = document.documentInformation
            val title = info?.title?.takeIf { it.isNotBlank() } ?: queryDisplayName(context, uri) ?: "PDF Document"
            val author = info?.author?.takeIf { it.isNotBlank() }

            SavedDocument(
                title = title,
                author = author,
                chapters = chapters,
                sourceFormat = SourceFormat.PDF,
                pageCount = pageCount
            )
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx)?.substringBeforeLast(".") else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
