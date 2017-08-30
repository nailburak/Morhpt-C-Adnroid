package com.morhpt.c

import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.morhpt.c.help.DownloadImageTask
import com.morhpt.c.help.JavaHelp
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_messages.*
import java.io.IOException
import java.net.URL
import java.util.*


class MessagesActivity : GodLikeActivity() {


    private var mAdapter: FirebaseRecyclerAdapter<Messages, MessagesHolder>? = null
    private var chatId: String? = null
    private var chatUserId: String? = null

    private val PICK_IMAGE: Int = 1124
    private val FILE_REQUEST_CODE: Int = 1145
    private val PERMISSIONS_REQUEST_FILE: Int = 1144

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

        message_file_icon.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSIONS_REQUEST_FILE)
            } else chooseImage()
        }

        messages_back_arrow.setOnClickListener {
            finish()
        }
    }

    class MessagesHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val main = itemView.findViewById<RelativeLayout>(R.id.item_messages_main_view)
        val cardView = itemView.findViewById<RelativeLayout>(R.id.item_messages_card_view)
        val imageLayout = itemView.findViewById<RelativeLayout>(R.id.item_messages_image_layout)
        val message = itemView.findViewById<TextView>(R.id.item_messages_message)
        val image = itemView.findViewById<ImageView>(R.id.item_messages_image)



        fun setAll(model: Messages?, context: Context){

            if (model?.message == null){
                cardView.visibility = View.GONE
                imageLayout.visibility = View.VISIBLE

                Glide.with(context)
                        .load(model?.image)
                        .centerCrop()
                        .into(image)

                if (model?.messageFrom.equals(FirebaseAuth.getInstance().currentUser!!.uid)) {
                    imageLayout.gravity = Gravity.RIGHT
                } else {
                    imageLayout.gravity = Gravity.LEFT
                }

                image.setOnClickListener {
                    val intent = Intent(context, FullImageActivity::class.java)
                    intent.putExtra("url", model?.image)
                    context.startActivity(intent)
                }



                image.setOnLongClickListener {
                    try {
                        val url = URL(model?.image)
                        val bitmap = image.drawingCache

                        val options = arrayOf("Save Image", "Share Image")
                        val builder = AlertDialog.Builder(context)
                        builder.setTitle("Choose one")
                        builder.setItems(options) { dialog, which ->
                            when (which) {
                                0 -> DownloadImageTask(false, context).execute(model?.image)
                                1 -> DownloadImageTask(true, context).execute(model?.image)
                            }
                        }
                        builder.show()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    true
                }

            } else {
                imageLayout.visibility = View.GONE
                cardView.visibility = View.VISIBLE

                message.text = model?.message

                if (model?.messageFrom.equals(FirebaseAuth.getInstance().currentUser!!.uid)) {
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
                        when (which) {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode != RESULT_OK)
            return

        if (data == null)
            return

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK){
            val pickedImage = data.data

            val storage = FirebaseStorage.getInstance()
            val chatImages = storage.reference.child("chats").child(chatId!!).child(pickedImage.lastPathSegment)

            messages_progress_bar.visibility = View.VISIBLE

            val newBitmap = JavaHelp.decodeUri(this@MessagesActivity, pickedImage, 600)
            val byte = JavaHelp.getImageData(newBitmap)

            chatImages.putBytes(byte).addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                messages_progress_bar.progress = progress.toInt()
            }.addOnSuccessListener { taskSnapshot ->
                val downloadUrl = taskSnapshot.downloadUrl

                FirebaseDatabase.getInstance().reference.child("chats").child(chatId).push().setValue(Messages(null, downloadUrl.toString(), FirebaseAuth.getInstance().currentUser!!.uid, chatUserId ))
                        .addOnCompleteListener {
                            messages_progress_bar.progress = 0
                            messages_progress_bar.visibility = View.GONE
                        }
            }

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            PERMISSIONS_REQUEST_FILE -> {
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    chooseImage()

                } else {

                    Snackbar.make(messages_main, "You won't be able to add a friend unless accept it!", Snackbar.LENGTH_LONG).show()
                }
                return
            }
        }
    }



    fun chooseImage() {
        val intent = Intent()
        intent.type = "image/* video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select a Picture"), PICK_IMAGE)
    }
}
