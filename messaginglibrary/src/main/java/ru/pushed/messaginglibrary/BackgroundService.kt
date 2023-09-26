package ru.pushed.messaginglibrary

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import org.json.JSONObject

open class BackgroundService : Service(){
    val ACTION_RESPAWN = "pushed.background_service.RESPAWN"
    private val tag="BackgroundService"
    private lateinit var pref: SharedPreferences
    private var foreground: Boolean = false
    private var token:String?=null
    private val watchdogReceiver=WatchdogReceiver()
    private var messageListener:MessageListener?=null
    val listeners= mutableMapOf<Int,IBackgroundService?>()
    private val binder: IBackgroundServiceBinder.Stub = object : IBackgroundServiceBinder.Stub() {
        override fun bind(id: Int, service: IBackgroundService?) {
            Log.d(tag,"Bind: $id")
            synchronized(listeners) {
                listeners.put(id,service)
            }
        }
        override fun unbind(id: Int) {
            Log.d(tag,"UnBind: $id")
            synchronized(listeners) {
                listeners.remove(id)
            }
        }

        override fun invoke(data: String?) {
            receiveData(JSONObject(data?:""))
        }

    }

    override fun onBind(intent: Intent?): IBinder? {
        val binderId=intent?.getIntExtra("binder_id",0)?:0
        Log.d(tag,"On bind: $binderId")
        return binder
    }


    override fun onUnbind(intent: Intent?): Boolean {
        val binderId=intent?.getIntExtra("binder_id",0)?:0
        Log.d(tag,"On unbind: $binderId")

        if(binderId!=0) synchronized(listeners) {
            listeners.remove(binderId)
        }
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag,"On Create")
        pref=getSharedPreferences("Pushed", Context.MODE_PRIVATE)
        foreground=pref.getBoolean("foreground",false)
        Log.d(tag,"Fore: $foreground")
        if(foreground) {
            val packageName=applicationContext.packageName
            val i = packageManager.getLaunchIntentForPackage(packageName)
            var flags= PendingIntent.FLAG_CANCEL_CURRENT
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)
                flags = flags or PendingIntent.FLAG_MUTABLE
            val pi= PendingIntent.getActivity(this,11,i,flags)
            val mBuilder= NotificationCompat.Builder(this,"pushed")
                .setAutoCancel(true)
                .setSmallIcon(pref.getInt("icon",R.mipmap.ic_bg_service_small))
                .setOngoing(false)
                .setContentTitle(pref.getString("title","Pushed"))
                .setContentText(pref.getString("body","The service is active"))
                .setCategory("")
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setContentIntent(pi)
            startForeground(101,mBuilder.build())

        }

    }


    override fun onDestroy() {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
            stopForeground(STOP_FOREGROUND_REMOVE)
        else stopForeground(foreground)
        messageListener?.disconnect()
        messageListener=null
        super.onDestroy()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag,"ON Start")
        watchdogReceiver.enqueue(this)
        if(messageListener!=null){
            Log.d(tag,"Service already started")
            messageListener?.disconnect()
            return START_STICKY
        }
        token=pref.getString("token",null)
        Log.d(tag,"Token: $token")
        if(token!=null){
            pref.edit().putString("token",token).apply()
            messageListener=MessageListener("wss://sub.pushed.ru/v1/$token",this){message->
                var sended=false
                synchronized(listeners){
                    if(listeners.size>0)
                        for(key in listeners.keys)
                            if(listeners[key]?.invoke(message.toString()) != false) sended=true
                }
                if(!sended) onBackgroundMessage(message)
            }
        }
        return START_STICKY
    }
    open fun onBackgroundMessage(message: JSONObject){
        Log.d(tag,"Background message: $message")
    }
    fun receiveData(data : JSONObject) {
        Log.d(tag,"Receive: $data")
        if(data["method"]=="restart") messageListener?.disconnect()
    }

}