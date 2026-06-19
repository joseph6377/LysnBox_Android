package com.example.lysnboxandroid

import android.speech.tts.Voice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lysnboxandroid.ui.theme.LysnType
import java.util.Locale

@Composable
fun VoicesScreen(viewModel: ReaderViewModel) {
    val voices = viewModel.availableVoices
    val languages = remember(voices) {
        voices.map { it.locale.displayLanguage }.distinct().sorted()
    }
    var selectedLanguage by remember(languages) { mutableStateOf(languages.firstOrNull()) }

    val shown = remember(voices, selectedLanguage) {
        voices.filter { selectedLanguage == null || it.locale.displayLanguage == selectedLanguage }
            .sortedBy { it.name }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Voices", style = LysnType.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "On-device narration — fully private, works offline.",
                    style = LysnType.body,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(16.dp))
                if (languages.size > 1) {
                    LanguagePills(languages, selectedLanguage) { selectedLanguage = it }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        if (!viewModel.isTtsReady) {
            item {
                Text(
                    "Loading on-device voices…",
                    style = LysnType.body,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        items(shown, key = { it.name }) { voice ->
            VoiceRow(
                voice = voice,
                isSelected = voice.name == viewModel.selectedVoice?.name,
                onSelect = { viewModel.setVoice(voice) },
                onPreview = { viewModel.previewVoice(voice) }
            )
        }
    }
}

@Composable
private fun LanguagePills(languages: List<String>, selected: String?, onSelect: (String) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(languages) { lang ->
            val active = lang == selected
            Text(
                text = lang,
                style = LysnType.subheadlineSemibold,
                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .clickable { onSelect(lang) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun VoiceRow(voice: Voice, isSelected: Boolean, onSelect: () -> Unit, onPreview: () -> Unit) {
    val display = displayName(voice)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                display.firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(display, style = LysnType.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                descriptor(voice),
                style = LysnType.caption,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
        if (isSelected) {
            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
        }
        IconButton(onClick = onPreview) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Preview", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Build a friendlier voice name from the system Voice identifier. */
internal fun displayName(voice: Voice): String {
    // System names look like "en-US-language" or "en-us-x-iol-local". Make a readable label.
    val locale = voice.locale
    val country = locale.displayCountry.takeIf { it.isNotBlank() }
    val variant = voice.name.substringAfterLast("-").replace("local", "").replace("network", "").uppercase()
    return buildString {
        append(locale.displayLanguage.ifBlank { locale.language })
        if (country != null) append(" ($country)")
        if (variant.length in 1..4) append(" $variant")
    }.trim()
}

private fun descriptor(voice: Voice): String {
    val quality = when {
        voice.quality >= Voice.QUALITY_VERY_HIGH -> "High quality"
        voice.quality >= Voice.QUALITY_NORMAL -> "Balanced"
        else -> "Standard"
    }
    val network = if (voice.isNetworkConnectionRequired) "Network" else "On-device"
    return "$quality • $network"
}
