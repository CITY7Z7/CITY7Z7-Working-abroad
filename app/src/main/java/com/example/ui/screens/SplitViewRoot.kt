package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.database.CityDatabase
import com.example.data.database.entity.ArticleKind
import com.example.data.repository.CityRepository
import com.example.ui.navigation.Selection
import com.example.ui.navigation.ToolKind
import com.example.ui.viewmodel.DocumentHubViewModel
import com.example.ui.viewmodel.DocumentViewModel
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SplitViewRoot(
    mainViewModel: MainViewModel,
    documentViewModel: DocumentViewModel,
    documentHubViewModel: DocumentHubViewModel,
    repository: CityRepository,
    database: CityDatabase,
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSelection by mainViewModel.currentSelection.collectAsState()
    val countries by mainViewModel.countries.collectAsState()
    val selectedCountryId by mainViewModel.selectedCountryId.collectAsState()
    val rates by mainViewModel.currentCountryRates.collectAsState()

    val selectedCountry = countries.firstOrNull { it.id == selectedCountryId }

    // System BackHandler
    BackHandler(enabled = currentSelection != Selection.Home && currentSelection != Selection.NotesHub) {
        mainViewModel.navigateBack()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isDualPane = maxWidth >= 840.dp

        if (isDualPane) {
            // Dual-Pane Split-View (36% master pane, 340dp..440dp)
            val leftPaneWidth = (maxWidth * 0.36f).coerceIn(340.dp, 440.dp)

            Row(modifier = Modifier.fillMaxSize()) {
                LeftNavigationPane(
                    mainViewModel = mainViewModel,
                    modifier = Modifier.width(leftPaneWidth)
                )

                VerticalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("right_content_pane")
                ) {
                    RenderContentPane(
                        selection = currentSelection,
                        mainViewModel = mainViewModel,
                        documentViewModel = documentViewModel,
                        documentHubViewModel = documentHubViewModel,
                        repository = repository,
                        database = database,
                        currentTheme = currentTheme,
                        onThemeChange = onThemeChange,
                        selectedCountry = selectedCountry,
                        rates = rates,
                        onBack = null // In dual-pane, master list is always visible
                    )
                }
            }
        } else {
            // Single-Pane Responsive View: user sees NotesHub by default, can open NavigationMenu
            if (currentSelection == Selection.NavigationMenu) {
                LeftNavigationPane(
                    mainViewModel = mainViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                RenderContentPane(
                    selection = currentSelection,
                    mainViewModel = mainViewModel,
                    documentViewModel = documentViewModel,
                    documentHubViewModel = documentHubViewModel,
                    repository = repository,
                    database = database,
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange,
                    selectedCountry = selectedCountry,
                    rates = rates,
                    onBack = {
                        if (currentSelection == Selection.Home || currentSelection == Selection.NotesHub) {
                            mainViewModel.select(Selection.NavigationMenu)
                        } else {
                            mainViewModel.navigateBack()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RenderContentPane(
    selection: Selection,
    mainViewModel: MainViewModel,
    documentViewModel: DocumentViewModel,
    documentHubViewModel: DocumentHubViewModel,
    repository: CityRepository,
    database: CityDatabase,
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    selectedCountry: com.example.data.database.entity.CountryEntity?,
    rates: List<com.example.data.database.entity.RateEntity>,
    onBack: (() -> Unit)?
) {
    when (selection) {
        // Notes Hub as the primary home screen
        Selection.Home, Selection.NotesHub -> {
            NotesHubScreen(
                repository = repository,
                onNoteClick = { noteId -> mainViewModel.select(Selection.Note(noteId)) },
                onNewNoteClick = {
                    documentViewModel.initNewNote()
                    mainViewModel.select(Selection.NewNote)
                },
                onBack = onBack
            )
        }
        Selection.NavigationMenu -> {
            LeftNavigationPane(
                mainViewModel = mainViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
        is Selection.Country -> {
            ArticleDetailScreen(
                articleId = "${selection.id}.fact_card",
                country = selectedCountry,
                rates = rates,
                documentViewModel = documentViewModel,
                onBack = onBack,
                onCreateNoteFromArticle = { articleId ->
                    documentViewModel.initNewNote(articleId)
                    mainViewModel.select(Selection.NewNote)
                }
            )
        }
        is Selection.SectionArticles -> {
            SectionArticlesScreen(
                countryId = selection.countryId,
                sectionId = selection.sectionId,
                sectionTitle = selection.sectionTitle,
                repository = repository,
                onArticleClick = { articleId -> mainViewModel.select(Selection.Article(articleId)) },
                onBack = onBack
            )
        }
        is Selection.Article -> {
            ArticleDetailScreen(
                articleId = selection.id,
                country = selectedCountry,
                rates = rates,
                documentViewModel = documentViewModel,
                onBack = onBack,
                onCreateNoteFromArticle = { articleId ->
                    documentViewModel.initNewNote(articleId)
                    mainViewModel.select(Selection.NewNote)
                }
            )
        }
        is Selection.Checklist -> {
            ChecklistScreen(
                checklistId = selection.id,
                repository = repository,
                onBack = onBack
            )
        }
        Selection.NewNote -> {
            NoteEditorScreen(
                noteId = 0L,
                documentViewModel = documentViewModel,
                onBack = onBack,
                onNoteDeleted = { mainViewModel.navigateBack() }
            )
        }
        is Selection.Note -> {
            NoteEditorScreen(
                noteId = selection.id,
                documentViewModel = documentViewModel,
                onBack = onBack,
                onNoteDeleted = { mainViewModel.navigateBack() }
            )
        }
        is Selection.Tool -> {
            when (selection.kind) {
                ToolKind.CALCULATOR -> CalculatorScreen(repository = repository, onBack = onBack)
                ToolKind.PHRASES -> SimpleArticleKindScreen("Разговорник", ArticleKind.PHRASE_SET, repository, onBack)
                ToolKind.GLOSSARY -> SimpleArticleKindScreen("Словарь терминов", ArticleKind.GLOSSARY, repository, onBack)
                ToolKind.TEMPLATES -> SimpleArticleKindScreen("Шаблоны писем", ArticleKind.TEMPLATE, repository, onBack)
                ToolKind.SITUATIONS -> SimpleArticleKindScreen("Что делать, если...", ArticleKind.SITUATION, repository, onBack)
                ToolKind.CONTACTS -> ContactsScreen(repository = repository, onBack = onBack)
                ToolKind.SETTINGS -> SettingsScreen(
                    repository = repository,
                    database = database,
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange,
                    onBack = onBack
                )
            }
        }
        Selection.KnowledgeImport -> {
            KnowledgeImportScreen(
                repository = repository,
                onBack = onBack
            )
        }
        Selection.Sos -> {
            SosScreen(repository = repository, onBack = onBack)
        }
        is Selection.Search -> {
            SearchResultsScreen(
                query = selection.query,
                mainViewModel = mainViewModel,
                onArticleClick = { articleId -> mainViewModel.select(Selection.Article(articleId)) },
                onNoteClick = { noteId -> mainViewModel.select(Selection.Note(noteId)) },
                onBack = onBack
            )
        }
        Selection.Profile -> {
            ProfileScreen(repository = repository, onBack = onBack)
        }
        is Selection.Documents -> {
            DocumentHubScreen(
                viewModel = documentHubViewModel,
                onBack = onBack,
                onDocumentClick = { docId -> mainViewModel.select(Selection.Document(docId)) },
                onShowModeClick = { docId -> mainViewModel.select(Selection.ShowMode(docId)) }
            )
        }
        is Selection.Document -> {
            DocumentViewerScreen(
                documentId = selection.id,
                viewModel = documentHubViewModel,
                onBack = { mainViewModel.navigateBack() },
                onShowModeClick = { mainViewModel.select(Selection.ShowMode(selection.id)) }
            )
        }
        is Selection.ShowMode -> {
            DocumentShowModeScreen(
                documentId = selection.documentId,
                viewModel = documentHubViewModel,
                onExitShowMode = { mainViewModel.navigateBack() }
            )
        }
        Selection.QrCard -> {
            QrCardScreen(viewModel = documentHubViewModel, onBack = onBack)
        }
        Selection.ShareLog -> {
            ShareLogScreen(repository = repository, onBack = onBack)
        }
        is Selection.ShareBundle -> {
            DocumentHubScreen(
                viewModel = documentHubViewModel,
                onBack = onBack,
                onDocumentClick = { docId -> mainViewModel.select(Selection.Document(docId)) },
                onShowModeClick = { docId -> mainViewModel.select(Selection.ShowMode(docId)) }
            )
        }
    }
}
