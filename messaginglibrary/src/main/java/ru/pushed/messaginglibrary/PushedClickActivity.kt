package ru.pushed.messaginglibrary

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

class PushedClickActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val messageId  = intent.getStringExtra("messageId") ?: return finish()
    val transport  = intent.getStringExtra("transport") ?: "Fcm"
    val traceId    = intent.getStringExtra("traceId")   ?: ""
    val url        = intent.getStringExtra("url")       ?: ""
    val launchPkg  = packageName

    PushActionReceiver.send(this, messageId, "Click")

    val target = if (url.isNotEmpty()) {
      Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    } else {
      packageManager.getLaunchIntentForPackage(launchPkg)
    }

    startActivity(target)
    overridePendingTransition(0, 0)   // без анимации
    finish()
  }
}
