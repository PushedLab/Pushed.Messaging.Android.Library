package ru.pushed.pushedexample

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import ru.pushed.messaginglibrary.BackgroundService
import ru.pushed.messaginglibrary.PushedService


class MainActivity : AppCompatActivity() {
    private lateinit var titleText: TextView
    private lateinit var bodyText: TextView
    private lateinit var restartButton: Button
    private lateinit var pushedService:PushedService
    private lateinit var tokenText:TextView
    private var token:String?=""

    override fun onStop() {
        pushedService.unbindService()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        token=pushedService.start(){message ->
            try {
                runOnUiThread{
                    titleText.text=message["title"].toString()
                    bodyText.text=message["body"].toString()
                }
                true
            }
            catch (e:Exception) {false}

        }
        tokenText.text="Token: $token"
        Log.d("App","Token: $token")

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel= NotificationChannel("messages","Messages", NotificationManager.IMPORTANCE_HIGH)
            val notificationManager=getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        titleText=findViewById(R.id.title_text_view)
        bodyText=findViewById(R.id.body_text_view)
        restartButton=findViewById(R.id.restart_button)
        tokenText=findViewById(R.id.token_text_view)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1);
            }
        }
        pushedService= PushedService(this,"Pushed","Service is active",
            ru.pushed.messaginglibrary.R.mipmap.ic_bg_service_small,MyBackgroundService::class.java)

        restartButton.setOnClickListener{
            var myClipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText("simple text", token)
            myClipboard.setPrimaryClip(clip)

        }

    }
}