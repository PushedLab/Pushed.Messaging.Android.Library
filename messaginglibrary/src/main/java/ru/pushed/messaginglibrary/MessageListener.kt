package ru.pushed.messaginglibrary

import android.content.Context
import android.os.PowerManager
import android.util.Log
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MessageListener (private val url : String, context: Context, val listener: (JSONObject)->Unit) : WebSocketListener(){
    private val tag="MessageListener"
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0,  TimeUnit.MILLISECONDS)
        .build()
    private var wakeLock: PowerManager.WakeLock?=null
    private var activeWebSocket: WebSocket?=null
    private var connected = false
    private var active=false
    private val connectivity = Connectivity(context)
    init {
        val mgr=context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,this::class.java.name)
        wakeLock?.setReferenceCounted(true)
        lock()
        connectivity.setListener {
            Log.d(tag,"Con: $it")
            if(it==Connectivity.Status.WIFI) disconnect()
            connected = it != Connectivity.Status.NONE
            connect()
        }
    }

    private fun connect() {
        if(!connected) {
            unLock()
            return
        }
        if(active) return
        active=true
        disconnect()
        val request= Request.Builder()
            .url(url)
            .build()
        client.newWebSocket(request,this)
    }
    fun disconnect(){
        activeWebSocket?.cancel()
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        lock()
        val message=bytes.utf8()
        Log.d(tag,"onMessage: $message")
        if(message!="ONLINE") listener(JSONObject(message))
        Thread.sleep(3000)
        unLock()
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(tag,"webSocked Open")
        activeWebSocket=webSocket
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(tag,"webSocked Closing")
        disconnect()

    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        lock()
        Log.d(tag,"Err: $response")
        activeWebSocket=null
        active=false
        Thread.sleep(1000)
        connect()
    }
    private fun lock(){
        if(wakeLock?.isHeld == false){
            Log.d(tag,"Lock")
            wakeLock?.acquire(60*1000)
        }
    }
    private fun unLock(){
        if(wakeLock?.isHeld == true){
            Log.d(tag,"Unlock")
            wakeLock?.release()
        }
    }


}