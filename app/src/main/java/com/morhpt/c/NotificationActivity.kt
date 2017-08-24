package com.morhpt.c

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.AppCompatButton
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mikhaellopez.circularimageview.CircularImageView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_notification.*

class NotificationActivity : GodLikeActivity() {

    private val fUser = FirebaseAuth.getInstance().currentUser!!
    private val database = FirebaseDatabase.getInstance().reference.child("users").child(fUser.uid)

    private var mNotifChatAdapter: FirebaseRecyclerAdapter<Friends, NotifChatHolder>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        title = "Notifications"

        val chatNotifDb = database.child("friends").orderByChild("status").equalTo("requested")

        mNotifChatAdapter = object : FirebaseRecyclerAdapter<Friends, NotifChatHolder>(
                Friends::class.java,
                R.layout.item_notif_chat,
                NotifChatHolder::class.java,
                chatNotifDb
        ){
            override fun populateViewHolder(viewHolder: NotifChatHolder?, model: Friends?, position: Int) {
                viewHolder?.setAll(this@NotificationActivity, this.getRef(position).key, model)
            }
        }

        notification_chat_notifs.layoutManager = LinearLayoutManager(this)
        notification_chat_notifs.adapter = mNotifChatAdapter
    }

    class NotifChatHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mainView = itemView.findViewById<CardView>(R.id.item_notif_chat_card_view)
        val image    = itemView.findViewById<CircularImageView>(R.id.item_notif_chat_photo)
        val name     = itemView.findViewById<TextView>(R.id.item_notif_chat_user_name)
        val accept   = itemView.findViewById<AppCompatButton>(R.id.item_notif_chat_accept_btn)
        val decline  = itemView.findViewById<AppCompatButton>(R.id.item_notif_chat_decline_btn)

        fun setAll(context: Context, key: String, model: Friends?){
            val database = FirebaseDatabase.getInstance().reference.child("users")

            database.child(model?.userId).addValueEventListener( object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError?) {}

                override fun onDataChange(p0: DataSnapshot?) {
                    val user = p0?.getValue(User::class.java)

                    Picasso.with(context)
                            .load(user?.photoURL)
                            .into(image)
                    name.text = user?.displayName
                }
            })


            accept.setOnClickListener {
                database.child(FirebaseAuth.getInstance().currentUser?.uid).child("friends").limitToFirst(1).orderByChild("userId").equalTo(model?.userId).addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError?) { }

                            override fun onDataChange(p0: DataSnapshot) {
                                p0.children
                                        .map { it.key }
                                        .forEach { database.child(FirebaseAuth.getInstance().currentUser?.uid).child("friends").child(it).child("status").setValue("friend") }
                            }
                        }
                )
            }

            decline.setOnClickListener {
                database.child(FirebaseAuth.getInstance().currentUser?.uid).child("friends").limitToFirst(1).orderByChild("userId").equalTo(model?.userId).addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError?) { }

                            override fun onDataChange(p0: DataSnapshot) {
                               p0.ref.setValue(null)
                            }
                        }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mNotifChatAdapter?.cleanup()
    }
}
