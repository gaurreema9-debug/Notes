package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AboutDialog
import com.example.ui.AdMobTestAdHelper
import com.example.ui.AdMobTestInterstitialOverlay
import com.example.ui.EditNoteScreen
import com.example.ui.HomeScreen
import com.example.ui.NotesViewModel
import com.example.ui.Screen
import com.example.ui.theme.NotesTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NotesViewModel by viewModels {
        NotesViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AdMobTestAdHelper.initializeSdk(this)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
            val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
            val selectedSection by viewModel.selectedSection.collectAsStateWithLifecycle()
            val pinDialogState by viewModel.pinDialogState.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val notes by viewModel.notes.collectAsStateWithLifecycle()
            val editorState by viewModel.editorState.collectAsStateWithLifecycle()
            val showDeleteDialog by viewModel.showDeleteDialog.collectAsStateWithLifecycle()
            val showAboutDialog by viewModel.showAboutDialog.collectAsStateWithLifecycle()
            val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(statusMessage) {
                statusMessage?.let {
                    snackbarHostState.showSnackbar(
                        message = it,
                        duration = SnackbarDuration.Short
                    )
                    viewModel.clearStatusMessage()
                }
            }

            NotesTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = currentScreen,
                        label = "screen_crossfade"
                    ) { screen ->
                        when (screen) {
                            is Screen.Home -> {
                                HomeScreen(
                                    notes = notes,
                                    searchQuery = searchQuery,
                                    viewMode = viewMode,
                                    selectedSection = selectedSection,
                                    pinDialogState = pinDialogState,
                                    onSearchQueryChange = viewModel::onSearchQueryChange,
                                    onClearSearch = viewModel::clearSearch,
                                    onSelectSection = viewModel::selectSection,
                                    onLockNoteRequested = viewModel::onLockNoteRequested,
                                    onSetFourDigitPin = viewModel::onSetFourDigitPin,
                                    onUnlockPinSubmitted = viewModel::onUnlockPinSubmitted,
                                    onDismissPinDialog = viewModel::dismissPinDialog,
                                    onMoveNoteToTrash = viewModel::moveNoteToTrash,
                                    onRestoreNote = viewModel::restoreNote,
                                    onPermanentDeleteNote = viewModel::permanentlyDeleteNote,
                                    onViewModeChange = viewModel::setViewMode,
                                    onNoteClick = viewModel::openNote,
                                    onTogglePin = viewModel::togglePin,
                                    onAddNoteClick = viewModel::openNewNote,
                                    onOpenAbout = { viewModel.setShowAboutDialog(true) },
                                    snackbarHostState = snackbarHostState
                                )
                            }
                            is Screen.Editor -> {
                                EditNoteScreen(
                                    state = editorState,
                                    showDeleteDialog = showDeleteDialog,
                                    onTitleChange = viewModel::updateEditorTitle,
                                    onContentChange = viewModel::updateEditorContent,
                                    onContentValueChange = viewModel::updateEditorContentValue,
                                    onUndo = viewModel::undoEditor,
                                    onRedo = viewModel::redoEditor,
                                    onToggleInlineStyle = viewModel::toggleInlineStyle,
                                    onToggleBulletList = viewModel::toggleBulletList,
                                    onToggleNumberedList = viewModel::toggleNumberedList,
                                    onCycleTextAlign = viewModel::cycleTextAlignment,
                                    onSetTextAlign = viewModel::setTextAlignment,
                                    onSelectAll = viewModel::selectAllContent,
                                    onCutContent = viewModel::cutSelectedContent,
                                    onCopyContent = viewModel::copySelectedContent,
                                    onPasteContent = viewModel::pasteIntoContent,
                                    onTogglePin = viewModel::toggleEditorPin,
                                    onSaveNote = {
                                        val saved = viewModel.saveNote(showFeedback = true)
                                        if (saved) {
                                            AdMobTestAdHelper.showInterstitialAdIfAvailable(this@MainActivity)
                                        }
                                    },
                                    onDeleteRequest = { viewModel.setShowDeleteDialog(true) },
                                    onConfirmDelete = viewModel::deleteCurrentNote,
                                    onDismissDelete = { viewModel.setShowDeleteDialog(false) },
                                    onBack = {
                                        viewModel.onBackFromEditor()
                                        AdMobTestAdHelper.showInterstitialAdIfAvailable(this@MainActivity)
                                    }
                                )
                            }
                        }
                    }

                    if (showAboutDialog) {
                        AboutDialog(
                            currentTheme = themeMode,
                            onThemeSelected = viewModel::setThemeMode,
                            onDismiss = { viewModel.setShowAboutDialog(false) }
                        )
                    }

                    AdMobTestInterstitialOverlay()
                }
            }
        }
    }
}
