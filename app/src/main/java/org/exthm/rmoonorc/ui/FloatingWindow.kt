/*
 *
 * Copyright (C) 2025 The AviumUI Project
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.exthm.rmoonorc.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.exthm.rmoonorc.R

private val LightColorPalette = lightColorScheme(
    primary = Color(0xFF374151),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF6B7280),
    surfaceContainerHighest = Color(0xFFF1F5F9),
    surfaceContainerLow = Color(0xFFF7F8F9),
    primaryContainer = Color(0xFFEFF6FF)
)

@Composable
fun OcrResultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorPalette,
        typography = Typography(),
        content = {
            CompositionLocalProvider(
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor = MaterialTheme.colorScheme.primary,
                    backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            ) {
                content()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrResultBottomSheet(text: String, onDismissRequest: () -> Unit) {
    OcrResultTheme {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { SheetDragger() },
            modifier = Modifier.fillMaxHeight(1.0f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            SheetContent(
                text = text,
                onClose = onDismissRequest
            )
        }
    }
}

@Composable
private fun SheetContent(text: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val placeholderText = stringResource(id = R.string.ocr_result_placeholder)
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = if (text.isBlank()) placeholderText else text
            )
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
    ) {
        TopArea(onClose = onClose)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            val scrollState = rememberScrollState()
            BasicTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                readOnly = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                cursorBrush = SolidColor(Color.Transparent)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        // 将onClose回调传递给ActionsRow
        ActionsRow(
            textFieldValue = textFieldValue,
            context = context,
            onDismiss = onClose
        )
    }
}

@Composable
private fun TopArea(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.ocr_result_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(id = R.string.action_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SheetDragger() {
    Surface(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .width(40.dp)
            .height(4.dp),
        shape = RoundedCornerShape(2.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {}
}

@Composable
private fun ActionsRow(
    textFieldValue: TextFieldValue, 
    context: Context,
    onDismiss: () -> Unit 
) {
    val selection = textFieldValue.selection
    val fullText = textFieldValue.text
    val isTextSelected = !selection.collapsed
    val selectedText = if (isTextSelected) fullText.substring(selection.start, selection.end) else ""
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionItem(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Star,
            text = stringResource(id = R.string.action_copy),
            onClick = {
                val textToCopy = if (isTextSelected) selectedText else fullText
                if (textToCopy.isNotBlank()) {
                    copyToClipboard(context, textToCopy, isSelection = isTextSelected)
                }
            }
        )
        ActionItem(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Share,
            text = stringResource(id = R.string.action_share),
            onClick = {
                val textToSend = if (isTextSelected) selectedText else fullText
                if (textToSend.isNotBlank()) {
                    onDismiss()
                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed({
                        val launchIntent = Intent().setClassName("org.avium.systemuitools", "org.avium.systemuitools.MainActivity")
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        launchIntent.putExtra("ocr_text", textToSend)
                        context.applicationContext.startActivity(launchIntent)
                    }, 300L) 
                }
            }
        )
    }
}

@Composable
private fun ActionItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String, isSelection: Boolean) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(
        context.getString(R.string.clipboard_label_ocr_result),
        text
    )
    clipboard.setPrimaryClip(clip)
    val message = if (isSelection && text.isNotEmpty()) {
        context.getString(R.string.toast_copied_selection)
    } else {
        context.getString(R.string.toast_copied_all)
    }
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
