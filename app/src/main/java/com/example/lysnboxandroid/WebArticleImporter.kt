package com.example.lysnboxandroid

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

/**
 * Fetches a web page and extracts its main article text using a lightweight
 * readability-style heuristic (Jsoup). Mirrors iOS WebArticleImporter.
 */
object WebArticleImporter {
    private const val TAG = "WebArticleImporter"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val UA =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36"

    fun import(url: String): SavedDocument? {
        val normalized = if (url.startsWith("http")) url else "https://$url"
        val html = try {
            val request = Request.Builder().url(normalized).header("User-Agent", UA).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.string() ?: return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch $normalized", e)
            return null
        }

        val doc = Jsoup.parse(html, normalized)
        val title = extractTitle(doc)
        val author = doc.select("meta[name=author]").attr("content").takeIf { it.isNotBlank() }
        val cover = doc.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }

        val paragraphs = extractArticleParagraphs(doc)
        if (paragraphs.isEmpty()) return null

        val chapter = ChapterText(index = 0, title = title, paragraphs = paragraphs)
        return SavedDocument(
            title = title,
            author = author,
            chapters = listOf(chapter),
            sourceFormat = SourceFormat.WEB,
            sourceUrl = normalized,
            coverImageData = null // og:image is a remote URL; covers handled by Coil at display time
        )
    }

    private fun extractTitle(doc: Document): String {
        doc.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }?.let { return it }
        doc.title().takeIf { it.isNotBlank() }?.let { return it }
        doc.select("h1").firstOrNull()?.text()?.takeIf { it.isNotBlank() }?.let { return it }
        return "Web Article"
    }

    /** Pick the densest content container and pull its paragraphs. */
    private fun extractArticleParagraphs(doc: Document): List<String> {
        // Strip noise.
        doc.select("script, style, nav, header, footer, aside, form, noscript, figure, .ad, .ads, .advert").remove()

        val candidates = doc.select("article, main, [role=main], .article, .post, .entry-content, #content")
        val root: Element = candidates
            .maxByOrNull { it.select("p").sumOf { p -> p.text().length } }
            ?: doc.body()

        val paragraphs = root.select("p, h1, h2, h3, li, blockquote")
            .map { it.text().replace(Regex("\\s+"), " ").trim() }
            .filter { it.length > 30 || it.endsWith(":") }

        // Fallback: whole-body text split.
        if (paragraphs.size < 2) {
            return TextChunker.toParagraphs(doc.body().text())
        }
        return paragraphs
    }
}
