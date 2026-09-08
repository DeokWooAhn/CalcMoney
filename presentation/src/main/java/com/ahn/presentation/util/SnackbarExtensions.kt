package com.ahn.presentation.util

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

suspend fun SnackbarHostState.showSnackbarImmediately(
    message: String,
    actionLabel: String? = null,
    withDismissAction: Boolean = false,
): SnackbarResult {
    currentSnackbarData?.dismiss()
    return showSnackbar(
        message = message,
        actionLabel = actionLabel,
        withDismissAction = withDismissAction,
    )
}

@Composable
fun rememberShowSnackbar(hostState: SnackbarHostState): (UiText) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    return remember<(UiText) -> Unit>(hostState, context, scope) {
        { message ->
            scope.launch { hostState.showSnackbarImmediately(message.asString(context)) }
        }
    }
}
