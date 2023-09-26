package ru.pushed.messaginglibrary

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.StrictMode
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class PushedService(private val context : Context, private val title:String, private val body:String, private val icon:Int, private val backgroundServiceClass: Class<*>) {
    private val tag="Pushed Service"
    private val pref: SharedPreferences =context.getSharedPreferences("Pushed",Context.MODE_PRIVATE)
    private var  serviceBinder: IBackgroundServiceBinder?=null
    private var messageHandler: ((JSONObject) -> Boolean)?=null
    var mShouldUnbind=false
    private val binderId= (System.currentTimeMillis()/1000).toInt()
    private  val serviceConnection: ServiceConnection =object: ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceBinder=IBackgroundServiceBinder.Stub.asInterface(service)
            try{
                val listener =object : IBackgroundService.Stub(){
                    override fun invoke(data: String?): Boolean {
                        return receiveData(JSONObject(data?:"{}"))
                    }

                    override fun stop() {
                        unbindService()
                    }

                }
                serviceBinder?.bind(binderId,listener)
            }
            catch (e:Exception){
                Log.e(tag,e.message.toString())
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(tag,"on Disconnect")
            try{
                mShouldUnbind=false
                serviceBinder?.unbind(binderId)
                serviceBinder=null
            }
            catch (e:Exception){
                Log.e(tag,e.message.toString())
            }
        }

    }
    fun unbindService(){
        mShouldUnbind=false

        context.unbindService(serviceConnection)
        serviceBinder?.unbind(binderId)
    }
    fun reconnect(){
        serviceBinder?.invoke("{\"method\":\"restart\"}")
    }
    fun receiveData(data:JSONObject): Boolean{

        Log.d(tag,"Message Service($binderId): $data")
        return messageHandler?.invoke(data)?:false

    }
    fun start(onMessage:(JSONObject)->Boolean):String? {
        var token=pref.getString("token",null)
        if(token==null) token=getNewToken()
        if(token==null) return null
        pref.edit().putString("title",title).apply()
        pref.edit().putString("body",body).apply()
        pref.edit().putInt("icon",icon).apply()
        messageHandler=onMessage
        val firstRun=pref.getBoolean("firstrun", true)
        val packageName = context.packageName
        val pm=context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            if(!pm.isIgnoringBatteryOptimizations(packageName) && firstRun) {
                val intent= Intent()
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                context.startActivity(intent)

            }
        }
        var foreground=false
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            foreground=true
            val notificationChannel= NotificationChannel("pushed","Pushed", NotificationManager.IMPORTANCE_NONE)
            val notificationManager=context.getSystemService(NotificationManager::class.java)
            notificationChannel.setShowBadge(false)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val serviceIntent=Intent(context,backgroundServiceClass)
        serviceIntent.putExtra("binder_id", binderId)
        if(foreground) ContextCompat.startForegroundService(context,serviceIntent)
        else context.startService(serviceIntent)
        mShouldUnbind=context.bindService(serviceIntent,serviceConnection,Context.BIND_AUTO_CREATE)
        pref.edit().putBoolean("foreground",foreground).apply()
        pref.edit().putBoolean("firstrun",false).apply()
        return token
    }
    private fun getNewToken():String?{
        val policy= StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        var result:String?=null
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://sub.pushed.ru/tokens")
            .build()
        try {
            val response=client.newCall(request).execute()
            if(response.isSuccessful)
            {
                result= try{ JSONObject(response.body()?.string()?:"{}")["token"].toString()}
                catch (e:Exception) {null}
            }

        }
        catch (e: IOException){
            Log.e("App","Get Token Err")
        }
        if(result!=null) pref.edit().putString("token",result).apply()
        return result
    }

}