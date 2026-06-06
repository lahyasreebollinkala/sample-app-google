package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.AppDatabase
import com.example.db.DecisionEntity
import com.example.db.DecisionRepository
import com.example.api.GeminiApiClient
import com.example.api.DecisionAnalysis
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DecisionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DecisionRepository
    val decisions: StateFlow<List<DecisionEntity>>
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedDecision = MutableStateFlow<DecisionEntity?>(null)
    val selectedDecision: StateFlow<DecisionEntity?> = _selectedDecision.asStateFlow()

    // Screen State: "HOME" (list and input form), "DETAIL" (the active decision tabs)
    private val _currentScreen = MutableStateFlow("HOME")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = DecisionRepository(db.decisionDao())
        
        // Filter decisions reactively based on query and favorites toggles!
        decisions = combine(
            repository.allDecisions,
            _searchQuery,
            _showFavoritesOnly
        ) { rawList, query, favsOnly ->
            var filtered = rawList
            if (favsOnly) {
                filtered = filtered.filter { it.isFavorite }
            }
            if (query.isNotBlank()) {
                filtered = filtered.filter { 
                    it.question.contains(query, ignoreCase = true) || 
                    it.context.contains(query, ignoreCase = true) 
                }
            }
            filtered
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoritesFilter() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun clearError() {
        _error.value = null
    }

    fun selectDecision(decision: DecisionEntity) {
        _selectedDecision.value = decision
        _currentScreen.value = "DETAIL"
    }

    fun goBackToHome() {
        _selectedDecision.value = null
        _currentScreen.value = "HOME"
    }

    fun deleteDecision(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
            if (_selectedDecision.value?.id == id) {
                goBackToHome()
            }
        }
    }

    fun toggleFavorite(decision: DecisionEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(decision.id, decision.isFavorite)
            // Update the currently viewed decision state if it's the one toggled
            _selectedDecision.value?.let { current ->
                if (current.id == decision.id) {
                    _selectedDecision.value = current.copy(isFavorite = !decision.isFavorite)
                }
            }
        }
    }

    fun generateDecisionAnalysis(
        question: String,
        context: String,
        analysisType: String
    ) {
        if (question.isBlank()) {
            _error.value = "Please enter a decision question first!"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val rawJson = GeminiApiClient.analyzeDecision(question, context, analysisType)
                if (rawJson != null) {
                    // Try parsing to verify it's accurate JSON output
                    val parsed = GeminiApiClient.parseAnalysis(rawJson)
                    if (parsed != null) {
                        val newEntity = DecisionEntity(
                            question = question,
                            context = context,
                            analysisType = analysisType,
                            jsonResult = rawJson
                        )
                        val insertedId = repository.insert(newEntity)
                        val savedEntity = repository.getById(insertedId.toInt())
                        if (savedEntity != null) {
                            _selectedDecision.value = savedEntity
                            _currentScreen.value = "DETAIL"
                        } else {
                            // Backup in case lookup fails
                            _selectedDecision.value = newEntity.copy(id = insertedId.toInt())
                            _currentScreen.value = "DETAIL"
                        }
                    } else {
                        _error.value = "We generated the analysis, but failed to parse the resulting structure. Please try again!"
                    }
                } else {
                    _error.value = "Failed to connect to the AI model. Please ensure you configured GEMINI_API_KEY in the Secrets panel."
                }
            } catch (e: Exception) {
                _error.value = "An unexpected error occurred: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
