package com.morhpt.c

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.database.DatabaseReference



/**
 * Created by Serra on 2017-08-24.
 */
open class GodLikeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }

    override fun onStart() {
        super.onStart()


        val currentUser = FirebaseAuth.getInstance().currentUser


        if (currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        val database = FirebaseDatabase.getInstance().reference.child("users").child(currentUser!!.uid)

        val refreshedToken = FirebaseInstanceId.getInstance().token
        database.addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError?) { }

                    override fun onDataChange(p0: DataSnapshot) {
                        if (!p0.hasChild("token"))
                            database.child("token").setValue(refreshedToken)
                    }

                }
        )

        database.child("status").setValue("online")

        setOfline()
    }

    override fun onPause() {
        super.onPause()
        val currentUser = FirebaseAuth.getInstance().currentUser


        if (currentUser != null) {
            val database = FirebaseDatabase.getInstance().reference.child("users").child(currentUser!!.uid)

            database.child("status").setValue("offline")
        }
    }
}