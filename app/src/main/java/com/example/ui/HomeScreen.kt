package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Note
import com.example.data.NoteSection
import com.example.data.NoteViewMode
import com.example.ui.theme.PinGold
import com.example.ui.theme.PinGoldDark
import com.example.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    notes: List<Note>,
    searchQuery: String = "",
    viewMode: NoteViewMode = NoteViewMode.LIST,
    selectedSection: NoteSection = NoteSection.ALL,
    pinDialogState: PinDialogState? = null,
    onSearchQueryChange: (String) -> Unit = {},
    onClearSearch: () -> Unit = {},
    onSelectSection: (NoteSection) -> Unit = {},
    onLockNoteRequested: (Note) -> Unit = {},
    onSetFourDigitPin: (String, String, Note) -> Unit = { _, _, _ -> },
    onUnlockPinSubmitted: (String, Note, UnlockAction) -> Unit = { _, _, _ -> },
    onDismissPinDialog: () -> Unit = {},
    onMoveNoteToTrash: (Note) -> Unit = {},
    onRestoreNote: (Note) -> Unit = {},
    onPermanentDeleteNote: (Note) -> Unit = {},
    onViewModeChange: (NoteViewMode) -> Unit = {},
    onNoteClick: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onAddNoteClick: () -> Unit,
    onOpenAbout: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val pinnedNotes = notes.filter { it.isPinned }
    val otherNotes = notes.filter { !it.isPinned }
    val isTrashView = selectedSection == NoteSection.TRASH
    val isLockedView = selectedSection == NoteSection.LOCKED
    val isSearching = searchQuery.isNotBlank()

    BackHandler(enabled = isSearching || selectedSection != NoteSection.ALL) {
        if (isSearching) {
            onClearSearch()
        } else {
            onSelectSection(NoteSection.ALL)
        }
    }

    var showLeftFilterMenu by remember { mutableStateOf(false) }
    var showViewMenu by remember { mutableStateOf(false) }
    var longPressedNote by remember { mutableStateOf<Note?>(null) }
    var notePendingPermanentDelete by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    // Left Top View/Filter Toggle Button
                    Box {
                        IconButton(
                            onClick = { showLeftFilterMenu = true },
                            modifier = Modifier.testTag("left_filter_toggle_button")
                        ) {
                            Icon(
                                imageVector = when (selectedSection) {
                                    NoteSection.TRASH -> Icons.Default.Delete
                                    NoteSection.LOCKED -> Icons.Default.Lock
                                    NoteSection.ALL -> Icons.Default.FilterList
                                },
                                contentDescription = "Filter Notes",
                                tint = if (selectedSection != NoteSection.ALL) {
                                    PinGoldDark
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }

                        DropdownMenu(
                            expanded = showLeftFilterMenu,
                            onDismissRequest = { showLeftFilterMenu = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = Color(0xFF1E293B),
                            tonalElevation = 8.dp,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .width(250.dp)
                                .testTag("left_filter_dropdown_menu")
                        ) {
                            // Option 1: 🗑️ हाल ही में डिलीट किए गए
                            val isTrashSelected = selectedSection == NoteSection.TRASH
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "🗑️ " + stringResource(R.string.recently_deleted_option),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isTrashSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isTrashSelected) PinGoldDark else Color.White
                                    )
                                },
                                trailingIcon = {
                                    if (isTrashSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = PinGoldDark,
                                            modifier = Modifier.testTag("trash_filter_checkmark")
                                        )
                                    }
                                },
                                onClick = {
                                    if (isTrashSelected) {
                                        onSelectSection(NoteSection.ALL)
                                    } else {
                                        onSelectSection(NoteSection.TRASH)
                                    }
                                    showLeftFilterMenu = false
                                },
                                modifier = Modifier
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isTrashSelected) PinGoldDark.copy(alpha = 0.16f)
                                        else Color.Transparent
                                    )
                                    .testTag("filter_option_recently_deleted")
                            )

                            // Option 2: 🔒 लॉक नोट
                            val isLockedSelected = selectedSection == NoteSection.LOCKED
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "🔒 " + stringResource(R.string.locked_notes_option),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isLockedSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isLockedSelected) PinGoldDark else Color.White
                                    )
                                },
                                trailingIcon = {
                                    if (isLockedSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = PinGoldDark,
                                            modifier = Modifier.testTag("locked_filter_checkmark")
                                        )
                                    }
                                },
                                onClick = {
                                    if (isLockedSelected) {
                                        onSelectSection(NoteSection.ALL)
                                    } else {
                                        onSelectSection(NoteSection.LOCKED)
                                    }
                                    showLeftFilterMenu = false
                                },
                                modifier = Modifier
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isLockedSelected) PinGoldDark.copy(alpha = 0.16f)
                                        else Color.Transparent
                                    )
                                    .testTag("filter_option_locked_notes")
                            )
                        }
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val headerText = when (selectedSection) {
                            NoteSection.TRASH -> stringResource(R.string.recently_deleted_option)
                            NoteSection.LOCKED -> stringResource(R.string.locked_notes_option)
                            NoteSection.ALL -> stringResource(R.string.app_name)
                        }
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (selectedSection != NoteSection.ALL) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { onSelectSection(NoteSection.ALL) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("clear_section_filter_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Back to all notes",
                                    tint = PinGoldDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else if (notes.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${notes.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Right Top View Mode Toggle Button (List View / Card View)
                    Box {
                        IconButton(
                            onClick = { showViewMenu = true },
                            modifier = Modifier.testTag("view_mode_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (viewMode == NoteViewMode.LIST) {
                                    Icons.AutoMirrored.Filled.ViewList
                                } else {
                                    Icons.Default.GridView
                                },
                                contentDescription = stringResource(R.string.view_mode_toggle),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = showViewMenu,
                            onDismissRequest = { showViewMenu = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = Color(0xFF1E293B),
                            tonalElevation = 8.dp,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .width(220.dp)
                                .testTag("view_mode_dropdown_menu")
                        ) {
                            val isListSelected = viewMode == NoteViewMode.LIST
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.list_view_label),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isListSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isListSelected) PinGoldDark else Color.White
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ViewList,
                                        contentDescription = null,
                                        tint = if (isListSelected) PinGoldDark else Color.White.copy(alpha = 0.85f)
                                    )
                                },
                                trailingIcon = {
                                    if (isListSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = PinGoldDark,
                                            modifier = Modifier.testTag("list_view_checkmark")
                                        )
                                    }
                                },
                                onClick = {
                                    onViewModeChange(NoteViewMode.LIST)
                                    showViewMenu = false
                                },
                                modifier = Modifier
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isListSelected) PinGoldDark.copy(alpha = 0.16f)
                                        else Color.Transparent
                                    )
                                    .testTag("view_mode_list_option")
                            )

                            val isCardSelected = viewMode == NoteViewMode.CARD
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.card_view_label),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isCardSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isCardSelected) PinGoldDark else Color.White
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = null,
                                        tint = if (isCardSelected) PinGoldDark else Color.White.copy(alpha = 0.85f)
                                    )
                                },
                                trailingIcon = {
                                    if (isCardSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = PinGoldDark,
                                            modifier = Modifier.testTag("card_view_checkmark")
                                        )
                                    }
                                },
                                onClick = {
                                    onViewModeChange(NoteViewMode.CARD)
                                    showViewMenu = false
                                },
                                modifier = Modifier
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isCardSelected) PinGoldDark.copy(alpha = 0.16f)
                                        else Color.Transparent
                                    )
                                    .testTag("view_mode_card_option")
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenAbout,
                        modifier = Modifier.testTag("about_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About & Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (!isTrashView) {
                FloatingActionButton(
                    onClick = onAddNoteClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(64.dp)
                        .testTag("add_note_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create note",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        bottomBar = {
            AdMobBannerBar(
                adUnitId = AdMobTestAdHelper.TEST_BANNER_AD_UNIT_ID
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_hint),
                        tint = if (isSearching) PinGoldDark else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = onClearSearch,
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = PinGoldDark,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_notes_input")
            )

            if (notes.isEmpty()) {
                EmptyNotesState(
                    isSearching = isSearching,
                    selectedSection = selectedSection,
                    onCreateFirstNote = onAddNoteClick
                )
            } else {
                when (viewMode) {
                    NoteViewMode.LIST -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("notes_list"),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!isTrashView && !isLockedView && pinnedNotes.isNotEmpty() && otherNotes.isNotEmpty()) {
                                item(key = "section_pinned") {
                                    SectionHeader(
                                        title = stringResource(R.string.pinned),
                                        count = pinnedNotes.size
                                    )
                                }
                            }

                            items(
                                items = if (!isTrashView && !isLockedView && pinnedNotes.isNotEmpty() && otherNotes.isNotEmpty()) pinnedNotes else notes,
                                key = { it.id }
                            ) { note ->
                                NoteCard(
                                    note = note,
                                    isTrashItem = isTrashView,
                                    onClick = {
                                        if (isTrashView) {
                                            longPressedNote = note
                                        } else {
                                            onNoteClick(note)
                                        }
                                    },
                                    onLongClick = { longPressedNote = note },
                                    onTogglePin = { onTogglePin(note) }
                                )
                            }

                            if (!isTrashView && !isLockedView && pinnedNotes.isNotEmpty() && otherNotes.isNotEmpty()) {
                                item(key = "section_others") {
                                    SectionHeader(
                                        title = stringResource(R.string.others),
                                        count = otherNotes.size
                                    )
                                }

                                items(
                                    items = otherNotes,
                                    key = { it.id }
                                ) { note ->
                                    NoteCard(
                                        note = note,
                                        isTrashItem = false,
                                        onClick = { onNoteClick(note) },
                                        onLongClick = { longPressedNote = note },
                                        onTogglePin = { onTogglePin(note) }
                                    )
                                }
                            }
                        }
                    }

                    NoteViewMode.CARD -> {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("notes_grid"),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalItemSpacing = 12.dp
                        ) {
                            if (!isTrashView && !isLockedView && pinnedNotes.isNotEmpty() && otherNotes.isNotEmpty()) {
                                item(
                                    key = "grid_section_pinned",
                                    span = StaggeredGridItemSpan.FullLine
                                ) {
                                    SectionHeader(
                                        title = stringResource(R.string.pinned),
                                        count = pinnedNotes.size
                                    )
                                }
                            }

                            items(
                                items = if (!isTrashView && !isLockedView && pinnedNotes.isNotEmpty() && otherNotes.isNotEmpty()) pinnedNotes else notes,
                                key = { it.id }
                            ) { note ->
                                NoteGridCard(
                                    note = note,
                                    isTrashItem = isTrashView,
                                    onClick = {
                                        if (isTrashView) {
                                            longPressedNote = note
                                        } else {
                                            onNoteClick(note)
                                        }
                                    },
                                    onLongClick = { longPressedNote = note },
                                    onTogglePin = { onTogglePin(note) }
                                )
                            }

                            if (!isTrashView && !isLockedView && pinnedNotes.isNotEmpty() && otherNotes.isNotEmpty()) {
                                item(
                                    key = "grid_section_others",
                                    span = StaggeredGridItemSpan.FullLine
                                ) {
                                    SectionHeader(
                                        title = stringResource(R.string.others),
                                        count = otherNotes.size
                                    )
                                }

                                items(
                                    items = otherNotes,
                                    key = { it.id }
                                ) { note ->
                                    NoteGridCard(
                                        note = note,
                                        isTrashItem = false,
                                        onClick = { onNoteClick(note) },
                                        onLongClick = { longPressedNote = note },
                                        onTogglePin = { onTogglePin(note) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Long Press Rounded ModalBottomSheet
    longPressedNote?.let { targetNote ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { longPressedNote = null },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = Color(0xFF1E293B),
            contentColor = Color.White,
            modifier = Modifier.testTag("note_long_press_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = if (targetNote.isLocked) {
                        stringResource(R.string.locked_note_mask)
                    } else {
                        targetNote.title.ifBlank { "Untitled Note" }
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PinGoldDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (!targetNote.isDeleted) {
                    // Active Note Long Press Options:
                    // 1. 🗑️ Delete Note
                    BottomSheetOptionRow(
                        icon = Icons.Outlined.DeleteOutline,
                        label = "🗑️ " + stringResource(R.string.delete_note_action),
                        tint = Color(0xFFF87171),
                        onClick = {
                            val noteToTrash = targetNote
                            longPressedNote = null
                            onMoveNoteToTrash(noteToTrash)
                        },
                        testTag = "bottom_sheet_delete_note"
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 2. 🔒 Lock Note (or Unlock Note if already locked)
                    BottomSheetOptionRow(
                        icon = if (targetNote.isLocked) Icons.Default.LockOpen else Icons.Outlined.Lock,
                        label = if (targetNote.isLocked) {
                            "🔓 " + stringResource(R.string.unlock_note_action)
                        } else {
                            "🔒 " + stringResource(R.string.lock_note_action)
                        },
                        tint = PinGoldDark,
                        onClick = {
                            val noteToLock = targetNote
                            longPressedNote = null
                            onLockNoteRequested(noteToLock)
                        },
                        testTag = "bottom_sheet_lock_note"
                    )
                } else {
                    // Deleted Note ("हाल ही में डिलीट किए गए") Long Press Options:
                    // 1. ♻️ Restore
                    BottomSheetOptionRow(
                        icon = Icons.Default.RestoreFromTrash,
                        label = "♻️ " + stringResource(R.string.restore_option),
                        tint = PinGoldDark,
                        onClick = {
                            val noteToRestore = targetNote
                            longPressedNote = null
                            onRestoreNote(noteToRestore)
                        },
                        testTag = "bottom_sheet_restore_note"
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 2. 🗑️ Permanently Delete
                    BottomSheetOptionRow(
                        icon = Icons.Outlined.DeleteOutline,
                        label = "🗑️ " + stringResource(R.string.permanently_delete_option),
                        tint = Color(0xFFF87171),
                        onClick = {
                            val noteToDelete = targetNote
                            longPressedNote = null
                            notePendingPermanentDelete = noteToDelete
                        },
                        testTag = "bottom_sheet_permanently_delete_note"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // 4-Digit PIN Setup / Unlock Dialogs
    when (val dialog = pinDialogState) {
        is PinDialogState.SetPin -> {
            SetFourDigitPinDialog(
                state = dialog,
                onConfirm = { pin, confirmPin ->
                    onSetFourDigitPin(pin, confirmPin, dialog.noteToLock)
                },
                onDismiss = onDismissPinDialog
            )
        }
        is PinDialogState.UnlockNote -> {
            UnlockNotePinDialog(
                state = dialog,
                onUnlock = { pin ->
                    onUnlockPinSubmitted(pin, dialog.note, dialog.unlockAction)
                },
                onDismiss = onDismissPinDialog
            )
        }
        null -> Unit
    }

    // Permanently Delete Confirmation Dialog
    notePendingPermanentDelete?.let { noteToDelete ->
        AlertDialog(
            onDismissRequest = { notePendingPermanentDelete = null },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.9f),
            title = {
                Text(
                    text = stringResource(R.string.permanently_delete_option),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.permanent_delete_confirm_message),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPermanentDeleteNote(noteToDelete)
                        notePendingPermanentDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_permanent_delete_button")
                ) {
                    Text(stringResource(R.string.delete_permanently_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { notePendingPermanentDelete = null },
                    modifier = Modifier.testTag("cancel_permanent_delete_button")
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = Color.White
                    )
                }
            },
            modifier = Modifier.testTag("permanent_delete_confirm_dialog")
        )
    }
}

@Composable
private fun BottomSheetOptionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF283548),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White
            )
        }
    }
}

@Composable
private fun SetFourDigitPinDialog(
    state: PinDialogState.SetPin,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember(state.noteToLock.id) { mutableStateOf("") }
    var confirmPin by remember(state.noteToLock.id) { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PinGoldDark
                )
                Text(
                    text = stringResource(R.string.lock_note_action),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all { it.isDigit() }) {
                            pin = input
                            localError = null
                        }
                    },
                    label = { Text(stringResource(R.string.enter_4_digit_pin)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("set_pin_enter_input")
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all { it.isDigit() }) {
                            confirmPin = input
                            localError = null
                        }
                    },
                    label = { Text(stringResource(R.string.confirm_4_digit_pin)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("set_pin_confirm_input")
                )

                val errorToShow = localError ?: state.errorMessage
                if (errorToShow != null) {
                    Text(
                        text = errorToShow,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF87171),
                        modifier = Modifier.testTag("set_pin_error_text")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length != 4 || confirmPin.length != 4) {
                        localError = "कृपया केवल 4 अंकों का PIN दर्ज करें।"
                    } else if (pin != confirmPin) {
                        localError = "दोनों PIN समान नहीं हैं, कृपया दोबारा प्रयास करें।"
                    } else {
                        onConfirm(pin, confirmPin)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinGoldDark,
                    contentColor = Color(0xFF1E293B)
                ),
                modifier = Modifier.testTag("set_pin_confirm_button")
            ) {
                Text(
                    text = stringResource(R.string.lock_note_action),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("set_pin_cancel_button")
            ) {
                Text(stringResource(R.string.cancel), color = Color.White)
            }
        },
        modifier = Modifier.testTag("set_four_digit_pin_dialog")
    )
}

@Composable
private fun UnlockNotePinDialog(
    state: PinDialogState.UnlockNote,
    onUnlock: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pinInput by remember(state) { mutableStateOf("") }
    var localError by remember(state) { mutableStateOf<String?>(null) }
    val wrongPinErrorMsg = stringResource(R.string.wrong_pin_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PinGoldDark
                )
                Text(
                    text = stringResource(R.string.locked_note_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.enter_4_digit_pin),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all { it.isDigit() }) {
                            pinInput = input
                            localError = null
                        }
                    },
                    label = { Text(stringResource(R.string.enter_4_digit_pin)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unlock_pin_input")
                )

                val errorToShow = localError ?: state.errorMessage
                if (errorToShow != null) {
                    Text(
                        text = errorToShow,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF87171),
                        modifier = Modifier.testTag("unlock_pin_error_text")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinInput.length != 4) {
                        localError = wrongPinErrorMsg
                    } else {
                        onUnlock(pinInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinGoldDark,
                    contentColor = Color(0xFF1E293B)
                ),
                modifier = Modifier.testTag("unlock_pin_confirm_button")
            ) {
                Text(
                    text = "Unlock",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("unlock_pin_cancel_button")
            ) {
                Text(stringResource(R.string.cancel), color = Color.White)
            }
        },
        modifier = Modifier.testTag("unlock_note_dialog")
    )
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 4.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    isTrashItem: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onTogglePin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("note_card_${note.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = if (note.isPinned || note.isLocked) 1.5.dp else 1.dp,
            color = when {
                note.isLocked -> PinGoldDark.copy(alpha = 0.55f)
                note.isPinned -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (note.isLocked) {
                    // Hide full title and content for Locked Notes; show "🔒 Locked Note"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = PinGoldDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.locked_note_mask),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PinGoldDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = note.title.ifBlank { "Untitled Note" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (note.title.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                }

                if (!isTrashItem && !note.isLocked) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("pin_button_${note.id}")
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Unpin" else "Pin",
                            tint = if (note.isPinned) PinGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Show content preview ONLY if the note is NOT locked
            if (!note.isLocked && note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateFormatter.formatNoteDate(note.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                if (note.isPinned && !isTrashItem && !note.isLocked) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.pinned),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteGridCard(
    note: Note,
    isTrashItem: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onTogglePin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("note_card_${note.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = if (note.isPinned || note.isLocked) 1.5.dp else 1.dp,
            color = when {
                note.isLocked -> PinGoldDark.copy(alpha = 0.55f)
                note.isPinned -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (note.isLocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = PinGoldDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.locked_note_mask),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PinGoldDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = note.title.ifBlank { "Untitled Note" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (note.title.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                    )
                }

                if (!isTrashItem && !note.isLocked) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("pin_button_${note.id}")
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Unpin" else "Pin",
                            tint = if (note.isPinned) PinGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (!note.isLocked && note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateFormatter.formatNoteDate(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (note.isPinned && !isTrashItem && !note.isLocked) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.pinned),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNotesState(
    isSearching: Boolean = false,
    selectedSection: NoteSection = NoteSection.ALL,
    onCreateFirstNote: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("empty_notes_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isSearching -> Icons.Default.Search
                        selectedSection == NoteSection.LOCKED -> Icons.Default.Lock
                        selectedSection == NoteSection.TRASH -> Icons.Default.Delete
                        else -> Icons.Default.NoteAdd
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = when {
                    isSearching -> stringResource(R.string.no_search_results)
                    selectedSection == NoteSection.TRASH -> stringResource(R.string.recently_deleted_option)
                    selectedSection == NoteSection.LOCKED -> stringResource(R.string.locked_notes_option)
                    else -> stringResource(R.string.no_notes_title)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    isSearching -> "किसी अन्य शब्द से खोज कर देखें"
                    selectedSection == NoteSection.TRASH -> "कोई डिलीट किया गया नोट नहीं है"
                    selectedSection == NoteSection.LOCKED -> "कोई लॉक किया गया नोट नहीं है"
                    else -> stringResource(R.string.no_notes_desc)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isSearching && selectedSection == NoteSection.ALL) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onCreateFirstNote,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("create_first_note_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.create_first_note),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
