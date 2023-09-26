package ru.pushed.pushedexample

import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONObject
import ru.pushed.messaginglibrary.BackgroundService

class MyBackgroundService:BackgroundService() {
    override fun onBackgroundMessage(message: JSONObject) {
        Log.d("Mybackground","MyBackground message: $message")
        val builder = NotificationCompat.Builder(this, "messages").apply {
            setSmallIcon(ru.pushed.messaginglibrary.R.mipmap.ic_bg_service_small)
            setContentTitle(message["title"].toString())
            setContentText(message["body"].toString())
            setPriority(NotificationCompat.PRIORITY_DEFAULT)
        }
        with(NotificationManagerCompat.from(this)) {
            notify(111, builder.build())
        }

    }
}