package ru.pushed.messaginglibrary

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject

class PushedPermissionActivity : Activity() {

    var needAskNotification = false
    var needAskOptimization = false

    private fun getOptimizationInfo() : Boolean{
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return !pm.isIgnoringBatteryOptimizations(packageName)
    }
    private fun askOptimizationInfo(){
        if(needAskOptimization && getOptimizationInfo()){
            val batteryIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            batteryIntent.data = Uri.parse("package:$packageName")
            startActivityForResult(batteryIntent,3333)
        }
        else{
            finishAndRefresh()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        needAskNotification = intent.getBooleanExtra("notification",false)
        needAskOptimization = intent.getBooleanExtra("optimization",false)
        if(needAskNotification){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    2222
                    )
                } else {
                    askOptimizationInfo()
                }
            } else {
                askOptimizationInfo()
            }
        }
        else {
            askOptimizationInfo()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(getOptimizationInfo()){
            PushedService.addServerLog(this,"The user refused to work in the background", JSONObject())
        }
        else{
            PushedService.addServerLog(this,"The user has allowed work in the background", JSONObject())

        }
        finishAndRefresh()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            PushedService.addServerLog(this, "The user rejected the notification messages", JSONObject())
        }
        else{
            PushedService.addServerLog(this, "The user has allowed notification messages", JSONObject())
        }
        askOptimizationInfo()
    }

    private fun finishAndRefresh() {
        try {
            val context = applicationContext
            val secretPref = PushedService.getSecure(context)
            val oldPushedToken=secretPref.getString("token",null)

            // Просто инициируем обновление токена напрямую
            PushedService.refreshToken(context, oldPushedToken) { token ->
                PushedService.addLogEvent(context, "PermissionActivity token refresh result: $token")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finish()
    }

}
