package sg.com.temasys.skylink.sdk.sampleapp.service;


import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.VideoCapturer
import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig
import sg.com.temasys.skylink.sdk.rtc.SkylinkError
import sg.com.temasys.skylink.sdk.rtc.SkylinkEvent
import sg.com.temasys.skylink.sdk.sampleapp.service.SkylinkCommonService.Companion.currentSkylinkConnection
import java.util.*


class ScreenCaptureService : Service() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null

    private var mediaProjectionCallback: MediaProjectionCallback? = null

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

        // create ScreenService

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
//                    val screenCaptureIntent = intent.getParcelableExtra(EXTRA_RESULT_DATA)
                    mediaProjection =
                            mediaProjectionManager.getMediaProjection(
                                    Activity.RESULT_OK,
                                    intent.getParcelableExtra(EXTRA_RESULT_DATA)!!
                            ) as MediaProjection

//                    mediaProjectionManager.createScreenCaptureIntent()

                    mediaProjectionCallback = MediaProjectionCallback()

//                    var handler: Handler()

                    if(mediaProjectionCallback != null) {
                        mediaProjection?.registerCallback(mediaProjectionCallback!!, null)
                    }

                    startScreenCapture(intent.getParcelableExtra(EXTRA_RESULT_DATA)!!)
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

    private fun startScreenCapture(screenCaptureIntent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

//            val intent = mediaProjectionManager.createScreenCaptureIntent()

            screenVideoCapturer = createScreenVideoCapturer(screenCaptureIntent)

            if (screenVideoCapturer != null) {
                currentSkylinkConnection!!.createLocalMedia(SkylinkConfig.VideoDevice.SCREEN, "SCREEN video from mobile",
                        screenVideoCapturer, 800, 1600, 60, object : SkylinkCallback {
                    override fun onError(error: SkylinkError, details: HashMap<String, Any>) {
                        val contextDescription = details[SkylinkEvent.CONTEXT_DESCRIPTION] as String?
                        Log.e("SkylinkCallback", contextDescription)
//                        Utils.toastLog("Screen", context, "\"Unable to createLocalCustomVideo as $contextDescription")
                    }
                })
            }
        } else {
            TODO("VERSION.SDK_INT < Q")
        }
    }

    /**
     * Create and return a [ScreenCapturerAndroid].
     */
    private fun createScreenVideoCapturer(mediaProjectionPermissionResultIntent: Intent): VideoCapturer? {
        return ScreenCapturerAndroid(mediaProjectionPermissionResultIntent, MediaProjectionCallback())
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

    //Called when the MediaProjection session is no longer valid.
    private class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {}
    }


}
