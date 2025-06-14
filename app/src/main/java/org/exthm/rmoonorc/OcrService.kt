package org.exthm.rmoonorc.service

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import org.exthm.rmoonorc.OcrResultActivity
import org.exthm.rmoonorc.R
import org.exthm.rmoonorc.ScanAnimationActivity
import org.exthm.rmoonorc.utils.Constants

class OcrService : Service() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private lateinit var handler: Handler

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        handler = Handler(Looper.getMainLooper())
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Constants.ACTION_START_SERVICE) {
            val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
            val data = intent.getParcelableExtra<Intent>("data")

            if (resultCode == Activity.RESULT_OK && data != null) {
                startForegroundService()
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
                mediaProjection?.registerCallback(mediaProjectionCallback, handler)
                handler.postDelayed({ startScreenCapture() }, 100)
            } else {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("WrongConstant")
    private fun startScreenCapture() {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val density = displayMetrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()
                stopCaptureResources()
                
                showWiredChargingRippleScanAnimation()
                
                handler.postDelayed({
                    recognizeTextFromBitmap(bitmap)
                }, 500)
            }
        }, handler)
    }

    private fun showWiredChargingRippleScanAnimation() {
        val animationIntent = Intent(this, ScanAnimationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(animationIntent)
    }

    private fun recognizeTextFromBitmap(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val resultIntent = Intent(this, OcrResultActivity::class.java).apply {
                    putExtra("ocr_text", visionText.text)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(resultIntent)
                stopSelf()
            }
            .addOnFailureListener { e ->
                handler.post {
                    Toast.makeText(this, "OCR 识别失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
                stopSelf()
            }
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("OCR 服务运行中")
            .setContentText("正在截取屏幕并识别文字...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
        startForeground(Constants.NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            "OCR Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun stopCaptureResources() {
        virtualDisplay?.release()
        imageReader?.close()
        virtualDisplay = null
        imageReader = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCaptureResources()
        mediaProjection?.let {
            it.unregisterCallback(mediaProjectionCallback)
            it.stop()
        }
        mediaProjection = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}