package ru.pushed.pushed_demo.data

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import ru.pushed.messaginglibrary.PushedEnvironment
import ru.pushed.messaginglibrary.PushedService
import ru.pushed.messaginglibrary.Status
import ru.pushed.pushed_demo.MyMessageReceiver

class PushedRepository(private val context: Context) {

    private val service = PushedService(
        context,
        MyMessageReceiver::class.java,
        environment = PushedEnvironment.PROD
    )

    private val _statusFlow = MutableSharedFlow<Status>(replay = 1)
    val statusFlow: SharedFlow<Status> = _statusFlow.asSharedFlow()

    private val _pushFlow = MutableSharedFlow<Pair<String, String>>()
    val pushFlow: SharedFlow<Pair<String, String>> = _pushFlow.asSharedFlow()

    val token: String? get() = service.pushedToken

    init {
        service.setStatusHandler { status -> _statusFlow.tryEmit(status) }
    }

    fun start(): String? = service.start { message ->
        try {
            val pn    = message.getJSONObject("pushedNotification")
            val title = pn.optString("Title", "")
            val body  = pn.optString("Body", "")
            PushedService.addLogEvent(context, "DEMO FG: $message")
            _pushFlow.tryEmit(title to body)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun unbind() = service.unbindService()

    fun getLogs(context: Context): String = PushedService.getLog(context)
}
