package com.morhpt.c

import android.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.morhpt.c.help.DownloadImageTask
import kotlinx.android.synthetic.main.activity_full_image.*
import java.io.IOException
import java.net.URL


class FullImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_image)

        val iin = intent
        val b = iin.extras

        if (b != null) {
            val url = b.get("url") as String

            Glide.with(this@FullImageActivity)
                    .load(url)
                    .into(full_image)

            var expend = false

            full_image_main.setOnClickListener {
                finish()
            }

            full_image_main.setOnLongClickListener {
                try {
                    val options = arrayOf("Save Image", "Share Image")
                    val builder = AlertDialog.Builder(this@FullImageActivity)
                    builder.setTitle("Choose one")
                    builder.setItems(options) { dialog, which ->
                        when (which) {
                            0 -> DownloadImageTask(false, this@FullImageActivity).execute(url)
                            1 -> DownloadImageTask(true, this@FullImageActivity).execute(url)
                        }
                    }
                    builder.show()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                true
            }
        } else finish()


    }
}
