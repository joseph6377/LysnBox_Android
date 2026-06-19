package com.example.lysnboxandroid

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

object EpubParser {
    private const val TAG = "EpubParser"

    fun parse(context: Context, uri: Uri): SavedDocument? {
        val zipBytes = readUriBytes(context, uri) ?: return null
        
        // Load zip contents in memory for easy random access
        val zipContents = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    zipContents[entry.name] = zis.readBytes()
                }
                entry = zis.nextEntry
            }
        }

        // 1. Locate container.xml to get OPF path
        val containerXml = zipContents["META-INF/container.xml"] ?: return null
        val opfPath = parseContainerXml(containerXml) ?: return null
        
        // 2. Parse OPF file
        val opfContent = zipContents[opfPath] ?: return null
        val (metadata, spineItemPaths) = parseOpfFile(opfContent, opfPath) ?: return null

        // 3. Extract cover image if present
        var coverBase64: String? = null
        metadata.coverHref?.let { relativeHref ->
            val resolvedPath = resolveRelativePath(opfPath, relativeHref)
            zipContents[resolvedPath]?.let { coverBytes ->
                coverBase64 = Base64.encodeToString(coverBytes, Base64.DEFAULT)
            }
        }

        // 4. Parse content documents
        val chapters = mutableListOf<ChapterText>()
        var chapterIndex = 0
        for (itemPath in spineItemPaths) {
            val contentBytes = zipContents[itemPath] ?: continue
            val docString = String(contentBytes, Charsets.UTF_8)
            
            val paragraphs = extractParagraphsFromHtml(docString)
            if (paragraphs.isEmpty()) continue

            val title = extractTitleFromHtml(docString) ?: "Chapter ${chapterIndex + 1}"
            chapters.add(ChapterText(index = chapterIndex, title = title, paragraphs = paragraphs))
            chapterIndex++
        }

        if (chapters.isEmpty()) {
            return null
        }

        return SavedDocument(
            title = metadata.title ?: "Untitled Audiobook",
            author = metadata.author,
            coverImageData = coverBase64,
            chapters = chapters
        )
    }

    private fun readUriBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read uri bytes", e)
            null
        }
    }

    private fun parseContainerXml(bytes: ByteArray): String? {
        return try {
            val dbf = DocumentBuilderFactory.newInstance()
            val db = dbf.newDocumentBuilder()
            val doc = db.parse(ByteArrayInputStream(bytes))
            val rootfile = doc.getElementsByTagName("rootfile").item(0) as? Element
            rootfile?.getAttribute("full-path")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing container.xml", e)
            null
        }
    }

    private data class OpfMetadata(val title: String?, val author: String?, val coverHref: String?)
    private data class OpfData(val metadata: OpfMetadata, val spinePaths: List<String>)

    private fun parseOpfFile(bytes: ByteArray, opfPath: String): OpfData? {
        return try {
            val dbf = DocumentBuilderFactory.newInstance()
            val db = dbf.newDocumentBuilder()
            val doc = db.parse(ByteArrayInputStream(bytes))
            
            // Metadata
            val title = doc.getElementsByTagName("dc:title").item(0)?.textContent
            val author = doc.getElementsByTagName("dc:creator").item(0)?.textContent
            
            // Cover href logic
            // Check for <meta name="cover" content="some-id" />
            var coverId: String? = null
            val metaElements = doc.getElementsByTagName("meta")
            for (i in 0 until metaElements.length) {
                val element = metaElements.item(i) as Element
                if (element.getAttribute("name") == "cover") {
                    coverId = element.getAttribute("content")
                    break
                }
            }

            // Manifest map (id -> href)
            val manifestMap = mutableMapOf<String, String>()
            val manifestItems = doc.getElementsByTagName("item")
            var coverHref: String? = null
            for (i in 0 until manifestItems.length) {
                val element = manifestItems.item(i) as Element
                val id = element.getAttribute("id")
                val href = element.getAttribute("href")
                manifestMap[id] = href
                
                // Fallback check if properties="cover-image"
                if (element.getAttribute("properties") == "cover-image") {
                    coverHref = href
                }
            }
            
            if (coverHref == null && coverId != null) {
                coverHref = manifestMap[coverId]
            }

            // Spine
            val spinePaths = mutableListOf<String>()
            val spineItems = doc.getElementsByTagName("itemref")
            for (i in 0 until spineItems.length) {
                val element = spineItems.item(i) as Element
                val idref = element.getAttribute("idref")
                manifestMap[idref]?.let { relativeHref ->
                    spinePaths.add(resolveRelativePath(opfPath, relativeHref))
                }
            }

            OpfData(
                metadata = OpfMetadata(title, author, coverHref),
                spinePaths = spinePaths
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing OPF file", e)
            null
        }
    }

    private fun resolveRelativePath(basePath: String, relativePath: String): String {
        val lastSlash = basePath.lastIndexOf('/')
        if (lastSlash == -1) return relativePath
        val baseDir = basePath.substring(0, lastSlash + 1)
        val full = baseDir + relativePath
        
        // Resolve references like "../" if any
        val parts = full.split("/")
        val resolvedParts = mutableListOf<String>()
        for (part in parts) {
            if (part == "..") {
                if (resolvedParts.isNotEmpty()) resolvedParts.removeAt(resolvedParts.size - 1)
            } else if (part != "." && part.isNotEmpty()) {
                resolvedParts.add(part)
            }
        }
        return resolvedParts.joinToString("/")
    }

    private fun extractTitleFromHtml(html: String): String? {
        val titleMatch = Regex("<title>([^<]*)</title>", RegexOption.IGNORE_CASE).find(html)
        if (titleMatch != null) {
            val title = titleMatch.groupValues[1].trim()
            if (title.isNotEmpty()) return title
        }
        val h1Match = Regex("<h1>([^<]*)</h1>", RegexOption.IGNORE_CASE).find(html)
        if (h1Match != null) {
            val title = h1Match.groupValues[1].trim()
            if (title.isNotEmpty()) return title
        }
        val h2Match = Regex("<h2>([^<]*)</h2>", RegexOption.IGNORE_CASE).find(html)
        if (h2Match != null) {
            val title = h2Match.groupValues[1].trim()
            if (title.isNotEmpty()) return title
        }
        return null
    }

    private fun extractParagraphsFromHtml(html: String): List<String> {
        // Extract paragraph elements or split by tags
        val paragraphs = mutableListOf<String>()
        
        // Find everything inside body
        val bodyMatch = Regex("<body[^>]*>(.*?)</body>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)
        val bodyContent = bodyMatch?.groupValues?.get(1) ?: html

        // Find matches for tags <p>, <li>, <div>, <h1>-<h6>, <dd>
        val tagRegex = Regex("<(p|li|div|h[1-6]|dd)[^>]*>(.*?)</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val matches = tagRegex.findAll(bodyContent)
        
        for (match in matches) {
            val innerText = cleanHtmlTags(match.groupValues[2])
            if (innerText.length > 5) { // Skip tiny paragraphs or formatting markers
                paragraphs.add(innerText)
            }
        }

        // If simple tag parsing yielded nothing, fallback to regex splitting on block tags
        if (paragraphs.isEmpty()) {
            val blocks = bodyContent.split(Regex("</?(?:p|div|br|li|h[1-6])[^>]*>", RegexOption.IGNORE_CASE))
            for (block in blocks) {
                val cleaned = cleanHtmlTags(block)
                if (cleaned.length > 10) {
                    paragraphs.add(cleaned)
                }
            }
        }

        return paragraphs
    }

    private fun cleanHtmlTags(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), "") // strip tags
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace(Regex("\\s+"), " ") // normalize spacing
            .trim()
    }
}
