package ru.pushed.messaginglibrary


import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.util.Log
import org.json.JSONObject

open class BackgroundService : Service(){
    private val tag="BackgroundService"
    private lateinit var pref: SharedPreferences
    private lateinit var secretPref: SharedPreferences

    private var token:String?=null
    private val watchdogReceiver=WatchdogReceiver()
    private var messageListener:MessageListener?=null
    private var status:Status=Status.NOTACTIVE
    val listeners= mutableMapOf<Int,IBackgroundService?>()
    private val binder: IBackgroundServiceBinder.Stub = object : IBackgroundServiceBinder.Stub() {
        override fun bind(id: Int, service: IBackgroundService?) {
            Log.d(tag,"Bind: $id")
            synchronized(listeners) {
                listeners[id] = service
                service?.invoke("{\"ServiceStatus\":${status.name}}")
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
    companion object {
        var active=false
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
        secretPref=PushedService.getSecure(this)

    }


    override fun onDestroy() {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
            stopForeground(STOP_FOREGROUND_REMOVE)
        messageListener?.deactivate()

        messageListener=null
        active=false
        watchdogReceiver.enqueue(this,5000)
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        messageListener?.deactivate()
        messageListener=null
        active=false
        watchdogReceiver.enqueue(this,5000)
        super.onTaskRemoved(rootIntent)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag,"ON Start")
        PushedService.addLogEvent(this,"Start service")
        val restarted = pref.getBoolean("restarted", false)
        active=true
        watchdogReceiver.enqueue(this,if(restarted)900000 else 15000)
        if(messageListener!=null){
            Log.d(tag,"Service already started")
            PushedService.addLogEvent(this,"Service already started")
            messageListener?.disconnect()
            return START_STICKY
        }
        token=secretPref.getString("token",null)
        Log.d(tag,"Token: $token")
        if(token!=null){
            messageListener=MessageListener("wss://sub.pushed.ru/v2/open-websocket/$token",this){message->
                if(message.has("ServiceStatus")){
                    if(status!= Status.valueOf(message.getString("ServiceStatus"))){
                        status=Status.valueOf(message.getString("ServiceStatus"))
                        synchronized(listeners){
                            if(listeners.isNotEmpty())
                                for(key in listeners.keys)
                                    listeners[key]?.invoke("{\"ServiceStatus\":${status.name}}")
                        }

                    }
                }
                else if(message["messageId"]!=pref.getString("lastmessage","")){
                    pref.edit().putString("lastmessage",message["messageId"].toString()).apply()
                    var sent=false
                    synchronized(listeners){
                        if(listeners.isNotEmpty())
                            for(key in listeners.keys)
                                if(listeners[key]?.invoke(message.toString()) != false) sent=true
                    }
                    if(!sent) onBackgroundMessage(message)
                }
            }
        }
        return START_STICKY
    }
    open fun onBackgroundMessage(message: JSONObject){
        Log.d(tag,"Background message: $message")
        try{
            val notification=JSONObject(message.toString())
            PushedService.showNotification(this,message)
        }
        catch (e:Exception){
            PushedService.addLogEvent(this,"Notification error: ${e.message}")
            //e.printStackTrace()
        }
        val listenerClassName= pref.getString("listenerclass",null) ?: return
        val intent = Intent(applicationContext, Class.forName(listenerClassName))
        intent.action = "ru.pushed.action.MESSAGE"
        intent.putExtra("message",message.toString())
        sendBroadcast(intent)
    }

    fun receiveData(data : JSONObject) {
        Log.d(tag,"Receive: $data")
        if(data["method"]=="restart") messageListener?.disconnect(forceDisconnect = true)
    }

}
