package ru.pushed.messaginglibrary

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject
import java.nio.file.WatchService

class FcmService : FirebaseMessagingService(){
    private val tag="FcmService"

    override fun onNewToken(token: String) {
        Log.d(tag, "Fcm Refreshed token: $token")
        PushedService.addLogEvent(this, "FCM change token: $token")

        // Сохраняем новый токен
        val secret = PushedService.getSecure(this)
        val oldFcmToken = secret.getString("fcmtoken", null)
        val oldToken = secret.getString("token", null)
        PushedService.addLogEvent(this, "FCM $oldFcmToken,$oldToken")

        if (token != oldFcmToken && oldToken!=null && oldFcmToken != null) {
            PushedService.refreshToken(this, oldPushedToken = oldToken, fcmToken = token)
        }
    }


    override fun onMessageReceived(message: RemoteMessage) {
        val pref=getSharedPreferences("Pushed", Context.MODE_PRIVATE)
        PushedService.addLogEvent(this, "Fcm Message: ${message.data}")
        val pushedMessage= JSONObject()
        val fcmData=message.data
        val traceId=fcmData["mfTraceId"]
        val messageId=fcmData["messageId"]
        val notification=fcmData["pushedNotification"]
        try {
            pushedMessage.put("data",JSONObject(fcmData["data"].toString()))
        }
        catch(e: Exception) {
            pushedMessage.put("data",fcmData["data"]?:"")
            Log.d(tag,"Data is String")
        }
        if(messageId!=null)
            pushedMessage.put("messageId",messageId)
        if(traceId!=null)
            pushedMessage.put("mfTraceId",traceId)
        if(notification!=null)
            try{
                pushedMessage.put("pushedNotification",JSONObject(notification.toString()))
            } catch (_: Exception){}

        PushedService.addLogEvent(this, "Fcm PushedMessage: $pushedMessage")
        if(messageId!=null && PushedService.checkLastMessages(this,messageId)){
            PushedService.confirmDelivered(this,messageId,"Fcm",traceId?:"")
            if(PushedService.isApplicationForeground(this)){
                MessageLiveData.getInstance()?.postRemoteMessage(pushedMessage)
            }
            else{
                val listenerClassName = pref.getString("listenerclass",null)
                if(notification!=null) {
                    try {
                        PushedService.showNotification(this, pushedMessage)
                    }
                    catch (e:Exception){
                        PushedService.addLogEvent(this,"Notification error: ${e.message}")
                    }
                }
                WatchdogReceiver().enqueue(this,5000)
                if(listenerClassName!=null){
                    val intent = Intent(applicationContext, Class.forName(listenerClassName))
                    intent.action = "ru.pushed.action.MESSAGE"
                    intent.putExtra("message",pushedMessage.toString())
                    sendBroadcast(intent)
                }
            }
        }


        super.onMessageReceived(message)
    }

}
