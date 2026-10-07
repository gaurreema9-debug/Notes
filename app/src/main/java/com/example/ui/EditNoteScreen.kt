package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.PinGold
import com.example.ui.theme.PinGoldDark
import com.example.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteScreen(
    state: EditorState,
    showDeleteDialog: Boolean,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onContentValueChange: (TextFieldValue) -> Unit = { onContentChange(it.text) },
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onToggleInlineStyle: (InlineStyleType) -> Unit = {},
    onToggleBulletList: () -> Unit = {},
    onToggleNumberedList: () -> Unit = {},
    onCycleTextAlign: () -> Unit = {},
    onSetTextAlign: (EditorTextAlign) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onCutContent: () -> String = { "" },
    onCopyContent: () -> String = { "" },
    onPasteContent: (String) -> Unit = {},
    onTogglePin: () -> Unit,
    onSaveNote: () -> Unit,
    onDeleteRequest: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val isNewNote = state.noteId == null
    val titleFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(isNewNote) {
        if (isNewNote) {
            titleFocusRequester.requestFocus()
        }
    }

    val contentTfv = TextFieldValue(
        text = state.content,
        selection = state.contentSelection
    )

    val visualTransformation = remember(state.formatting) {
        RichTextVisualTransformation(state.formatting)
    }

    val isBoldActive = RichTextEditorHelper.isStyleActiveInSelection(
        formatting = state.formatting,
        selection = state.contentSelection,
        activeTypingStyles = state.activeTypingStyles,
        type = InlineStyleType.BOLD
    )
    val isItalicActive = RichTextEditorHelper.isStyleActiveInSelection(
        formatting = state.formatting,
        selection = state.contentSelection,
        activeTypingStyles = state.activeTypingStyles,
        type = InlineStyleType.ITALIC
    )
    val isUnderlineActive = RichTextEditorHelper.isStyleActiveInSelection(
        formatting = state.formatting,
        selection = state.contentSelection,
        activeTypingStyles = state.activeTypingStyles,
        type = InlineStyleType.UNDERLINE
    )
    val isBulletActive = RichTextEditorHelper.isBulletListActive(
        text = state.content,
        selection = state.contentSelection
    )
    val isNumberedActive = RichTextEditorHelper.isNumberedListActive(
        text = state.content,
        selection = state.contentSelection
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isNewNote) stringResource(R.string.new_note) else stringResource(R.string.edit_note),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("note_editor_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back and save"
                            )
                        }
                    },
                    actions = {
                        // Top Undo Quick Action
                        IconButton(
                            onClick = onUndo,
                            enabled = state.canUndo,
                            modifier = Modifier.testTag("top_undo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (state.canUndo) PinGoldDark else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                        }

                        // Top Redo Quick Action
                        IconButton(
                            onClick = onRedo,
                            enabled = state.canRedo,
                            modifier = Modifier.testTag("top_redo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo",
                                tint = if (state.canRedo) PinGoldDark else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                        }

                        // Pin / Unpin action
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.testTag("note_editor_pin_button")
                        ) {
                            Icon(
                                imageVector = if (state.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (state.isPinned) "Unpin note" else "Pin note",
                                tint = if (state.isPinned) PinGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Delete action (available for existing notes)
                        if (!isNewNote) {
                            IconButton(
                                onClick = onDeleteRequest,
                                modifier = Modifier.testTag("note_editor_delete_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Save action
                        IconButton(
                            onClick = onSaveNote,
                            modifier = Modifier.testTag("note_editor_save_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.save),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .testTag("editor_bottom_toolbar_container")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Rich Text & Undo/Redo Toolbar directly above the keyboard
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("editor_formatting_toolbar"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. ↶ Undo
                        EditorToolbarChip(
                            icon = Icons.AutoMirrored.Filled.Undo,
                            label = "↶ Undo",
                            enabled = state.canUndo,
                            active = false,
                            onClick = onUndo,
                            testTag = "editor_undo_button"
                        )

                        // 2. ↷ Redo
                        EditorToolbarChip(
                            icon = Icons.AutoMirrored.Filled.Redo,
                            label = "↷ Redo",
                            enabled = state.canRedo,
                            active = false,
                            onClick = onRedo,
                            testTag = "editor_redo_button"
                        )

                        ToolbarDivider()

                        // 3. Bold
                        EditorToolbarIconButton(
                            icon = Icons.Default.FormatBold,
                            contentDescription = "Bold",
                            active = isBoldActive,
                            onClick = {
                                contentFocusRequester.requestFocus()
                                onToggleInlineStyle(InlineStyleType.BOLD)
                            },
                            testTag = "editor_bold_button"
                        )

                        // 4. Italic
                        EditorToolbarIconButton(
                            icon = Icons.Default.FormatItalic,
                            contentDescription = "Italic",
                            active = isItalicActive,
                            onClick = {
                                contentFocusRequester.requestFocus()
                                onToggleInlineStyle(InlineStyleType.ITALIC)
                            },
                            testTag = "editor_italic_button"
                        )

                        // 5. Underline
                        EditorToolbarIconButton(
                            icon = Icons.Default.FormatUnderlined,
                            contentDescription = "Underline",
                            active = isUnderlineActive,
                            onClick = {
                                contentFocusRequester.requestFocus()
                                onToggleInlineStyle(InlineStyleType.UNDERLINE)
                            },
                            testTag = "editor_underline_button"
                        )

                        ToolbarDivider()

                        // 6. Bullet List
                        EditorToolbarIconButton(
                            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                            contentDescription = "Bullet List",
                            active = isBulletActive,
                            onClick = {
                                contentFocusRequester.requestFocus()
                                onToggleBulletList()
                            },
                            testTag = "editor_bullet_list_button"
                        )

                        // 7. Numbered List
                        EditorToolbarIconButton(
                            icon = Icons.Default.FormatListNumbered,
                            contentDescription = "Numbered List",
                            active = isNumberedActive,
                            onClick = {
                                contentFocusRequester.requestFocus()
                                onToggleNumberedList()
                            },
                            testTag = "editor_numbered_list_button"
                        )

                        ToolbarDivider()

                        // 8. Text Alignment (Left / Center / Right)
                        EditorToolbarIconButton(
                            icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                            contentDescription = "Align Left",
                            active = state.formatting.textAlign == EditorTextAlign.LEFT,
                            onClick = { onSetTextAlign(EditorTextAlign.LEFT) },
                            testTag = "editor_align_left_button"
                        )

                        EditorToolbarIconButton(
                            icon = Icons.Default.FormatAlignCenter,
                            contentDescription = "Align Center",
                            active = state.formatting.textAlign == EditorTextAlign.CENTER,
                            onClick = { onSetTextAlign(EditorTextAlign.CENTER) },
                            testTag = "editor_align_center_button"
                        )

                        EditorToolbarIconButton(
                            icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                            contentDescription = "Align Right",
                            active = state.formatting.textAlign == EditorTextAlign.RIGHT,
                            onClick = { onSetTextAlign(EditorTextAlign.RIGHT) },
                            testTag = "editor_align_right_button"
                        )

                        ToolbarDivider()

                        // 9. Copy
                        EditorToolbarChip(
                            icon = Icons.Default.ContentCopy,
                            label = "Copy",
                            enabled = state.content.isNotEmpty(),
                            active = false,
                            onClick = {
                                val copied = onCopyContent()
                                if (copied.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(copied))
                                }
                            },
                            testTag = "editor_copy_button"
                        )

                        // 10. Cut
                        EditorToolbarChip(
                            icon = Icons.Default.ContentCut,
                            label = "Cut",
                            enabled = state.content.isNotEmpty(),
                            active = false,
                            onClick = {
                                val cutText = onCutContent()
                                if (cutText.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(cutText))
                                }
                            },
                            testTag = "editor_cut_button"
                        )

                        // 11. Paste
                        EditorToolbarChip(
                            icon = Icons.Default.ContentPaste,
                            label = "Paste",
                            enabled = true,
                            active = false,
                            onClick = {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrEmpty()) {
                                    onPasteContent(clipText)
                                }
                            },
                            testTag = "editor_paste_button"
                        )

                        // 12. Select All
                        EditorToolbarChip(
                            icon = Icons.Default.SelectAll,
                            label = "Select All",
                            enabled = state.content.isNotEmpty(),
                            active = state.content.isNotEmpty() &&
                                state.contentSelection.start == 0 &&
                                state.contentSelection.end == state.content.length,
                            onClick = {
                                contentFocusRequester.requestFocus()
                                onSelectAll()
                            },
                            testTag = "editor_select_all_button"
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    // Status Bar (Word & Character Count)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val wordCount = if (state.content.isBlank()) 0 else state.content.trim().split("\\s+".toRegex()).size
                        val charCount = state.content.length

                        Text(
                            text = if (isNewNote) "Draft" else "Edited ${DateFormatter.formatDetailDate(state.updatedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "$wordCount words • $charCount chars",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Note Title Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                if (state.title.isEmpty()) {
                    Text(
                        text = stringResource(R.string.title_hint),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
                BasicTextField(
                    value = state.title,
                    onValueChange = onTitleChange,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester)
                        .testTag("note_title_input")
                )
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Note Content Input (Rich Text supported)
            val composeTextAlign: TextAlign = state.formatting.textAlign.toComposeTextAlign()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                if (state.content.isEmpty()) {
                    Text(
                        text = stringResource(R.string.content_hint),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 26.sp,
                            textAlign = composeTextAlign,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                BasicTextField(
                    value = contentTfv,
                    onValueChange = onContentValueChange,
                    visualTransformation = visualTransformation,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 26.sp,
                        textAlign = composeTextAlign,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(contentFocusRequester)
                        .testTag("note_content_input")
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = {
                Text(
                    text = stringResource(R.string.delete_note_confirm_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_note_confirm_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDelete,
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            modifier = Modifier.testTag("delete_confirm_dialog")
        )
    }
}

@Composable
private fun ToolbarDivider() {
    VerticalDivider(
        modifier = Modifier
            .height(24.dp)
            .padding(horizontal = 2.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

@Composable
private fun EditorToolbarChip(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bgColor = when {
        active -> PinGoldDark.copy(alpha = 0.22f)
        enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    }
    val contentColor = when {
        active -> PinGoldDark
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }
    val borderColor = if (active) {
        PinGoldDark
    } else if (enabled) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    } else {
        Color.Transparent
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = contentColor
            )
        }
    }
}

@Composable
private fun EditorToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bgColor = if (active) {
        PinGoldDark.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val iconColor = if (active) {
        PinGoldDark
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (active) {
        PinGoldDark
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
