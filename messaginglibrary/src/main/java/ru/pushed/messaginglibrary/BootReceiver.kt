package ru.pushed.messaginglibrary

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        PushedService.addLogEvent(context,"Boot:${BackgroundService.active}")
        if(intent?.action==Intent.ACTION_BOOT_COMPLETED || intent?.action=="android.intent.action.QUICKBOOT_POWERON"){
            val pref=context?.getSharedPreferences("Pushed", Context.MODE_PRIVATE)
            if(pref?.getString("token","")!="") {
                try {
                    context?.startService(Intent(context, BackgroundService::class.java))
                    if (!BackgroundService.active)
                        pref?.edit()?.putBoolean("restarted", true)?.apply()
                    PushedService.addLogEvent(context,"Boot start service")
                } catch (e: Exception) {
                    PushedService.addLogEvent(context,"Boot Err:${e.message}")
                    if (!BackgroundService.active) {
                        pref?.edit()?.putBoolean("restarted", false)?.apply()
                        if(PushedJobService.startMyJob(context!!, 3000, 5000, 1))
                            PushedService.addLogEvent(context,"Sheduled")
                    }
                }
            }
        }
    }
}