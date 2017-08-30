package com.morhpt.c

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.content.Intent
import android.support.v4.app.RemoteInput


class FCMActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fcm)

        Log.wtf("notif text ", getMessageText(intent).toString())
    }

    private fun getMessageText(intent: Intent): CharSequence? {
        val KEY_TEXT_REPLY = "key_text_reply"
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        return if (remoteInput != null) {
            remoteInput!!.getCharSequence(KEY_TEXT_REPLY)
        } else null
    }
}
