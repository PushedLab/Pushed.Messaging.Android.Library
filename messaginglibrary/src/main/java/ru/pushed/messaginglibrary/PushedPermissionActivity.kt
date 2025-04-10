package ru.pushed.messaginglibrary

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PushedPermissionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    2222
                )
            } else {
                finishAndRefresh(true)
            }
        } else {
            finishAndRefresh(true)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 2222) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            finishAndRefresh(granted)
        } else {
            finishAndRefresh(null)
        }
    }

    private fun finishAndRefresh(granted: Boolean?) {
        try {
            val context = applicationContext
            val prefs = context.getSharedPreferences("Pushed", MODE_PRIVATE)
            prefs.edit().putBoolean("firstrun", false).apply()

            // вызовем getNewToken с актуальным значением
            val service = PushedService(context, null)
            service.getNewToken()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finish()
    }
}
