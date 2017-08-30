package com.morhpt.c

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.morhpt.c.help.NotificationService

class EmptyActivity : GodLikeActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty)
    }

    override fun onStart() {
        super.onStart()

        startService(Intent(this, NotificationService::class.java))

        val currentUser = FirebaseAuth.getInstance().currentUser

        Log.wtf("hey", currentUser.toString())

        if (currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
