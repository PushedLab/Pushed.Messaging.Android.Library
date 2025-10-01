package ru.pushed.pushed_demo

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONObject
import ru.pushed.messaginglibrary.MessageReceiver
import ru.pushed.messaginglibrary.PushedService
import java.lang.Exception
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat


class MyMessageReceiver : MessageReceiver(){
    override fun onBackgroundMessage(context: Context?, message: JSONObject) {
        PushedService.addLogEvent(context ,"DEMO BG: $message")
        try {
            val data = message["data"] as JSONObject
            val builder = NotificationCompat.Builder(context!!, "messages").apply {
                setSmallIcon(R.mipmap.ic_launcher_round)
                setContentTitle(data["title"].toString())
                setContentText(data["body"].toString())
                priority = NotificationCompat.PRIORITY_MAX
            }
            with(NotificationManagerCompat.from(context)) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(111, builder.build())
                } else {
                    PushedService.addLogEvent(context, "Notification not shown: POST_NOTIFICATIONS permission missing.")
                }
            }
        }
        catch (e:Exception) {
            PushedService.addLogEvent(context ,"Notify err: ${e.message}")
        }
    }
}