package com.morhpt.c

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_messages.*
import java.util.*


class MessagesActivity : GodLikeActivity() {


    private var mAdapter: FirebaseRecyclerAdapter<Messages, MessagesHolder>? = null
    private var chatId: String? = null
    private var chatUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        setSupportActionBar(messages_toolbar)

        if (savedInstanceState == null) {
            val extras = intent.extras
            if (extras == null) {
                chatId = null
                chatUserId = null
            } else {
                chatId = extras.getString("chatId")
                chatUserId = extras.getString("chatUserId")
            }
        } else {
            chatId = savedInstanceState.getSerializable("chatId") as String
            chatUserId = savedInstanceState.getSerializable("chatUserId") as String
        }

        val context = this
        val database = FirebaseDatabase.getInstance().reference

        Log.wtf("chatUserID", chatUserId)
        database.child("users").child(chatUserId).addValueEventListener(
                object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError?) { }

                    override fun onDataChange(p0: DataSnapshot?) {
                        val user = p0?.getValue(User::class.java)

                        messages_chat_name.text = user?.displayName
                        messages_status.text = user?.status

                        Picasso.with(context)
                                .load(user?.photoURL)
                                .into(messages_user_photo)
                    }
                }
        )

        val chatRef = database.child("chats").child(chatId)
        chatRef.keepSynced(true)

        mAdapter = object : FirebaseRecyclerAdapter<Messages, MessagesHolder>(
                Messages::class.java,
                R.layout.item_messages,
                MessagesHolder::class.java,
                chatRef
        ){
            override fun populateViewHolder(viewHolder: MessagesHolder?, model: Messages?, position: Int) {
                val key = this.getRef(position).key

                viewHolder?.setAll(model, context)
            }
        }

        val layoutManager = LinearLayoutManager(this)
        //layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true

        messages_recycler_view.layoutManager = layoutManager
        messages_recycler_view.adapter = mAdapter

        (mAdapter as FirebaseRecyclerAdapter<Messages, MessagesHolder>).registerAdapterDataObserver(
                object : RecyclerView.AdapterDataObserver() {
                    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                        super.onItemRangeInserted(positionStart, itemCount)
                        layoutManager.smoothScrollToPosition(messages_recycler_view, null, (mAdapter as FirebaseRecyclerAdapter<Messages, MessagesHolder>).itemCount)
                    }
                }
        )

        messages_edit_text.addTextChangedListener(
                object : TextWatcher {
                    private var timer = Timer()
                    private val DELAY: Long = 500
                    val fUser = FirebaseAuth.getInstance().currentUser!!

                    override fun afterTextChanged(p0: Editable?) {
                         timer.cancel();
                            timer = Timer();
                            timer.schedule(
                                    object : TimerTask() {
                                        override fun run() {
                                            database.child("users").child(fUser.uid).child("status").setValue("online")
                                        }

                                    }
                            ,DELAY)
                    }

                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        database.child("users").child(fUser.uid).child("status").setValue("typing to someone")
                    }

                }
        )

        messages_send_button.setOnClickListener {
            val message = messages_edit_text.text.toString()
            val emptyCheck = messages_edit_text.text.toString().trim()

            if (message.length > 400){
                Snackbar.make(messages_main, "You cannot send long texts", Snackbar.LENGTH_SHORT).show()
            } else {



                if (!emptyCheck.isEmpty() || emptyCheck != "" || emptyCheck.contains("\u200F") || emptyCheck.contains("\u200E")) {
                    messages_edit_text.setText("")
                    val fUser = FirebaseAuth.getInstance().currentUser!!
                    database.child("users").child(fUser.uid).child("chats").orderByChild("chatUserId").equalTo(chatUserId).addListenerForSingleValueEvent(
                            object : ValueEventListener {
                                override fun onCancelled(p0: DatabaseError?) {}

                                override fun onDataChange(p0: DataSnapshot) {
                                    if (!p0.hasChildren()) {
                                        database.child("users").child(fUser.uid).child("chats").child(chatId).setValue(Chats(chatUserId))
                                    }
                                }

                            }
                    )

                    database.child("chats").child(chatId).push().setValue(Messages(message, FirebaseAuth.getInstance().currentUser!!.uid, chatUserId))
                }
            }
        }
    }

    class MessagesHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val main = itemView.findViewById<RelativeLayout>(R.id.item_messages_main_view)
        val cardView = itemView.findViewById<RelativeLayout>(R.id.item_messages_card_view)
        val message = itemView.findViewById<TextView>(R.id.item_messages_message)

        fun setAll(model: Messages?, context: Context){
            message.text =  model?.message

            val scale = context.resources.displayMetrics.density
            val dpAsPixels32 = (32 * scale + 0.5f).toInt()
            val dpAsPixels16 = (1 * scale + 0.5f).toInt()

            if (model?.messageFrom.equals(FirebaseAuth.getInstance().currentUser!!.uid /* user Id */)) {
                cardView.background = context.resources.getDrawable(R.drawable.chat_you)
                cardView.gravity = Gravity.CENTER
                val params = cardView.layoutParams as RelativeLayout.LayoutParams
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            } else {
                cardView.background = context.resources.getDrawable(R.drawable.chat_other)
                cardView.gravity = Gravity.CENTER
                val params = cardView.layoutParams as RelativeLayout.LayoutParams
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            }

            main.setOnLongClickListener {
                val options = arrayOf("Copy text", "Share Text")
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Choose one")
                builder.setItems(options) { dialog, which ->
                    when(which){
                        0 -> {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("message", model?.message)
                            clipboard.primaryClip = clip
                        }
                        1 -> {
                            val intent = Intent()
                            intent.action = Intent.ACTION_SEND
                            intent.putExtra(Intent.EXTRA_TEXT, model?.message)
                            intent.type = "text/plain"
                            context.startActivity(intent)
                        }
                    }
                }
                builder.show()
                true
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mAdapter?.cleanup()
    }
}
