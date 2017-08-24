package com.morhpt.c

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.content_profile.*
import android.view.View


class ProfileActivity : GodLikeActivity() {

    private val fUser = FirebaseAuth.getInstance().currentUser!!
    val database = FirebaseDatabase.getInstance().reference
    var uid:String? = null
    var displayName:String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val toolbar = findViewById<Toolbar>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.md_white_1000))


        if (savedInstanceState == null) {
            val extras = intent.extras
            if (extras == null) {
                uid = null
                displayName = null
            } else {
                uid = extras.getString("uid")
                displayName = extras.getString("displayName")
            }
        } else {
            uid = savedInstanceState.getSerializable("uid") as String
            displayName = savedInstanceState.getSerializable("displayName") as String
        }

        title = displayName

        database.child("users").child(uid).addValueEventListener(
                object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError?) { }

                    override fun onDataChange(p0: DataSnapshot?) {
                        val user = p0?.getValue(User::class.java)

                        Picasso.with(this@ProfileActivity)
                                .load(user?.photoURL)
                                .into(profile_image)
                        title = user?.displayName
                        profile_email.text = user?.email
                    }
                }
        )

        database.child("users").child(fUser.uid).child("friends").orderByChild("userId").equalTo(uid).addValueEventListener(
                object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError?) { }

                    override fun onDataChange(p0: DataSnapshot) {
                        if (p0.exists()){
                            profile_fab.visibility = View.GONE
                        }
                    }

                }
        )

        profile_fab.setOnClickListener {
            database.child("users").child(fUser.uid).child("friends").orderByChild("userId").equalTo(uid).addListenerForSingleValueEvent(
                    object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError?) { }

                        override fun onDataChange(p0: DataSnapshot) {
                            if (!p0.exists()){
                                database.child("users").child(fUser.uid).child("friends").push().setValue(Friends(uid, "pending")).addOnSuccessListener {
                                    Snackbar.make(profile_view, "Friend request sent", Snackbar.LENGTH_LONG).show()
                                }
                            } else {
                                val data = p0.getValue(Friends::class.java)

                                Snackbar.make(profile_view,
                                        if(data?.status.equals("pending"))
                                            "Waiting to accept the request"
                                        else if(data?.status.equals("friend"))
                                            "You're already friends!"
                                        else
                                            "Try again later!",
                                    Snackbar.LENGTH_LONG).show()



                            }
                        }

                    }
            )

        }

        profile_message_btn.setOnClickListener {
            database.child("users").child(FirebaseAuth.getInstance().currentUser!!.uid).child("chats").orderByChild("chatUserId").equalTo(uid).addListenerForSingleValueEvent(
                    object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError?) { }

                        override fun onDataChange(p0: DataSnapshot) {
                            for (objSnapshot in p0.children) {
                                val intent = Intent(this@ProfileActivity, MessagesActivity::class.java)
                                intent.putExtra("chatId",
                                        if (p0.hasChildren())
                                            objSnapshot.key
                                        else
                                            database.push().key)
                                intent.putExtra("chatUserId", uid)
                                startActivity(intent)
                            }
                        }
                    }
            )
        }

        profile_email_btn.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", profile_email.text.toString(), null))
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Hi There!")
            startActivity(Intent.createChooser(emailIntent, "Send email..."))
        }
    }
}
