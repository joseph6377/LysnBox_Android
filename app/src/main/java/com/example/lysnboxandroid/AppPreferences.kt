package com.example.lysnboxandroid

import android.content.Context

/** Lightweight SharedPreferences wrapper for user reading/playback preferences. */
class AppPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("lysnbox_prefs", Context.MODE_PRIVATE)

    var readingThemeId: String
        get() = prefs.getString(KEY_THEME, "modernBlue") ?: "modernBlue"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var fontFamily: String
        get() = prefs.getString(KEY_FONT_FAMILY, "serif") ?: "serif"
        set(value) = prefs.edit().putString(KEY_FONT_FAMILY, value).apply()

    var fontSize: Float
        get() = prefs.getFloat(KEY_FONT_SIZE, 18f)
        set(value) = prefs.edit().putFloat(KEY_FONT_SIZE, value).apply()

    var hideText: Boolean
        get() = prefs.getBoolean(KEY_HIDE_TEXT, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_TEXT, value).apply()

    var speed: Float
        get() = prefs.getFloat(KEY_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEED, value).apply()

    var voiceName: String?
        get() = prefs.getString(KEY_VOICE, null)
        set(value) = prefs.edit().putString(KEY_VOICE, value).apply()

    var isGridView: Boolean
        get() = prefs.getBoolean(KEY_GRID_VIEW, true)
        set(value) = prefs.edit().putBoolean(KEY_GRID_VIEW, value).apply()

    companion object {
        private const val KEY_THEME = "reading_theme"
        private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_HIDE_TEXT = "hide_text"
        private const val KEY_SPEED = "speed"
        private const val KEY_VOICE = "voice_name"
        private const val KEY_GRID_VIEW = "grid_view"
    }
}
