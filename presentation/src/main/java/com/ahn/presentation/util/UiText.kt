package com.ahn.presentation.util

import android.content.Context
import androidx.annotation.StringRes

sealed interface UiText {
    fun asString(context: Context): String

    data class StringResource(
        @param:StringRes val resId: Int,
        val args: List<Any> = emptyList(),
    ) : UiText {
        override fun asString(context: Context): String {
            return context.getString(resId, *args.toTypedArray())
        }
    }
}
