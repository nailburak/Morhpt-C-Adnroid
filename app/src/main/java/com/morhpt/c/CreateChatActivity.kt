package com.morhpt.c

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mikhaellopez.circularimageview.CircularImageView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_create_chat.*

class CreateChatActivity : GodLikeActivity() {

    private var mAdapter: FirebaseRecyclerAdapter<Friends, FriendsHolder>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_chat)

        title = "Friends"

        val database = FirebaseDatabase.getInstance().reference.child("users").child(FirebaseAuth.getInstance().currentUser!!.uid)
        database.keepSynced(true)
        val context = this

        mAdapter = object : FirebaseRecyclerAdapter<Friends, FriendsHolder>(
                Friends::class.java,
                R.layout.item_chats,
                FriendsHolder::class.java,
                database.child("friends").orderByChild("status").equalTo("friend")
        ){
            override fun populateViewHolder(viewHolder: FriendsHolder?, model: Friends?, position: Int) {
                viewHolder?.setAll(this.getRef(position).key, context, model)
            }
        }

        create_chat_recycler_view.layoutManager = LinearLayoutManager(this)
        create_chat_recycler_view.adapter = mAdapter
    }

    class FriendsHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val cardView = itemView.findViewById<CardView>(R.id.item_chat_card_view)
        val chatPhoto = itemView.findViewById<CircularImageView>(R.id.item_chats_photo)
        val chatName = itemView.findViewById<TextView>(R.id.item_chats_chat_name)
        val chatLastMessage = itemView.findViewById<TextView>(R.id.item_chats_chat_last_message)

        fun setAll(key: String, context: Context, model: Friends?){
            chatLastMessage.visibility = View.GONE

            val db = FirebaseDatabase.getInstance().reference.child("users").child(model?.userId)
            db.keepSynced(true)
            db.addValueEventListener(
                    object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError?)  {}

                        override fun onDataChange(p0: DataSnapshot?) {
                            val user = p0?.getValue(User::class.java)

                            chatName.text = user?.displayName
                            Picasso.with(context)
                                    .load(user?.photoURL)
                                    .into(chatPhoto)

                            setClickListener(db.child("chats").push().key, model?.userId, user?.photoURL, context)
                        }
                    }
            )
        }

        fun setClickListener(chatId: String, chatUserId: String?, photoURL: String?, context: Context){
            cardView.setOnClickListener {
                FirebaseDatabase.getInstance().reference.child("users").child(FirebaseAuth.getInstance().currentUser!!.uid).child("chats").orderByChild("chatUserId").equalTo(chatUserId).addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError?) { }

                            override fun onDataChange(p0: DataSnapshot) {
                                Log.wtf("hey", "hey")
                                if(!p0.exists()){
                                    val intent = Intent(context, MessagesActivity::class.java)
                                    intent.putExtra("chatId",

                                            chatId)
                                    intent.putExtra("chatUserId", chatUserId)
                                    context.startActivity(intent)
                                }
                                for (objSnapshot in p0.children) {
                                    Log.wtf("hey", "hoy")
                                    val intent = Intent(context, MessagesActivity::class.java)
                                    intent.putExtra("chatId",

                                            objSnapshot.key)
                                    intent.putExtra("chatUserId", chatUserId)
                                    context.startActivity(intent)
                                }
                            }
                        }
                )
            }

            chatPhoto.setOnClickListener {
                val dialog = Dialog(context)
                dialog.setContentView(R.layout.pp_dialog)
//                dialog.setTitle("Title...")

                val image = dialog.findViewById<ImageView>(R.id.pp_dialog_image)
                Picasso.with(context)
                        .load(photoURL)
                        .into(image)

                dialog.show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mAdapter?.cleanup()
    }
}
