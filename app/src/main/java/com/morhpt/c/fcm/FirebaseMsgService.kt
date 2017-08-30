package com.morhpt.c.fcm

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.morhpt.c.Messages
import com.morhpt.c.R
import com.morhpt.c.help.NotificationService
import android.R.attr.label
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.app.RemoteInput
import android.support.v4.app.TaskStackBuilder
import android.support.v7.app.NotificationCompat
import com.morhpt.c.FCMActivity
import java.io.ByteArrayOutputStream
import java.net.URL


class FirebaseMsgService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {

        val data = remoteMessage?.data

        Log.wtf("heyasd", data.toString())
        Log.wtf("heyasdasdas", remoteMessage.toString())

        if (remoteMessage != null){
            Log.wtf("hey", data!!["title"])
            sendNotification(data)
        }
    }

    private fun sendNotification(data: MutableMap<String, String>) {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id = "message"
        val name = data["title"]
        val description = "Message Notification"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(id, name, importance)

            mChannel.description = description
            mChannel.enableLights(true)
            mChannel.lightColor = Color.RED
            mChannel.enableVibration(true)

            mNotificationManager.createNotificationChannel(mChannel)

            val resultIntent = Intent(this, NotificationService::class.java)
            val stackBuilder = TaskStackBuilder.create(this)

            stackBuilder.addNextIntent(resultIntent)

            //val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
            val resultPendingIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            )
            val KEY_TEXT_REPLY = "key_text_reply"
            val replyLabel = "Reply"
            val remoteInput = android.app.RemoteInput.Builder(KEY_TEXT_REPLY)
                    .setLabel(replyLabel)
                    .build()
            val action = Notification.Action.Builder(R.drawable.ic_send_white_24dp,
                    "reply to $name", resultPendingIntent)
                    .addRemoteInput(remoteInput)
                    .build()
            val url = URL(data["image"])
            val image = BitmapFactory.decodeStream(url.openConnection().getInputStream())
            val mBuilder = Notification.Builder(this, id)
                    .setContentTitle(data["title"])
                    .setContentText(data["body"])
                    .setLargeIcon(image)
                    .addAction(action)
                    .setSmallIcon(R.drawable.logo)
                    .setAutoCancel(true)

            mBuilder.setContentIntent(resultPendingIntent)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.notify(1, mBuilder.build())
        }
    }

    fun getImageData(bmp: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}