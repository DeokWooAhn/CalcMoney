package com.ahn.presentation.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun CustomSnackbarHost(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) { data ->
        CustomSnackbar(snackbarData = data)
    }
}

@Composable
fun CustomSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    containColor: Color = Color.DarkGray,
    contentColor: Color = Color.White,
    cornerRadius: Int = 20,
) {
    Snackbar(
        snackbarData = snackbarData,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius.dp),
        containerColor = containColor,
        contentColor = contentColor,
    )
}