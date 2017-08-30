package com.morhpt.c.help

import android.app.IntentService
import android.content.Intent
import android.support.v4.app.RemoteInput
import android.util.Log


/**
 * Created by nbura on 2017-08-29.
 */
class NotificationService : IntentService("NotificationService") {

    override fun onHandleIntent(p0: Intent) {
        Log.wtf("hey", "from Intent Service")
        Log.wtf("notif text ", getMessageText(p0).toString())
    }

    private fun getMessageText(intent: Intent): CharSequence? {
        val KEY_TEXT_REPLY = "key_text_reply"
        return RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_TEXT_REPLY)
    }
}