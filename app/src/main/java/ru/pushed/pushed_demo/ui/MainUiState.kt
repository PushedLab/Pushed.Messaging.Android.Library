package ru.pushed.pushed_demo.ui

import ru.pushed.messaginglibrary.Status

data class UiState(
    val token: String?        = null,
    val status: Status        = Status.NOTACTIVE,
    val lastPushTitle: String = "",
    val lastPushBody: String  = ""
)
