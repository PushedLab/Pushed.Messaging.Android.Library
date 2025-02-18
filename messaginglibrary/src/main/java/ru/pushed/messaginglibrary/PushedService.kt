package ru.pushed.messaginglibrary

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import android.os.StrictMode
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.lifecycle.Observer
import com.google.firebase.messaging.FirebaseMessaging
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.api.HuaweiApiAvailability
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import ru.rustore.sdk.pushclient.RuStorePushClient
import java.io.IOException
import java.util.Calendar


class PushedService(private val context : Context, messageReceiverClass: Class<*>?) {
    private val tag="Pushed Service"
    private val pref: SharedPreferences =context.getSharedPreferences("Pushed",Context.MODE_PRIVATE)
    private var  serviceBinder: IBackgroundServiceBinder?=null
    private var messageHandler: ((JSONObject) -> Boolean)?=null
    var mShouldUnbind=false
    private var fcmToken:String?=null
    private var hpkToken:String?=null
    private var ruStoreToken:String?=null

    public var pushedToken:String?=null


    private val messageLiveData=MessageLiveData.getInstance()
    private var messageObserver:Observer<JSONObject>?=null
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
    companion object{
        fun addLogEvent(context: Context? ,event:String){
            if(BuildConfig.DEBUG) {
                val sp = context?.getSharedPreferences("Pushed", Context.MODE_PRIVATE)
                if (sp != null) {
                    val date: String = Calendar.getInstance().time.toString()
                    val fEvent = "$date: $event\n"
                    Log.d("PushedLogger", fEvent)
                    val log = sp.getString("log", "")
                    sp.edit().putString("log", log + fEvent).apply()
                }
            }
        }
        fun getLog(context: Context? ):String{
            val sp =context?.getSharedPreferences("Pushed",Context.MODE_PRIVATE)
            if(sp!=null) return sp.getString("log","")?:""
            return ""
        }
        fun confirmDelivered(context: Context? ,messageId :String,transport:String,traceId:String){
            val sp =context?.getSharedPreferences("Pushed",Context.MODE_PRIVATE)
            val token: String = sp?.getString("token",null) ?: return
            val basicAuth = "Basic ${Base64.encodeToString("$token:$messageId".toByteArray(),Base64.NO_WRAP)}"
            val body="".toRequestBody("application/json; charset=utf-8".toMediaType())
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://pub.pushed.ru/v2/confirm?transportKind=$transport")
                .addHeader("Authorization", basicAuth)
                .addHeader("mf-trace-id",traceId)
                .post(body)
                .build()
            client.newCall(request).enqueue(object :Callback{
                override fun onFailure(call: Call, e: IOException) {
                    addLogEvent(context,"Confirm failure: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    if(response.isSuccessful){
                        val responseBody= response.body?.string()
                        addLogEvent(context,"Confirm response: $responseBody")
                    }
                    else
                        addLogEvent(context,"Confirm code: ${response.code}")
                }

            })
        }
        fun isApplicationForeground(context: Context): Boolean {
            val keyguardManager =
                context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager?
            if (keyguardManager != null && keyguardManager.isKeyguardLocked) {
                return false
            }
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
                    ?: return false
            val appProcesses = activityManager.runningAppProcesses ?: return false
            val packageName = context.packageName
            for (appProcess in appProcesses) {
                if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName == packageName) {
                    return true
                }
            }
            return false
        }

    }
    init{
        pushedToken=pref.getString("token",null)
        fcmToken=pref.getString("fcmtoken",null)
        ruStoreToken=pref.getString("rustoretoken",null)
        hpkToken=pref.getString("hpktoken",null)
        pushedToken=getNewToken()
        addLogEvent(context,"Pushed Token: $pushedToken")
        if(pushedToken!=null){
            pref.edit().putString("listenerclass",messageReceiverClass?.name).apply()
            val firstRun=pref.getBoolean("firstrun", true)
            val pm=context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if(!pm.isIgnoringBatteryOptimizations(context.packageName) && firstRun) {
                val intent= Intent()
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:${context.packageName}")
                context.startActivity(intent)
            }
            pref.edit().putBoolean("firstrun",false).apply()
            messageObserver = Observer<JSONObject> { message: JSONObject? ->
                messageHandler?.invoke(message!!)
            }
            messageLiveData?.observeForever(messageObserver!!)
            //Fcm

            try{
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        addLogEvent(context, "Fcm Token: ${task.result}")
                        if (fcmToken != task.result) {
                            fcmToken = task.result
                            getNewToken()
                        }
                    }
                    else{
                        addLogEvent(context, "Cant init Fcm")
                    }
                }
            }
            catch (e:Exception){
                addLogEvent(context, "Fcm init Error: ${e.message}")
            }

            //RuStore
            try {
                RuStorePushClient.getToken().addOnSuccessListener { token: String ->
                    addLogEvent(context, "RuStore Token: $token")
                    if(token!=ruStoreToken){
                        ruStoreToken = token
                        getNewToken()
                    }
                }
            } catch (e: Exception) {
                addLogEvent(context, "RuStore init Error: ${e.message}")
            }

            //Hpk
            try{
                val hmsResult=HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context)
                addLogEvent(context, "HMS Core: $hmsResult")
                if(hmsResult==0) {
                    object : Thread(){
                        override fun run() {
                            try{
                                val token = HmsInstanceId.getInstance(context).getToken("","HCM")
                                addLogEvent(context, "Hpk Token: $token")
                                if(token!=hpkToken) {
                                    hpkToken=token
                                    getNewToken()
                                }
                            } catch(e: Exception){
                                addLogEvent(context, "Hpk init Error: ${e.message}")
                            }
                        }
                    }.start()
                }
            } catch (e:Exception){
                addLogEvent(context, "HMS Core init Error: ${e.message}")
            }
        }
    }
    fun unbindService(){
        if(mShouldUnbind){
            mShouldUnbind=false
            context.unbindService(serviceConnection)
            serviceBinder?.unbind(binderId)
        }
    }
    fun reconnect(){
        serviceBinder?.invoke("{\"method\":\"restart\"}")
    }
    fun receiveData(data:JSONObject): Boolean{

        Log.d(tag,"Message Service($binderId): $data")
        return messageHandler?.invoke(data)?:false

    }
    fun start(onMessage:(JSONObject)->Boolean):String? {
        if(pushedToken==null) return null
        messageHandler=onMessage
        val serviceIntent=Intent(context,BackgroundService::class.java)
        serviceIntent.putExtra("binder_id", binderId)
        context.startService(serviceIntent)
        mShouldUnbind=context.bindService(serviceIntent,serviceConnection,Context.BIND_AUTO_CREATE)
        PushedJobService.startMyJob(context,3000,5000,1)
        pref.edit().putBoolean("restarted",false).apply()
        return pushedToken
    }
    private fun getNewToken():String?{
        val policy= StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        val deviceSettings= JSONArray()// mutableListOf<JSONObject>()
        if(fcmToken?.isNotEmpty()==true) deviceSettings.put(JSONObject().put("deviceToken",fcmToken).put("transportKind","Fcm"))
        if(hpkToken?.isNotEmpty()==true) deviceSettings.put(JSONObject().put("deviceToken",hpkToken).put("transportKind","Hpk"))
        if(ruStoreToken?.isNotEmpty() == true) deviceSettings.put(JSONObject().put("deviceToken",ruStoreToken).put("transportKind","RuStore"))
        val content=JSONObject("{\"clientToken\": \"${pushedToken?:""}\"}")
        if(deviceSettings.length()>0) content.put("deviceSettings",deviceSettings)
        val body= content.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        addLogEvent(context,"Content: $content")
        var result:String?=null
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://sub.pushed.ru/v2/tokens")
            .post(body)
            .build()
        try {
            val response=client.newCall(request).execute()
            if(response.isSuccessful)
            {

                val responseBody= response.body?.string()
                addLogEvent(context,"Get Token response: $responseBody")
                result = try{
                    val model=JSONObject(responseBody!!)["model"] as JSONObject
                    addLogEvent(context,"model: $model")
                    model["clientToken"] as String?
                } catch (e:Exception){
                    addLogEvent(context,"Convert ERR: ${e.message}")
                    null
                }
            }

        }
        catch (e: IOException){
            addLogEvent(context,"Get Token Err: ${e.message}")
        }
        if(result!=null && result!=""){
            pref.edit().putString("token",result).apply()
            if(fcmToken!=null) pref.edit().putString("fcmtoken",fcmToken).apply()
            if(hpkToken!=null) pref.edit().putString("hpktoken",hpkToken).apply()
            if(ruStoreToken!=null) pref.edit().putString("rustoretoken",ruStoreToken).apply()
            pushedToken=result
        }
        else result=pushedToken
        return result
    }

}