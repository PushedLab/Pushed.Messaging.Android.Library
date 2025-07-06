package ru.pushed.messaginglibrary

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class PushActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action

    val messageId = intent.getStringExtra("messageId") ?: run {
      PushedService.addLogEvent(context, "PushActionReceiver: No messageId in intent extras!")
      return
    }
    val transport = intent.getStringExtra("transport") ?: "Fcm"
    val traceId = intent.getStringExtra("traceId") ?: ""

    when (action) {
      "ru.pushed.action.CLICK" -> {
        PushedService.addLogEvent(context, "Action CLICK → tracking 'Click' interaction")
        send(context, messageId, "Click")
        PushedService.confirmDelivered(context, messageId, transport, traceId)
        // Запускаем сохранённый интент
        val clickIntent: Intent? = intent.getParcelableExtra("clickIntent")
        if (clickIntent != null) {
          clickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          context.startActivity(clickIntent)
        } else {
          // fallback, если нет интента
          val fallbackIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
          fallbackIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          context.startActivity(fallbackIntent)
        }
      }

      "ru.pushed.action.DISMISS" -> {
        PushedService.addLogEvent(context, "Action DISMISS → confirming delivery and tracking 'Close'")
        PushedService.confirmDelivered(context, messageId, transport, traceId)
        send(context, messageId, "Close")
      }

      "ru.pushed.action.SHOWN" -> {
        PushedService.addLogEvent(context, "Action SHOWN → tracking 'Show' interaction") 
        PushedService.confirmDelivered(context, messageId, transport, traceId)
        send(context, messageId, "Show")
      }

      else -> {
        PushedService.addLogEvent(context, "Unknown action received in PushActionReceiver: $action")
      }
    }
  }

  companion object InteractionSender {
    fun send(context: Context, messageId: String, interaction: String) {
      val sp = PushedService.getSecure(context)
      val token = sp.getString("token", null)


      if (token == null) {
        PushedService.addLogEvent(context, "Interaction $interaction skipped: no token found in secure storage")
        return
      }

      val basicAuth = "Basic " + Base64.encodeToString("$token:$messageId".toByteArray(), Base64.NO_WRAP)
      val url = "https://api.multipushed.ru/v2/mobile-push/confirm-client-interaction?clientInteraction=$interaction"

      PushedService.addLogEvent(context, "Sending interaction [$interaction] to $url")

      val request = Request.Builder()
        .url(url)
        .post("".toRequestBody(null))
        .addHeader("Authorization", basicAuth)
        .build()

      val client = OkHttpClient()
      client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
          PushedService.addLogEvent(context, "Interaction $interaction failed: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
          if (response.isSuccessful) {
            PushedService.addLogEvent(context, "Interaction $interaction SUCCESS: HTTP ${response.code}")
          } else {
            PushedService.addLogEvent(context, "Interaction $interaction ERROR: HTTP ${response.code} — ${response.message}")
          }
        }
      })
    }
  }
}
