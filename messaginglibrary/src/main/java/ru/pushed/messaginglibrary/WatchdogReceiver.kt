package ru.pushed.messaginglibrary

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat

class WatchdogReceiver: BroadcastReceiver() {
    private val QUEUE_REQUEST_ID = 111
    private val ACTION_RESPAWN = "pushed.background_service.RESPAWN"

    fun enqueue(context: Context){
        enqueue(context,900000)

    }
    fun enqueue(context:Context, millis:Int){

        val intent = Intent(context, WatchdogReceiver::class.java)
        intent.action = ACTION_RESPAWN
        val manager = context.getSystemService(ALARM_SERVICE) as AlarmManager

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        val pIntent = PendingIntent.getBroadcast(context, QUEUE_REQUEST_ID, intent, flags)
        manager.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis() + millis, pIntent)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("WatchDog","Alarm:${BackgroundService.active}")
        PushedService.addLogEvent(context,"Alarm1:${BackgroundService.active}")
        if(intent?.action==ACTION_RESPAWN){
            val pref=context?.getSharedPreferences("Pushed", Context.MODE_PRIVATE)
            try{
                //if(BackgroundService.active /*|| SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE*/){
                    context?.startService(Intent(context, BackgroundService::class.java))
                    if(!BackgroundService.active) pref?.edit()?.putBoolean("restarted",true)?.apply()
                    PushedService.addLogEvent(context,"Alarm start service")
                //}
            }
            catch (e: Exception){
                PushedService.addLogEvent(context,"Alarm Err:${e.message}")
                pref?.edit()?.putBoolean("restarted",false)?.apply()
                /*if(!BackgroundService.active){
                    pref?.edit()?.putBoolean("restarted",false)?.apply()
                    if(PushedJobService.startMyJob(context!!,3000,5000,1))
                        PushedService.addLogEvent(context,"Sheduled")
                }*/

            }
            if(!BackgroundService.active){
                try{
                    val mgr=context!!.getSystemService(Context.POWER_SERVICE) as PowerManager
                    val wakeLock=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,this::class.java.name)
                    wakeLock?.setReferenceCounted(true)
                    wakeLock?.acquire(3000)
                    //pref?.edit()?.putBoolean("restarted",false)?.apply()
                    //if(PushedJobService.startMyJob(context!!,3000,5000,1))
                    //    PushedService.addLogEvent(context,"Alarm Sheduled")
                    val jobIntent = Intent(context, PushedJobIntentService::class.java)
                    PushedJobIntentService.enqueueWork(context, jobIntent)
                    WatchdogReceiver().enqueue(context)
                }
                catch (e: Exception){
                    PushedService.addLogEvent(context,"Alarm  Job Err:${e.message}")
                }
            }

        }

    }


}