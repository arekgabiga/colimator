package com.colimator.app.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

open class BaseViewModel {
    // using IO/Main dispatchers via standard library if available, but for now Dispatchers.Main 
    // requires Swing or JavaFX dispatcher on Desktop.
    protected val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    open fun onCleared() {
        viewModelScope.cancel()
    }
}
