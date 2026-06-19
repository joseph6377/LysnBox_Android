package com.example.lysnboxandroid

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.lysnboxandroid.ui.theme.LysnType

@Composable
fun ImportScreen(viewModel: ReaderViewModel) {
    var showUrlDialog by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? -> uri?.let { viewModel.importFile(it) } }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 240.dp)
    ) {
        Text("Import", style = LysnType.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Add a book, document, article, or your own text.",
            style = LysnType.body,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(24.dp))

        ImportCard(
            icon = Icons.Filled.UploadFile,
            title = "Upload a file",
            subtitle = "EPUB or PDF from your device",
            onClick = { filePicker.launch(arrayOf("application/epub+zip", "application/pdf")) }
        )
        Spacer(Modifier.height(16.dp))
        ImportCard(
            icon = Icons.Filled.Link,
            title = "Paste a link",
            subtitle = "Read a web article aloud",
            onClick = { showUrlDialog = true }
        )
        Spacer(Modifier.height(16.dp))
        ImportCard(
            icon = Icons.Filled.TextFields,
            title = "Write or paste text",
            subtitle = "Turn any text into audio",
            onClick = { showTextDialog = true }
        )

        viewModel.importError?.let {
            Spacer(Modifier.height(20.dp))
            Text(it, style = LysnType.body, color = MaterialTheme.colorScheme.error)
        }
    }

    if (showUrlDialog) {
        InputDialog(
            title = "Paste a link",
            placeholder = "https://example.com/article",
            singleLine = true,
            confirmLabel = "Import",
            onDismiss = { showUrlDialog = false },
            onConfirm = { text, _ ->
                showUrlDialog = false
                if (text.isNotBlank()) viewModel.importUrl(text.trim())
            }
        )
    }

    if (showTextDialog) {
        InputDialog(
            title = "Paste text",
            placeholder = "Paste or type the text you want read aloud…",
            singleLine = false,
            confirmLabel = "Create",
            withTitleField = true,
            onDismiss = { showTextDialog = false },
            onConfirm = { text, titleField ->
                showTextDialog = false
                if (text.isNotBlank()) viewModel.importText(text, titleField?.takeIf { it.isNotBlank() })
            }
        )
    }
}

@Composable
private fun ImportCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = LysnType.title3Serif, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = LysnType.caption, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun InputDialog(
    title: String,
    placeholder: String,
    singleLine: Boolean,
    confirmLabel: String,
    withTitleField: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (text: String, title: String?) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var titleField by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(text, titleField) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(title, style = LysnType.title3) },
        text = {
            Column {
                if (withTitleField) {
                    OutlinedTextField(
                        value = titleField,
                        onValueChange = { titleField = it },
                        label = { Text("Title (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(placeholder) },
                    singleLine = singleLine,
                    minLines = if (singleLine) 1 else 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
