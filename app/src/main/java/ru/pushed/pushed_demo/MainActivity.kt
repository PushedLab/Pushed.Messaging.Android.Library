package ru.pushed.pushed_demo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import ru.pushed.pushed_demo.ui.MainViewModel
import ru.pushed.pushed_demo.ui.PushedDemoScreen
import ru.pushed.pushed_demo.ui.theme.PushedDemoTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupNotificationChannel()
        requestNotificationPermission()

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            PushedDemoTheme {
                PushedDemoScreen(
                    state       = state,
                    onCopyToken = {
                        copyToClipboard("pushed_token", state.token ?: "")
                        Toast.makeText(this, "Token copied", Toast.LENGTH_SHORT).show()
                    },
                    onCopyLogs  = {
                        copyToClipboard("pushed_logs", viewModel.getLogs())
                        Toast.makeText(this, "Logs copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.start()
    }

    override fun onStop() {
        viewModel.unbind()
        super.onStop()
    }

    private fun copyToClipboard(label: String, text: String) {
        val cb = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "messages", "Messages", NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }
}
