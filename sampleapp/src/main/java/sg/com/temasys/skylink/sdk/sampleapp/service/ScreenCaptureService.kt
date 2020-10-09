package sg.com.temasys.skylink.sdk.sampleapp.service;


import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.VideoCapturer


class ScreenCaptureService : Service() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null

    private var screenVideoCapturer: VideoCapturer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(
                SERVICE_ID,
                NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).build()
        )

        // use applicationContext to avoid memory leak on Android 10.
        // see: https://partnerissuetracker.corp.google.com/issues/139732252
        mediaProjectionManager =
                applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Audio Capture Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent != null) {
            when (intent.action) {
                ACTION_START -> {
                    mediaProjection =
                            mediaProjectionManager.getMediaProjection(
                                    Activity.RESULT_OK,
                                    intent.getParcelableExtra(EXTRA_RESULT_DATA)!!
                            ) as MediaProjection
                    startScreenCapture()
                    Service.START_STICKY
                }
                ACTION_STOP -> {
                    stopScreenCapture()
                    Service.START_NOT_STICKY
                }
                else -> throw IllegalArgumentException("Unexpected action received: ${intent.action}")
            }
        } else {
            Service.START_NOT_STICKY
        }
    }

    private fun startScreenCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val intent = mediaProjectionManager.createScreenCaptureIntent()

            screenVideoCapturer = createScreenVideoCapturer(intent)

            // how to pass screenCapturer into videoService ???

//            videoService.createLocalCustomVideo(screenCapturer)


        } else {
            TODO("VERSION.SDK_INT < Q")
        }
    }

    /**
     * Create and return a [ScreenCapturerAndroid].
     */
    private fun createScreenVideoCapturer(mediaProjectionPermissionResultIntent: Intent): VideoCapturer? {
        return ScreenCapturerAndroid(mediaProjectionPermissionResultIntent, object : MediaProjection.Callback() {
            override fun onStop() {
                val log = "[ScreenCapturerAndroid][onStop] Screen capturing has been stopped."
            }
        })
    }

    private fun stopScreenCapture() {
//        requireNotNull(mediaProjection) { "Tried to stop audio capture, but there was no ongoing capture in place!" }
//
//        audioCaptureThread.interrupt()
//        audioCaptureThread.join()
//
//        audioRecord!!.stop()
//        audioRecord!!.release()
//        audioRecord = null
//
//        mediaProjection!!.stop()
//        stopSelf()
    }



    companion object {
        private const val SERVICE_ID = 123
        private const val NOTIFICATION_CHANNEL_ID = "ScreenCapture channel"

        private const val NUM_SAMPLES_PER_READ = 1024
        private const val BYTES_PER_SAMPLE = 2 // 2 bytes since we hardcoded the PCM 16-bit format
        private const val BUFFER_SIZE_IN_BYTES = NUM_SAMPLES_PER_READ * BYTES_PER_SAMPLE

        const val ACTION_START = "ScreenCaptureService:Start"
        const val ACTION_STOP = "ScreenCaptureService:Stop"
        const val EXTRA_RESULT_DATA = "ScreenCaptureService:Extra:ResultData"
    }


}
