package com.example.lysnboxandroid

import android.content.Context
import com.google.gson.Gson
import java.io.File

class LibraryService(private val context: Context) {
    private val gson = Gson()
    private val libraryDir: File
        get() = File(context.filesDir, "library").apply { if (!exists()) mkdirs() }

    fun saveDocument(doc: SavedDocument) {
        val file = File(libraryDir, "${doc.id}.json")
        val json = gson.toJson(doc)
        file.writeText(json)
    }

    fun loadDocument(id: String): SavedDocument? {
        val file = File(libraryDir, "$id.json")
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), SavedDocument::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAllDocuments(): List<SavedDocument> {
        val files = libraryDir.listFiles { _, name -> name.endsWith(".json") } ?: return emptyList()
        return files.mapNotNull { file ->
            try {
                gson.fromJson(file.readText(), SavedDocument::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }.sortedByDescending { it.lastOpenedAt }
    }

    fun deleteDocument(id: String) {
        val file = File(libraryDir, "$id.json")
        if (file.exists()) {
            file.delete()
        }
    }
}
