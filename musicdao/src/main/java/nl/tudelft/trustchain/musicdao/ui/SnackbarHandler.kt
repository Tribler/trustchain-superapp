package nl.tudelft.trustchain.musicdao.ui

import androidx.compose.material.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object SnackbarHandler {
    var snackbarHostState: SnackbarHostState? = null
    var coroutineScope: CoroutineScope? = null

    fun displaySnackbar(text: String) {
        val snackbarHostState = snackbarHostState
        val coroutineScope = coroutineScope

        if (snackbarHostState != null && coroutineScope != null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message = text)
            }
        }
    }
}
