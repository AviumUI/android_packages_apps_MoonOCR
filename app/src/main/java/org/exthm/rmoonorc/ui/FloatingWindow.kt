package org.exthm.rmoonorc.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
    primary = Color(0xFF0D47A1),
    surface = Color(0xFFF8F9FA),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceContainerHighest = Color(0xFFE7E0EC),
    surfaceContainerLow = Color(0xFFF1EEF4),
    primaryContainer = Color(0xFFD1E3FF)
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
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
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
            modifier = Modifier.fillMaxHeight(1.0f)
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

        ActionsRow(
            textFieldValue = textFieldValue,
            context = context
        )
    }
}

@Composable
private fun TopArea(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
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
            .padding(vertical = 16.dp)
            .width(32.dp)
            .height(4.dp),
        shape = RoundedCornerShape(2.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {}
}

@Composable
private fun ActionsRow(textFieldValue: TextFieldValue, context: Context) {
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
                val textToShare = if (isTextSelected) selectedText else fullText
                if (textToShare.isNotBlank()) {
                    shareText(context, textToShare)
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

private fun shareText(context: Context, text: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(shareIntent)
}