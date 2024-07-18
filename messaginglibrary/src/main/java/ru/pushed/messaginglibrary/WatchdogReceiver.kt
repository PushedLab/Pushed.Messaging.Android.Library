package ru.pushed.messaginglibrary

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat

class WatchdogReceiver: BroadcastReceiver() {
    private val QUEUE_REQUEST_ID = 111
    private val ACTION_RESPAWN = "pushed.background_service.RESPAWN"
    companion object {
        lateinit var cls:Class<*>
    }
    fun enqueue(context: Context){
        enqueue(context,900000)

    }
    private fun enqueue(context:Context, millis:Int){
        cls=context::class.java
        val intent = Intent(context, WatchdogReceiver::class.java)
        intent.setAction(ACTION_RESPAWN)
        val manager = context.getSystemService(ALARM_SERVICE) as AlarmManager

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        val pIntent = PendingIntent.getBroadcast(context, QUEUE_REQUEST_ID, intent, flags)
        manager.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis() + millis, pIntent)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("WatchDog","Restart")
        if(intent?.action==ACTION_RESPAWN){
            val pref=context?.getSharedPreferences("Pushed", Context.MODE_PRIVATE)
            if(pref?.getBoolean("foreground",false)==true){
                ContextCompat.startForegroundService(context, Intent(context, cls))
            }
            else context?.startService(Intent(context, cls))
        }

    }


}