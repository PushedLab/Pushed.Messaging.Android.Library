package ru.pushed.pushed_demo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import ru.pushed.pushed_demo.data.PushedRepository

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PushedRepository(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        repo.statusFlow
            .onEach { status ->
                _state.update { it.copy(status = status, token = repo.token ?: it.token) }
            }
            .launchIn(viewModelScope)

        repo.pushFlow
            .onEach { (title, body) ->
                _state.update { it.copy(lastPushTitle = title, lastPushBody = body) }
            }
            .launchIn(viewModelScope)
    }

    fun start() {
        val token = repo.start()
        if (!token.isNullOrEmpty()) _state.update { it.copy(token = token) }
    }

    fun unbind() = repo.unbind()

    fun getLogs(): String = repo.getLogs(getApplication())
}
