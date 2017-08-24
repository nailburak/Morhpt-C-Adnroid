package com.morhpt.c

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast

import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector

import java.io.IOException

class AddFriendActivity : GodLikeActivity() {
    internal var cameraView: SurfaceView? = null
    internal var barcode: BarcodeDetector? = null
    internal var cameraSource: CameraSource? = null
    internal var holder: SurfaceHolder? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)
        cameraView = findViewById<SurfaceView>(R.id.add_friend_camera_view)
        cameraView?.setZOrderMediaOverlay(true)
        holder = cameraView?.holder
        barcode = BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build()
        if (!barcode!!.isOperational) {
            Toast.makeText(applicationContext, "Sorry, Couldn't setup the detector", Toast.LENGTH_LONG).show()
            this.finish()
        }
        cameraSource = CameraSource.Builder(this, barcode)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(24f)
                .setAutoFocusEnabled(true)
                .setRequestedPreviewSize(1920, 1024)
                .build()
        cameraView?.holder?.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource?.start(cameraView?.holder)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {

            }
        })
        barcode?.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {

            }

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                val barcodes = detections.detectedItems
                if (barcodes.size() > 0) {
                    val intent = Intent()
                    intent.putExtra("barcode", barcodes.valueAt(0))
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
        })
    }
}