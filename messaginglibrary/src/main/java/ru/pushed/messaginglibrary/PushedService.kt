package ru.pushed.messaginglibrary

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.StrictMode
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.concurrent.CountDownLatch

enum class Status(val value: Int){
    ACTIVE(0),OFFLINE(1),NOTACTIVE(2)
}


class PushedService(private val context : Context, messageReceiverClass: Class<*>?, channel:String?="messages",enableLogger:Boolean=true) {
    private val tag="Pushed Service"
    private val pref: SharedPreferences =context.getSharedPreferences("Pushed",Context.MODE_PRIVATE)
    private val secretPref: SharedPreferences = getSecure(context)
    private var serviceBinder: IBackgroundServiceBinder?=null
    private var messageHandler: ((JSONObject) -> Boolean)?=null
    private var statusHandler: ((Status) -> Unit)?=null
    private var sheduled=false
    var mShouldUnbind=false
    private var fcmToken:String?=null
    private var hpkToken:String?=null
    private var ruStoreToken:String?=null
    var status:Status=Status.NOTACTIVE
    var pushedToken:String?=null


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
        fun getSecure(context: Context):SharedPreferences{
            val masterKey: MasterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                "SecretPushed",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

        }

        fun refreshToken(
            context: Context,
            oldPushedToken: String?,
            fcmToken: String? = null,
            hpkToken: String? = null,
            ruStoreToken: String? = null,
            callback: ((String?) -> Unit)? = null
        ) {
            val secretPref = getSecure(context)

            val deviceSettings = JSONArray().apply {
                if (!fcmToken.isNullOrEmpty()) {
                    put(JSONObject().put("deviceToken", fcmToken).put("transportKind", "Fcm"))
                }
                if (!hpkToken.isNullOrEmpty()) {
                    put(JSONObject().put("deviceToken", hpkToken).put("transportKind", "Hpk"))
                }
                if (!ruStoreToken.isNullOrEmpty()) {
                    put(JSONObject().put("deviceToken", ruStoreToken).put("transportKind", "RuStore"))
                }
            }

            val content = JSONObject().apply {
                put("clientToken", oldPushedToken ?: "")
                put("operatingSystem", "Android")
                put("sdkVersion", Build.VERSION.SDK_INT.toString())

                if (deviceSettings.length() > 0) {
                    put("deviceSettings", deviceSettings)
                }

                try {
                    val permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    } else {
                        NotificationManagerCompat.from(context).areNotificationsEnabled()
                    }
                    put("displayPushNotificationsPermission", permissionGranted)
                } catch (e: Exception) {
                    addLogEvent(context, "Permission check error: ${e.message}")
                }

                try {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    val batteryOptimizationsIgnored = pm.isIgnoringBatteryOptimizations(context.packageName)
                    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    val backgroundRestricted =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            activityManager.isBackgroundRestricted
                        } else {
                            false
                        }

                    val backgroundWorkPermission = batteryOptimizationsIgnored && !backgroundRestricted
                    put("backgroundWorkPermission", backgroundWorkPermission)
                } catch (e: Exception) {
                    addLogEvent(context, "Background permission check error: ${e.message}")
                }
            }

            addLogEvent(context, "refreshToken request body: $content")

            val body = content.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://sub.pushed.ru/v2/tokens")
                .post(body)
                .build()

            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    addLogEvent(context, "refreshToken error: ${e.message}")
                    callback?.invoke(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        addLogEvent(context, "refreshToken failed with code: ${response.code}")
                        callback?.invoke(null)
                        return
                    }

                    val responseBody = response.body?.string()
                    try {
                        val model = JSONObject(responseBody!!)["model"] as JSONObject
                        val newToken = model.optString("clientToken", null)
                        if (!newToken.isNullOrEmpty()) {
                            secretPref.edit().apply {
                                putString("token", newToken)
                                if (!fcmToken.isNullOrEmpty()) putString("fcmtoken", fcmToken)
                                if (!hpkToken.isNullOrEmpty()) putString("hpktoken", hpkToken)
                                if (!ruStoreToken.isNullOrEmpty()) putString("rustoretoken", ruStoreToken)
                                apply()
                            }
                        }
                        callback?.invoke(newToken)
                    } catch (e: Exception) {
                        addLogEvent(context, "refreshToken parse error: ${e.message}")
                        callback?.invoke(null)
                    }
                }
            })
        }

        private fun getBitmap(context: Context,uri:String?): Bitmap?{
            if(uri=="null") return null
            var bigIconRes=context.resources.getIdentifier(uri,"mipmap",context.packageName)
            if(bigIconRes==0)
                bigIconRes=context.resources.getIdentifier(uri,"drawable",context.packageName)
            if(bigIconRes!=0) return BitmapFactory.decodeResource(context.resources,bigIconRes)
            var bitmap: Bitmap?=null
            try {
                val url = URL(uri)
                val connection =
                    url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val stream = connection.inputStream
                bitmap = BitmapFactory.decodeStream(stream)
                Log.d("DemoApp", "Res: ${bitmap?.density}")
            }
            catch (e:Exception){
                addLogEvent(context, "Get Bitmap Error: ${e.message}")
            }
            return bitmap
        }
        fun showNotification(context: Context,pushedNotification: JSONObject){
            addLogEvent(context ,"Notification: $pushedNotification")
            val sp =context.getSharedPreferences("Pushed",Context.MODE_PRIVATE)
            val channel= sp.getString("channel",null) ?: return
            val id=sp.getInt("pushId",0)+1
            val body=(pushedNotification["Body"].toString())
            if(body=="null") return
            var iconRes=context.resources.getIdentifier(pushedNotification["Logo"].toString(),"mipmap",context.packageName)
            if(iconRes==0)
                iconRes=context.resources.getIdentifier(pushedNotification["Logo"].toString(),"drawable",context.packageName)
            if(iconRes==0)
                iconRes=context.applicationInfo.icon
            if(iconRes==0) return
            val bitmap= getBitmap(context,pushedNotification["Image"].toString())
            var intent=context.packageManager.getLaunchIntentForPackage(context.packageName)
            try{
                if(pushedNotification["Url"].toString() !="null"){
                    intent=Intent(Intent.ACTION_VIEW, Uri.parse(pushedNotification["Url"].toString()))
                    intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            } catch (e:Exception){
                intent=context.packageManager.getLaunchIntentForPackage(context.packageName)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)
            var title=""
            if(pushedNotification["Title"].toString()!="null")
                title=pushedNotification.getString("Title")
            try {
                val builder = NotificationCompat.Builder(context, channel).apply {
                    setSmallIcon(iconRes)
                    setContentTitle(title)
                    setContentText(body)
                    setAutoCancel(true)
                    setContentIntent(pendingIntent)
                    priority = NotificationCompat.PRIORITY_MAX
                    if(bitmap!=null){
                        setLargeIcon(bitmap)
                        setStyle(NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon(null))
                    }
                }
                with(NotificationManagerCompat.from(context)) {
                    notify(id, builder.build())
                }
                sp.edit().putInt("pushId",id).apply()
            }
            catch (e:SecurityException) {
                addLogEvent(context ,"Notify Security Error: ${e.message}")
            }
            catch (e:Exception) {
                addLogEvent(context ,"Notify Error: ${e.message}")
            }

        }


        fun addLogEvent(context: Context? ,event:String){
            val sp = context?.getSharedPreferences("Pushed", Context.MODE_PRIVATE)
            if(sp?.getBoolean("enablelogger",false)==true) {
                    val date: String = Calendar.getInstance().time.toString()
                    val fEvent = "$date: $event\n"
                    Log.d("PushedLogger", fEvent)
                    val log = sp.getString("log", "")
                    sp.edit().putString("log", log + fEvent).apply()
            }
        }
        fun getLog(context: Context? ):String{
            val sp =context?.getSharedPreferences("Pushed",Context.MODE_PRIVATE)
            if(sp!=null) return sp.getString("log","")?:""
            return ""
        }
        fun confirmDelivered(context: Context? ,messageId :String,transport:String,traceId:String){
            val sp =context?.getSharedPreferences("Pushed",Context.MODE_PRIVATE)
            addLogEvent(context,"Confirm: $messageId/$transport")
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
      pref.edit().putBoolean("enablelogger", enableLogger).apply()
      pushedToken=secretPref.getString("token",null)
        fcmToken=secretPref.getString("fcmtoken",null)
        ruStoreToken=secretPref.getString("rustoretoken",null)
        hpkToken=secretPref.getString("hpktoken",null)
        pushedToken=getNewToken()
        addLogEvent(context,"Pushed Token: $pushedToken")
        val firstRun = pref.getBoolean("firstrun", true)
        if (channel != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(channel, "Messages", NotificationManager.IMPORTANCE_HIGH)
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(notificationChannel)
            }
            if (firstRun) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        try {
                            val intent = Intent(context, PushedPermissionActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            addLogEvent(context, "PermissionActivity start error: ${e.message}")
                            getNewToken()
                        }
                    } else {
                        getNewToken()
                    }
                } else {
                    getNewToken()
                }
            }
        }
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName) && firstRun) {
            val batteryIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            batteryIntent.data = Uri.parse("package:${context.packageName}")
            context.startActivity(batteryIntent)
        }
        pref.edit().putBoolean("firstrun", false).apply()
        if (pushedToken != null) {
            status = Status.OFFLINE
            pref.edit().putString("listenerclass", messageReceiverClass?.name).apply()
            pref.edit().putString("channel", channel).apply()
            pref.edit().putBoolean("enablelogger", enableLogger).apply()

            messageObserver = Observer<JSONObject> { message: JSONObject? ->
                if (messageHandler == null || messageHandler?.invoke(message!!) == false) {
                    try {
                        val notification = JSONObject(message!!["pushedNotification"].toString())
                        showNotification(context, notification)
                    } catch (e: Exception) {
                        addLogEvent(context, "Notification error: ${e.message}")
                    }
                    if (messageReceiverClass != null) {
                        val intent = Intent(context, messageReceiverClass)
                        intent.action = "ru.pushed.action.MESSAGE"
                        intent.putExtra("message", message.toString())
                        context.sendBroadcast(intent)
                    }
                }
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
    fun setStatusHandler(handler: (Status)->Unit){
        statusHandler=handler
    }
    fun unbindService(){
        messageHandler=null
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

        addLogEvent(context,"Message Service($binderId): $data")
        if(data.has("ServiceStatus")){
            if(status!= Status.valueOf(data.getString("ServiceStatus"))){
                status=Status.valueOf(data.getString("ServiceStatus"))
                addLogEvent(context,"Status changed: $status")
                statusHandler?.invoke(status)

            }

        }
        else return messageHandler?.invoke(data)?:false
        return true

    }
    fun start(onMessage:((JSONObject)->Boolean)?):String? {
        if(pushedToken==null) return null
        messageHandler=onMessage
        val serviceIntent=Intent(context,BackgroundService::class.java)
        serviceIntent.putExtra("binder_id", binderId)
        context.startService(serviceIntent)
        mShouldUnbind=context.bindService(serviceIntent,serviceConnection,Context.BIND_AUTO_CREATE)
        if(!sheduled){
            sheduled=true
            PushedJobIntentService.deactivateJob()
            val jobIntent = Intent(context, PushedJobIntentService::class.java)
            PushedJobIntentService.enqueueWork(context, jobIntent)
            //PushedJobService.stopActiveJob(context)
            //PushedJobService.startMyJob(context,3000,5000,1)
        }
        pref.edit().putBoolean("restarted",false).apply()
        return pushedToken
    }

    fun getNewToken(): String? {
      val oldToken = secretPref.getString("token", null)

      // Если токена ещё ни разу не было — снимаем ограничения StrictMode
      val isFirstTokenRequest = oldToken.isNullOrEmpty()
      if (isFirstTokenRequest) {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
      }

      val isMainThread = Looper.getMainLooper().thread == Thread.currentThread()

      // Если токен пустой — ждём синхронно (даже на UI потоке, если нужно)
      if (isFirstTokenRequest || !isMainThread) {
        val latch = CountDownLatch(1)
        var resultToken: String? = oldToken

        refreshToken(context, oldToken, fcmToken, hpkToken, ruStoreToken) { newToken ->
          if (!newToken.isNullOrEmpty()) {
            resultToken = newToken
            pushedToken = newToken
          }
          latch.countDown()
        }

        latch.await()
        return resultToken
      }

      // Если уже есть старый токен — просто обновляем его асинхронно
      refreshToken(context, oldToken, fcmToken, hpkToken, ruStoreToken) { newToken ->
        if (!newToken.isNullOrEmpty()) {
          pushedToken = newToken
        }
      }

      return oldToken
    }

}
