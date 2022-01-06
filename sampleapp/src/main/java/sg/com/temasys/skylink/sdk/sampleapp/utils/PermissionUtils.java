package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.Deque;

import sg.com.temasys.skylink.sdk.rtc.SkylinkInfo;
import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.PermissionService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is used to process the runtime permission required by media usage
 */

public class PermissionUtils {

    private static final String TAG = PermissionUtils.class.getName();

    // request code for permission for file browser from file transfer in the app
    // this permission is not from the SDK
    public static final int APP_PERMISSIONS_READ_EXTERNAL_STORAGE = 1031;

    public static final int REQUEST_BUTTON_OVERLAY_CODE = 1032;

    public static final int MEDIA_PROJECTION_REQUEST_CODE = 1002;


    // Queue of Permission requesting objects.
    //these variables need to be static for all type call and configuration change
    private static Deque<PermRequester> permQ = new ArrayDeque<>();

    /**
     * Captures state of whether an Android Runtime permission is currently being processed.
     * Processing is complete when onRequestPermissionsResult is called, or if
     */
    private static Boolean permProcessing = new Boolean(false);
    /**
     * The currently processing Permission request.
     * Used for resuming after device rotation.
     */
    private static PermRequester permRequester = null;

    private boolean sendOverlayAlready = false;

    public PermissionUtils() {
    }

    /**
     * Handles the Android provided result(s) of requested Android permission(s).
     * Sends to Skylink SDK for processing.
     * If the permission requests did not originate from the SDK, the App should process them.
     *
     * @param requestCode  As given in Android method.
     * @param permissions  As given in Android method.
     * @param grantResults As given in Android method.
     * @param tag          Tag string for logging.
     * @return wasAppRequest variable to check the permission is from SDK or from the app
     */
    /**
     * Handles the Android provided result(s) of requested Activity for result calls.
     * Sends to Skylink SDK for processing.
     * If the requests did not originate from the SDK, the App should process them.
     *
     * @param requestCode As given in Android method.
     * @param resultCode  As given in Android method.
     * @param data        As given in Android method.
     */
    public void onRequestActivityResultHandler(int requestCode, int resultCode, Intent data) {

        String logTag = "[SA][onActResult][requestCode:" + requestCode + "] ";
        String log = logTag + "Was ";

        boolean permGranted = (Activity.RESULT_OK == resultCode);
        if (permGranted) {
            log += "Granted.";
        } else {
            log += "Denied!";
        }

        log += " This Activity for result ";

        // Call SDK processPermissionsResult with the given parameters.
        boolean wasSkylinkRequest = PermissionService.processActivityResult(requestCode, resultCode, data);

        if (wasSkylinkRequest) {
            log += "originates from the Skylink SDK.";
        } else {
            log += "does NOT originate from the Skylink SDK.";
            // If result is false, process the results in the app
            // (permission request was not from SDK).
        }
        Log.d(TAG, log);
    }

    /**
     * Handles the Android provided result(s) of requested Android permission(s).
     * Sends to Skylink SDK for processing.
     * If the permission requests did not originate from the SDK, the App should process them.
     *
     * @param requestCode  As given in Android method.
     * @param permissions  As given in Android method.
     * @param grantResults As given in Android method.
     * @param tag          Tag string for logging.
     * @return wasAppRequest variable to check the permission is from SDK or from the app
     */
    public void onRequestPermissionsResultHandler(
            int requestCode, String[] permissions, int[] grantResults,
            String tag) {

        // Trigger for next permission to be processed.
        permQTaskCompleted();

        // Null check.
        String logTag = "[SA][onPermRes] ";
        String error = "";
        if (permissions.length < 1) {
            error = logTag + "Unable to process empty permissions array!";
        }
        if (grantResults.length < 1) {
            error = logTag + "Unable to process empty grantResults array!";
        }
        if (!"".equals(error)) {
            Log.e(tag, error);
        }

        String permission = permissions[0];
        int grantResult = grantResults[0];
        String log = logTag + "Received results for requestCode:" + requestCode +
                ", Permissions:" + permission + ", with results:" + grantResult + ", that ";
        // Call SDK processPermissionsResult with the given parameters.
        boolean wasSkylinkRequest = PermissionService.processPermissionsResult(requestCode, permissions, grantResults);

        if (wasSkylinkRequest) {
            log += "originates from the Skylink SDK.";
            Log.d(tag, log);
        } else {
            log += "does NOT originate from the Skylink SDK.";
            Log.d(tag, log);
            // If result is false, process the results in the app
            // (permission request was not from SDK).
        }
    }

    /**
     * Handles the Skylink SDK OsListener callback onIntentRequired.
     * Use the intent and requestCode provided to call
     * {@link android.app.Activity#startActivityForResult(Intent, int)}.
     * Once the corresponding {@link android.app.Activity#onActivityResult(int, int, Intent)}
     * is received, pass the parameters (requestCode, resultCode, Intent) to the SDK's
     * {@link SkylinkConnection#processActivityResult(int, int, Intent)}.
     *  @param intent          As that in {@link sg.com.temasys.skylink.sdk.listener.OsListener#onIntentRequired}.
     * @param requestCode     As that in {@link sg.com.temasys.skylink.sdk.listener.OsListener#onIntentRequired}.
     * @param skylinkInfo     As that in {@link sg.com.temasys.skylink.sdk.listener.OsListener#onIntentRequired}.
     * @param currentActivity The current {@link Activity}.
     */
    public void onIntentRequiredHandler(Intent intent, int requestCode, SkylinkInfo skylinkInfo, Activity currentActivity) {
        String logTag = "[SPmS][getScreenCaptureIntent] ";
        currentActivity.startActivityForResult(intent, requestCode);
        String log = logTag + "Started Activity for result, " + skylinkInfo;
        Log.d("PermissionUtils", log);
    }

    /**
     * Handles the Skylink SDK OsListener callback onPermissionRequired.
     * If permission required have already been granted,
     * directly call the SDK's processPermissionsResult with PackageManager.PERMISSION_GRANTED.
     * If not, ask user for the required permission.
     * If user had denied required permission before, but did not indicate to never ask again,
     * provide a dialog to inform user why such permissions are required,
     * and provide the chance to set the required permissions again.
     *
     * @param permRequesterInfo As given in OsListener method.
     * @param tag               Tag string for logging.
     * @param context           Current context.
     * @param fragment          Current fragment.
     */
    public void onPermissionRequiredHandler(PermRequesterInfo permRequesterInfo,
                                            final String tag, final Context context, final Fragment fragment) {

        // Create a new PermRequesterInfo to represent this request.
        PermRequester permRequester = new PermRequester(permRequesterInfo,
                tag, context, fragment);

        // Add PermRequesterInfo to Queue.
        permQOfferLast(permRequester);
    }

    /**
     * Handles Skylink SDK OsListener callback onPermissionGranted.
     * Log the permission that had been granted.
     * @param requestCode
     * @param skylinkInfo
     * @param granted
     */
    public static void onPermissionGrantedHandler(int requestCode, SkylinkInfo skylinkInfo, boolean granted) {
        String outcome = "GRANTED";
        if (!granted) {
            outcome = "DENIED";
        }
        String log = "[SA][onPermGrant] Permission has been " + outcome + " for requestCode " +
                requestCode + ", " + skylinkInfo;
        Log.d("PermissionUtils", log);
    }

    /**
     * Handles Skylink SDK OsListener callback onPermissionGranted.
     * Log the permission that had been granted.
     *
     * @param info As given in OsListener method.
     */
    public static void onPermissionGrantedHandler(PermRequesterInfo info) {
        String log = "[SA][onPermGrant] Permission has been GRANTED for " + info.getPermissions()[0]
                + ", " + info.getSkylinkInfo();
        Log.d("PermissionUtils", log);
    }

    /**
     * @param info    As given in OsListener method.
     * @param context Current context to show AlertDialog.
     */
    public static void onPermissionDeniedHandler(PermRequesterInfo info, Context context) {
        // Create alert to inform user about the permission denied and resultant feature disabled.
        // Log the same.
        // Check if should explain reason for requesting permission, which happens if the user
        // has denied this Permission before, but did not indicate to never ask again.
        String alertText = "";
        switch (info.getSkylinkInfo()) {
            case PERM_AUDIO_MIC:
                alertText += "Android permission to use the Microphone was denied. " +
                        "We are now NOT able to send our audio to a remote Peer!";
                break;
            case PERM_VIDEO_CAM:
                alertText += "Android permission to use the Camera was denied. " +
                        "We are now NOT able to send our video to a remote Peer!";
                break;
            case PERM_STORAGE_READ:
                alertText += "Android permission to read from device storage was denied. " +
                        "We are now NOT able to send file to a remote Peer!";
                break;
            case PERM_STORAGE_WRITE:
                alertText += "Android permission to write to device storage was denied. " +
                        "We are now NOT able to receive file from a remote Peer!";
                break;
        }
        alertText += "\r\nTo enable feature, restart this feature and grant the permission(s) " +
                "required. Alternatively, go to Android's Settings -> \"Apps\", select this App, " +
                "go to \"Permissions\", grant required permission(s), and restart this feature.";

        // Create AlertDialog to warn user of consequences of permission denied.
        AlertDialog.Builder permissionDeniedDialogBuilder =
                new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom));
        permissionDeniedDialogBuilder.setTitle("Warning! Feature(s) unavailable " +
                "due to Permission(s) denied.");

        // Create TextView for permission alert.
        final TextView msgTxtView = new TextView(context);
        msgTxtView.setText(alertText);
        msgTxtView.setMovementMethod(LinkMovementMethod.getInstance());
        permissionDeniedDialogBuilder.setView(msgTxtView);
        permissionDeniedDialogBuilder.setPositiveButton("Ok", null);

        alertText = "[SA][onPermDenied] " + alertText;
        Log.d("PermissionUtils", alertText);
        permissionDeniedDialogBuilder.show();
    }

    /**
     * Adds new permission request to the head of the queue.
     * This is for repeating a particular request immediately.
     * Synchronised with permProcessing.
     *
     * @param permRequester
     */
    public void permQOfferFirst(PermRequester permRequester) {
        synchronized (permProcessing) {
            permQ.offerFirst(permRequester);

            // Try to process permRun immediately.
            permProcessing = false;
            permRequester = null;

            String log = "[SA][permQOfferFirst] Added permission request to head of permQ. " +
                    "Set permProcessing to false and try to process queue immediately.";
            Log.d(TAG, log);
            permQPoll();
        }
    }

    /**
     * Adds new permission requests to the tail of the queue.
     * This is for normal adding of new permission requests to the queue.
     * Synchronised with permProcessing.
     *
     * @param permRequester
     */
    public void permQOfferLast(PermRequester permRequester) {
        synchronized (permProcessing) {
            permQ.offerLast(permRequester);
            String log = "[SA][permQOfferLast] Added permission request to tail of permQ. " +
                    "Triggered for queue processing.";
            Log.d(TAG, log);
            permQPoll();
        }
    }

    /**
     * Process the element at the head of the queue if not already currently processing.
     * Synchronised with permProcessing.
     *
     * @return True if a new permission request is triggered to process, false otherwise.
     */
    public boolean permQPoll() {
        synchronized (permProcessing) {
            String log = "[SA][permQPoll] ";
            if (permProcessing) {
                log += "Tried to process permQ but not starting new attempt as " +
                        "it is currently being processed.";
                Log.d(TAG, log);
                return false;
            }

            // Try to process next permission request since Q is not currently being processed.
            PermRequester permRequesterNext = permQ.poll();
            // Do not process if there are no more element in the queue.
            if (permRequesterNext == null) {
                log += "Tried to process permQ but not starting new attempt as " +
                        "there are no more permissions waiting to be processed.";
                Log.d(TAG, log);
                return false;
            }

            // Set states and process permission request.
            permProcessing = true;
            permRequester = permRequesterNext;
            log += "Processing next permission request in permQ. Permission states updated.";
            Log.d(TAG, log);
            permRequesterNext.processOnPermReq();
            return true;
        }
    }

    /**
     * Reset permQ tasks and states, typically at the start of a sample fragment.
     * This is required when for e.g.:
     * Restarting a sample after disruption by "Screen Overlay detected" permission setting error.
     * Synchronised with permProcessing.
     */
    public void permQReset() {
        synchronized (permProcessing) {

            permProcessing = false;
            permRequester = null;
            permQ = new ArrayDeque<>();

            String log = "[SA][permQReset] Reset all permQ related tasks and states! " +
                    "permQ is now empty.";
            Log.d(TAG, log);
        }
    }

    /**
     * Resume previously running permQ task (if any) after rotation.
     * Previous caller related parameters will be replaced by current caller.
     * Synchronised with permProcessing.
     *
     * @param context
     * @param fragment
     */
    public void permQResume(Context context, Fragment fragment) {
        synchronized (permProcessing) {
            if (permRequester == null) {
                return;
            }

            String log = "[SA][permQResume] Resuming permission request that was disrupted!";
            Log.d(TAG, log);

            // Set new caller for all PermRequesters, including those in Q.
            permRequester.setNewCallerInfo(context, fragment);
            permQOfferFirst(permRequester);
        }
    }

    /**
     * Call on completion of a permQ task.
     * Will trigger permQ to process next task.
     * Synchronised with permProcessing.
     */
    public void permQTaskCompleted() {
        synchronized (permProcessing) {
            // Set state to not processing.
            permProcessing = false;
            permRequester = null;

            String log = "[SA][permQTaskCom] Completed permission request. " +
                    "Set permProcessing to false and try to process next task in queue.";
            Log.d(TAG, log);
            permQPoll();
        }
    }

    /**
     * Stores parameters required for making permission request
     * and have methods to perform permission request.
     */
    public class PermRequester {

        private PermRequesterInfo requester;

        String tag;
        // Static elements that are common to all PermRequesterInfo
        Context context;
        Fragment fragment;

        public PermRequester(PermRequesterInfo requester, String tag,
                             Context context, Fragment fragment) {
            this.requester = requester;
            this.tag = tag;
            this.context = context;
            this.fragment = fragment;
        }

        /**
         * Sets new Permission request parameters specific to caller.
         * This could happen when a new calling fragment wishes to execute a previously requested
         * Permission, for e.g. after a screen rotation where a new fragment continues the
         * permission request of the previous fragment.
         *
         * @param context
         * @param fragment
         */
        void setNewCallerInfo(Context context, Fragment fragment) {
            context = context;
            fragment = fragment;
        }

        /**
         * Do the actual work of processing onPermissionRequired using class members as parameters.
         */
        void processOnPermReq() {
            processOnPermReq(requester, tag,
                    context, fragment);
        }

        /**
         * Do the actual work of processing onPermissionRequired using supplied parameters.
         *
         * @param requester
         * @param tag
         * @param context
         * @param fragment
         * @return
         */
        void processOnPermReq(final PermRequesterInfo requester, final String tag,
                              final Context context, final Fragment fragment) {
            int requestCode = requester.getRequestCode();
            SkylinkInfo skylinkInfo = requester.getSkylinkInfo();
            String[] permissions = requester.getPermissions();
            String permission = permissions[0];
            String log = "[SA][PR][procPermReq] SDK requesting permission for " + permission +
                    ", which ";

            // For permission already granted,
            // call Skylink SDK processPermissionsResult with PERMISSION_GRANTED as the result.
            int permissionState = ContextCompat.checkSelfPermission(context, permission);
            if (permissionState == PackageManager.PERMISSION_GRANTED) {
                log += "has already been granted, no need to request again. " + skylinkInfo;

                int[] grantResults = new int[]{PackageManager.PERMISSION_GRANTED};
                if (!PermissionService.processPermissionsResult(
                        requestCode, permissions, grantResults)) {
                    // If result is false, an error has occurred.
                    log += "\r\n[ERROR] The SDK should but does not recognise " +
                            "permission requestCode: " + requestCode + "!";
                    Log.e(tag, log);
                } else {
                    Log.d(tag, log);
                }

                // Trigger for next permission to be processed.
                permQTaskCompleted();
                return;

            } else {
                log += "has not been granted. ";
            }

            // Create explanation based on permission required.
            String alertText = "";
            switch (skylinkInfo) {
                case PERM_AUDIO_MIC:
                    alertText += "Android permission to use the Microphone must be given " +
                            "in order to send our audio to a remote Peer!";
                    break;
                case PERM_VIDEO_CAM:
                    alertText += "Android permission to use the Camera must be given " +
                            "in order to send our video to a remote Peer!";
                    break;
                case PERM_STORAGE_READ:
                    alertText += "Android permission to read from device storage must be given " +
                            "in order to send file to a remote Peer!";
                    break;
                case PERM_STORAGE_WRITE:
                    alertText += "Android permission to write to device storage must be given " +
                            "in order to receive file from a remote Peer!";
                    break;
            }
            log += alertText + "\r\n" + skylinkInfo;

            // Explain rationale for permission request if the user
            // has denied this Permission before, but did not indicate to never ask again.
            if (fragment.shouldShowRequestPermissionRationale(permission)) {

                // Create AlertDialog to present Permission rationale message.
                AlertDialog.Builder permissionRationaleDialogBuilder =
                        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom));
                permissionRationaleDialogBuilder.setTitle("Why this permission is requested...");

                // Create TextView for permission rationale alert.
                final TextView msgTxtView = new TextView(context);
                msgTxtView.setText(alertText);
                msgTxtView.setMovementMethod(LinkMovementMethod.getInstance());
                permissionRationaleDialogBuilder.setView(msgTxtView);

                // Indicates if permission request had been made (regardless of request outcome).
                final boolean[] requestMade = {false};

                // User denies permission even after providing rationale.
                final String finalLogDeny = log + "\r\n" +
                        "Not requesting Android for Permission after current & previous denial.";
                permissionRationaleDialogBuilder.setNegativeButton("Cancel",
                        (dialog, which) -> {
                            requestMade[0] = true;
                            String logDeny = finalLogDeny;

                            // Call Skylink SDK processPermissionsResult with
                            // PERMISSION_DENIED as the result.
                            int[] grantResults = new int[]{PackageManager.PERMISSION_DENIED};
                            if (!PermissionService.processPermissionsResult(
                                    requestCode, permissions, grantResults)) {
                                // If result is false, an error has occurred.
                                logDeny += "\r\n[ERROR] The SDK should but does not recognise "
                                        + "permission requestCode: " + requestCode + "!";
                                Log.e(tag, logDeny);
                            } else {
                                // Log permission denied.
                                Log.d(tag, logDeny);
                            }

                            // Trigger for next permission to be processed.
                            permQTaskCompleted();
                        });

                // User agrees to grant permission after rationale was shown.
                final String finalLogRequest = log +
                        "\r\nRequesting Android for Permission after previous denial.";
                permissionRationaleDialogBuilder.setPositiveButton("Grant Permission",
                        (dialog, which) -> {
                            requestMade[0] = true;
                            // Request for permission:
                            Log.d(tag, finalLogRequest);
                            fragment.requestPermissions(permissions, requestCode);
                        });

                // Set this permission in Q again if it was canceled without being requested.
                final String finalLogCancel = log + "\r\npermissionRationaleDialog canceled ";
                permissionRationaleDialogBuilder.setOnCancelListener(
                        dialog -> {
                            // If request had not been made, add permission Runnable again to
                            // Head of Queue, to be processed again.
                            if (!requestMade[0]) {
                                String logCancel = finalLogCancel + "without having made "
                                        + "request! Will make request again.";
                                Log.d(tag, logCancel);
                                PermRequester permRequester =
                                        new PermRequester(requester,
                                                tag, context, fragment);
                                permQOfferFirst(permRequester);
                                return;
                            }

                            // Otherwise, it was from elsewhere where
                            // the permission request was already made.
                            String logCancel = finalLogCancel + "after having made request, "
                                    + "hence will not make request again.";
                            Log.d(tag, logCancel);
                        });

                // Show explanation.
                permissionRationaleDialogBuilder.show();
                return;
            }

            // Request for permission for the first time:
            log += ".\r\nRequesting Android for Permission for the first time.";
            Log.d(tag, log);

            fragment.requestPermissions(permissions, requestCode);
            return;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // These methods to process the permission from the app
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * display the access file permission dialog for the user
     *
     * @param context
     * @param fragmentInstance the view instance
     * @return true if the permission already grant before
     */
    public static boolean requestFilePermission(Context context, Fragment fragmentInstance) {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            fragmentInstance.requestPermissions(
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    APP_PERMISSIONS_READ_EXTERNAL_STORAGE);

            return false;
        }
        return true;
    }

    public static void displayFilePermissionWarning(Context context) {
        String alertText = "Android permission to read from device storage was denied. " +
                "We are now NOT able to browse for file in device";

        alertText += "\r\nTo enable feature, restart this feature and grant the permission(s) " +
                "required. Alternatively, go to Android's Settings -> \"Apps\", select this App, " +
                "go to \"Permissions\", grant required permission(s), and restart this feature.";

        // Create AlertDialog to warn user of consequences of permission denied.
        AlertDialog.Builder permissionDeniedDialogBuilder =
                new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom));
        permissionDeniedDialogBuilder.setTitle("Warning! Feature(s) unavailable " +
                "due to Permission(s) denied.");

        // Create TextView for permission alert.
        final TextView msgTxtView = new TextView(context);
        msgTxtView.setText(alertText);
        msgTxtView.setMovementMethod(LinkMovementMethod.getInstance());
        permissionDeniedDialogBuilder.setView(msgTxtView);
        permissionDeniedDialogBuilder.setPositiveButton("Ok", null);

        alertText = "[SA][onPermDenied] " + alertText;
        Log.d("PermissionUtils", alertText);
        permissionDeniedDialogBuilder.show();
    }

    /**
     * display the activity for grant the overlay button permission
     *
     * @param context
     * @param fragmentInstance the view instance
     * @return true if the permission already grant before
     */
    public boolean requestButtonOverlayPermission(Context context, Fragment fragmentInstance) {
        /** check if we already  have permission to draw over other apps */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                /** if not construct intent to request permission */
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                /** request permission via start activity for result */
                fragmentInstance.startActivityForResult(intent, PermissionUtils.REQUEST_BUTTON_OVERLAY_CODE);
                sendOverlayAlready = true;
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean isSendOverlayAlready() {
        return sendOverlayAlready;
    }

    /**
     * display the dialog to warn about permission deny for overlay button
     *
     * @param context
     */
    public void displayOverlayButtonPermissionWarning(Context context) {
        String alertText = "\r\nAndroid permission to allow overlay button is denied. " +
                "We are now NOT able to show the \"Stop Screen Share\" button for stopping screen sharing at anytime";

        alertText += "\r\n\nTo enable feature, restart this feature and grant the permission " +
                "required. Alternatively, go to Android's Settings -> \"Appear on top/Display over other apps\", select "
                + context.getResources().getString(R.string.app_name) + ", " +
                "grant the required permission, and restart this feature.";

        // Create AlertDialog to warn user of consequences of permission denied.
        AlertDialog.Builder permissionDeniedDialogBuilder =
                new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom));
        permissionDeniedDialogBuilder.setTitle("Warning! Feature unavailable " +
                "due to Permission denied.");


        // Create TextView for permission alert.
        final TextView msgTxtView = new TextView(context);
        msgTxtView.setText(alertText);
        msgTxtView.setMovementMethod(LinkMovementMethod.getInstance());
        permissionDeniedDialogBuilder.setView(msgTxtView);
        permissionDeniedDialogBuilder.setPositiveButton("Ok", null);

        alertText = "[SA][onPermDenied] " + alertText;
        Log.d("PermissionUtils", alertText);
        permissionDeniedDialogBuilder.show();
    }

    /**
     * display the dialog to warn about permission deny for capturing screen
     *
     * @param context
     */
    public void displayScreenCapturePermissionWarning(Context context) {
        String alertText = "\r\nAndroid permission to allow capturing screen is denied. " +
                "We are now NOT able to capture the device screen.";

        alertText += "\r\n\nTo enable feature, restart this feature and grant the permission " +
                "required. ";

        // Create AlertDialog to warn user of consequences of permission denied.
        AlertDialog.Builder permissionDeniedDialogBuilder =
                new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogCustom));
        permissionDeniedDialogBuilder.setTitle("Warning! Feature unavailable " +
                "due to Permission denied.");


        // Create TextView for permission alert.
        final TextView msgTxtView = new TextView(context);
        msgTxtView.setText(alertText);
        msgTxtView.setMovementMethod(LinkMovementMethod.getInstance());
        permissionDeniedDialogBuilder.setView(msgTxtView);
        permissionDeniedDialogBuilder.setPositiveButton("Ok", null);

        alertText = "[SA][onPermDenied] " + alertText;
        Log.d("PermissionUtils", alertText);
        permissionDeniedDialogBuilder.show();
    }
}
