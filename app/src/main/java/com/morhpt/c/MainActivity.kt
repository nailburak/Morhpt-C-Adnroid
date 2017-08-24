package com.morhpt.c

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Base64
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.auth.FirebaseAuth
import com.mikhaellopez.circularimageview.CircularImageView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import android.view.MenuItem
import android.widget.EditText
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.vision.barcode.Barcode
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId


class MainActivity : GodLikeActivity() {

    private val SCAN_REQUEST_CODE = 11231
    private var mAdapter: FirebaseRecyclerAdapter<Chats, ChatsHolder>? = null
    private val fUser = FirebaseAuth.getInstance().currentUser!!
    private val database = FirebaseDatabase.getInstance().reference.child("users").child(fUser.uid)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database.keepSynced(true)
        val context = this


        mAdapter = object : FirebaseRecyclerAdapter<Chats, ChatsHolder>(
                Chats::class.java,
                R.layout.item_chats,
                ChatsHolder::class.java,
                database.child("chats")
        ){
            override fun populateViewHolder(viewHolder: ChatsHolder?, model: Chats?, position: Int) {
                viewHolder?.setAll(context, model, this.getRef(position).key)
            }
        }

        main_recycler_view.layoutManager = LinearLayoutManager(this)
        main_recycler_view.adapter = mAdapter

        main_create_chat.setOnClickListener {
            startActivity(Intent(this, CreateChatActivity::class.java))
        }
    }

     class ChatsHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val cardView = itemView.findViewById<CardView>(R.id.item_chat_card_view)
        val chatPhoto = itemView.findViewById<CircularImageView>(R.id.item_chats_photo)
        val chatName = itemView.findViewById<TextView>(R.id.item_chats_chat_name)
        val chatLastMessage = itemView.findViewById<TextView>(R.id.item_chats_chat_last_message)

        fun setAll(context: Context, model: Chats?, key: String){
            val db = FirebaseDatabase.getInstance().reference.child("users").child(model?.chatUserId)
            db.addValueEventListener(
                    object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError?) { }

                        override fun onDataChange(p0: DataSnapshot?) {
                            val user = p0?.getValue(User::class.java)

                            chatName.text = user?.displayName
                            setPhoto(user?.photoURL, context)
                            setClickListener(key, model?.chatUserId, user?.photoURL, context)

                            FirebaseDatabase.getInstance().reference.child("chats").child(key).limitToLast(1).addValueEventListener(
                                    object : ValueEventListener {
                                        override fun onCancelled(p0: DatabaseError?) { }

                                        override fun onDataChange(p0: DataSnapshot) {
                                            for (child in p0?.children){
                                                chatLastMessage.text = child.child("message").value.toString()
                                            }
                                        }
                                    }
                            )
                        }
                    }
            )
        }

        fun setPhoto(photoURL: String?, context: Context){
            Picasso.with(context)
                    .load(photoURL)
                    .into(chatPhoto)
        }

        fun setClickListener(chatId: String, chatUserId: String?, photoURL: String?, context: Context){
            cardView.setOnClickListener {
                val intent = Intent(context, MessagesActivity::class.java)
                intent.putExtra("chatId", chatId)
                intent.putExtra("chatUserId", chatUserId)
                context.startActivity(intent)
            }

            chatPhoto.setOnClickListener {
                val dialog = Dialog(context)
                dialog.setContentView(R.layout.pp_dialog)
                dialog.setTitle("Title...")

                val image = dialog.findViewById<ImageView>(R.id.pp_dialog_image)
                Picasso.with(context)
                        .load(photoURL)
                        .into(image)

                dialog.show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)

        val menuItem = menu.findItem(R.id.main_menu_notif)
        val actionView = MenuItemCompat.getActionView(menuItem)
        val count = actionView.findViewById<TextView>(R.id.custom_action_item_count)

        var countNotifs = -1

        database.child("friends").orderByChild("status").equalTo("requested").addChildEventListener(
                object : ChildEventListener{
                    override fun onCancelled(p0: DatabaseError?) { }

                    override fun onChildMoved(p0: DataSnapshot?, p1: String?) { }

                    override fun onChildChanged(p0: DataSnapshot?, p1: String?) { }

                    override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                        countNotifs += p0.childrenCount.toInt()
                        count.text = countNotifs.toString()
                    }

                    override fun onChildRemoved(p0: DataSnapshot) {
                        countNotifs -= p0.childrenCount.toInt()
                        count.text = if(countNotifs < 0) "0" else countNotifs.toString()
                    }
                }
        )






        count.setOnClickListener {
            onOptionsItemSelected(menuItem)
        }


        return true
    }

    private val PERMISSIONS_REQUEST_CAMERA: Int = 9547

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.main_menu_sign_out -> {
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener {
                            startActivity(Intent(this, SignInActivity::class.java))
                        }
            }
            R.id.main_menu_add_friend -> {
                val options = arrayOf("Scan a Qr code", "Enter User Id", "Upload Qr code")
                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle("Choose one")
                builder.setItems(options) { dialog, which ->
                    when(which){
                        0 -> {
                            if (ContextCompat.checkSelfPermission(this,
                                    android.Manifest.permission.CAMERA)
                                    != PackageManager.PERMISSION_GRANTED) {

                                ActivityCompat.requestPermissions(this,
                                        arrayOf(android.Manifest.permission.CAMERA),
                                        PERMISSIONS_REQUEST_CAMERA)
                            } else
                                startActivityForResult(Intent(this, AddFriendActivity::class.java), SCAN_REQUEST_CODE)
                        }
                        1 -> {
                            val edit = AlertDialog.Builder(this@MainActivity)
                            edit.setTitle("Enter UserId")
                            val edittext = EditText(this@MainActivity)
                            edit.setView(edittext)

                            edit.setPositiveButton("Go To Profile", { dialog, which ->
                                val uid = edittext.text.toString()

                                database.parent.child(uid).addListenerForSingleValueEvent(
                                        object : ValueEventListener {
                                            override fun onCancelled(p0: DatabaseError?) { }

                                            override fun onDataChange(p0: DataSnapshot) {
                                                if (p0.hasChildren()){
                                                    val user = p0.getValue(User::class.java)
                                                    val intent = Intent(this@MainActivity, ProfileActivity::class.java)
                                                    intent.putExtra("uid", uid)
                                                    intent.putExtra("displayName", user?.displayName)
                                                    startActivity(intent)
                                                } else {
                                                    Snackbar.make(main_view, "User not found", Snackbar.LENGTH_LONG).show()
                                                }
                                            }

                                        }
                                )
                            })

                            edit.setNegativeButton("Cancel", { dialog, which ->
                            })

                            edit.show()
                        }
                    }
                }
                builder.show()
            }
            R.id.main_menu_qr_code -> startQRcodeDialog(this)
            R.id.main_menu_notif -> startActivity(Intent(this, NotificationActivity::class.java))
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun startQRcodeDialog(context: Context) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.pp_dialog)
        dialog.setTitle("Title...")

        val image = dialog.findViewById<ImageView>(R.id.pp_dialog_image)
        database.addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError?) { }

                    override fun onDataChange(p0: DataSnapshot?) {
                        val user = p0?.getValue(User::class.java)

                        val decodedString = Base64.decode(user?.qrcode?.split(',')?.get(1), Base64.DEFAULT)
                        val decodedByte   = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        image.setImageBitmap(decodedByte)
                    }
                }
        )

        dialog.show()
    }

    override fun onStart() {
        super.onStart()

    }

    override fun onDestroy() {
        super.onDestroy()
        mAdapter?.cleanup()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            PERMISSIONS_REQUEST_CAMERA -> {
                if (grantResults.size > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    startActivityForResult(Intent(this, AddFriendActivity::class.java), SCAN_REQUEST_CODE)

                } else {

                    Snackbar.make(main_view, "You won't be able to add a friend unless accept it!", Snackbar.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SCAN_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            if (data != null) {
                val barcode = data.getParcelableExtra<Barcode>("barcode")
                var value: List<String>
                try {
                    value = barcode.rawValue.split('~')

                    val intent = Intent(this@MainActivity, ProfileActivity::class.java)
                    intent.putExtra("uid", value[0])
                    intent.putExtra("displayName", value[1])
                    startActivity(intent)

                } catch (e: Exception){
                    Snackbar.make(main_view, "Invalid QR code", Snackbar.LENGTH_LONG).show()
                }





            }
        }
    }
}
