package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.VideoCapturer;

import java.util.HashMap;

import sg.com.temasys.skylink.sdk.rtc.SkylinkCallback;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConfig;
import sg.com.temasys.skylink.sdk.rtc.SkylinkError;
import sg.com.temasys.skylink.sdk.rtc.SkylinkEvent;

import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;

import androidx.core.app.NotificationCompat;

public class ScreenCaptureService extends Service {

    private static final int SERVICE_ID = 123;
    public static final String NOTIFICATION_CHANNEL_ID = "ScreenCapture channel";
    public static final String ACTION_START = "ScreenCaptureService:Start";
    public static final String ACTION_STOP = "ScreenCaptureService:Stop";
    public static final String EXTRA_RESULT_DATA = "ScreenCaptureService:Extra:ResultData";

    private MediaProjectionManager mediaProjectionManager = null;
    private MediaProjection mediaProjection = null;
    private VideoCapturer screenVideoCapturer = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(
                SERVICE_ID,
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).build()
        );

        mediaProjectionManager = (MediaProjectionManager) getApplicationContext()
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Screen Capture Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return Service.START_NOT_STICKY;
        }

        if (intent.getAction() == ACTION_START) {
            mediaProjection =
                    mediaProjectionManager.getMediaProjection(
                            Activity.RESULT_OK,
                            intent.getParcelableExtra(EXTRA_RESULT_DATA));

            mediaProjection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    super.onStop();
                    Log.d("ScreenCapturerService", "mediaProjection onStop");
                }
            }, null);

            startScreenCapture(intent.getParcelableExtra(EXTRA_RESULT_DATA));

            return Service.START_STICKY;
        } else if (intent.getAction() == ACTION_STOP) {
            stopScreenCapture();
            return Service.START_NOT_STICKY;
        } else {
            Log.e("ScreenCapturerService", "Unexpected action received " + intent.getAction());
        }

        return Service.START_NOT_STICKY;
    }

    private void startScreenCapture(Intent screenCaptureIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            screenVideoCapturer = new ScreenCapturerAndroid(screenCaptureIntent, new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    super.onStop();
                    Log.d("ScreenCapturerService", "screenVideoCapturer onStop");
                }
            });

            if (screenVideoCapturer != null) {
                SkylinkCommonService.getCurrentSkylinkConnection().createLocalMedia(SkylinkConfig.VideoDevice.SCREEN,
                        "SCREEN video from mobile",
                        screenVideoCapturer, 800, 1600, 60, new SkylinkCallback() {
                            @Override
                            public void onError(SkylinkError error, HashMap<String, Object> details) {
                                String contextDescription = (String) details.get(SkylinkEvent.CONTEXT_DESCRIPTION);
                                Log.e("SkylinkCallback", contextDescription);
                                toastLog("ScreenCapturerService", getApplicationContext(), "\"Unable to createLocal screen as " + contextDescription);
                            }
                        }
                );
            }
        }
    }

    private void stopScreenCapture() {
        if (mediaProjection != null) {
            mediaProjection.stop();
        }

        if (screenVideoCapturer != null) {
            screenVideoCapturer = null;
        }

        stopSelf();
    }
}
