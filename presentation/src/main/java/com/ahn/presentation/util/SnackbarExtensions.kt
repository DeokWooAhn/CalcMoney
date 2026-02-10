package com.ahn.presentation.util

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

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