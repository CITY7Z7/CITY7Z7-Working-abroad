package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.entity.ArticleEntity
import com.example.data.database.entity.ChecklistEntity
import com.example.data.database.entity.ChecklistItemEntity
import com.example.data.database.entity.ChecklistProgressEntity
import com.example.data.database.entity.CountryEntity
import com.example.data.database.entity.DocumentEntity
import com.example.data.database.entity.NoteEntity
import com.example.data.database.entity.PersonProfileEntity
import com.example.data.database.entity.RateEntity
import com.example.data.database.entity.SectionEntity
import com.example.data.repository.CityRepository
import com.example.data.repository.SearchResults
import com.example.ui.navigation.Selection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: CityRepository
) : ViewModel() {

    // --- Navigation & Backstack (up to 20 items) ---
    private val backStack = mutableListOf<Selection>(Selection.Home)
    private val _currentSelection = MutableStateFlow<Selection>(Selection.Home)
    val currentSelection: StateFlow<Selection> = _currentSelection.asStateFlow()

    fun select(selection: Selection) {
        if (_currentSelection.value == selection) return
        if (backStack.size >= 20) {
            backStack.removeAt(0)
        }
        backStack.add(selection)
        _currentSelection.value = selection
    }

    fun navigateBack(): Boolean {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
            _currentSelection.value = backStack.last()
            return true
        }
        return false
    }

    // --- Active Country ---
    private val _selectedCountryId = MutableStateFlow("ie")
    val selectedCountryId: StateFlow<String> = _selectedCountryId.asStateFlow()

    fun selectCountry(countryId: String) {
        _selectedCountryId.value = countryId
    }

    val countries: StateFlow<List<CountryEntity>> = repository.allCountries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentCountrySections: StateFlow<List<SectionEntity>> = _selectedCountryId.flatMapLatest { id ->
        repository.getSectionsForCountry(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentCountryFactCard: StateFlow<ArticleEntity?> = _selectedCountryId.flatMapLatest { id ->
        repository.getFactCardForCountry(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentCountryRates: StateFlow<List<RateEntity>> = _selectedCountryId.flatMapLatest { id ->
        repository.getRatesForCountry(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Week 1 Progress ---
    private val week1Items: StateFlow<List<ChecklistItemEntity>> = repository.getItemsForChecklist("ie.w1")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val week1Progress: StateFlow<Pair<Int, Int>> = combine(
        week1Items,
        repository.allChecklistProgress
    ) { items, progressList ->
        val total = items.size
        val itemIds = items.map { it.id }.toSet()
        val completed = progressList.count { it.itemId in itemIds && it.isDone }
        Pair(completed, total)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0, 0))

    // --- Expiring Documents (Attention Needed) ---
    val expiringDocuments: StateFlow<List<DocumentEntity>> = repository.getExpiringDocuments(90)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Profile Summary ---
    val profile: StateFlow<PersonProfileEntity?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Notes for Sidebar (Recent 5) ---
    val sidebarNotes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Global Search ---
    private var screenBeforeSearch: Selection? = null
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow(SearchResults())
    val searchResults: StateFlow<SearchResults> = _searchResults.asStateFlow()

    fun updateSearchQuery(query: String) {
        if (_searchQuery.value.isBlank() && query.isNotBlank() && _currentSelection.value !is Selection.Search) {
            screenBeforeSearch = _currentSelection.value
        }
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = SearchResults()
        } else {
            select(Selection.Search(query))
            viewModelScope.launch {
                _searchResults.value = repository.search(query)
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = SearchResults()
        val target = screenBeforeSearch ?: Selection.NotesHub
        screenBeforeSearch = null
        select(target)
    }

    // --- Country Management ---
    fun addCountry(
        nameRu: String,
        flagEmoji: String,
        language: String,
        currencyCode: String,
        currencySymbol: String,
        onDone: (CountryEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val created = repository.addCountry(nameRu, flagEmoji, language, currencyCode, currencySymbol)
            _selectedCountryId.value = created.id
            select(Selection.Country(created.id))
            onDone(created)
        }
    }

    fun deleteCountry(countryId: String) {
        viewModelScope.launch {
            repository.deleteCountry(countryId)
            if (_selectedCountryId.value == countryId) {
                _selectedCountryId.value = "ie"
            }
            select(Selection.NotesHub)
        }
    }

    class Factory(private val repository: CityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
