package com.morhpt.c

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Created by Burak on 2017-08-19.
 */

data class Chats(val chatUserId: String? = null)
data class Messages(val message: String? = null, val messageFrom: String? = null, val messageTo: String? = null)
data class User(val email: String? = null, val displayName: String? = null, val status: String? = null, val photoURL: String? = null, val qrcode: String? = null)
data class Friends(val userId: String? = null, val status: String? = null)

class setOfline {
    val presenceRef = FirebaseDatabase.getInstance().reference.child("users").child(FirebaseAuth.getInstance().currentUser!!.uid).child("status")
    init {
        presenceRef.onDisconnect().setValue("offline")
    }
}
