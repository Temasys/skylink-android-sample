package sg.com.temasys.skylink.sdk.sampleapp.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.Deque;

import sg.com.temasys.skylink.sdk.sampleapp.service.PermissionService;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.PermRequesterInfo;

import static sg.com.temasys.skylink.sdk.rtc.Info.PERM_AUDIO_MIC;
import static sg.com.temasys.skylink.sdk.rtc.Info.PERM_STORAGE_READ;
import static sg.com.temasys.skylink.sdk.rtc.Info.PERM_STORAGE_WRITE;
import static sg.com.temasys.skylink.sdk.rtc.Info.PERM_VIDEO_CAM;
import static sg.com.temasys.skylink.sdk.rtc.Info.getInfoString;

/**
 * Created by muoi.pham on 20/07/18.
 * This class is used to process the runtime permission required by media usage
 */

public class PermissionUtils {

    private static final String TAG = PermissionUtils.class.getName();

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
     */
    public void onRequestPermissionsResultHandler(
            int requestCode, String[] permissions, int[] grantResults,
            String tag) {

        // Trigger for next permission to be processed.
        permQTaskCompleted();

        // Null check.
        String error = "";
        if (permissions.length < 1) {
            error = "[SA][onPermRes] Unable to process empty permissions array!";
        }
        if (grantResults.length < 1) {
            error = "[SA][onPermRes] Unable to process empty grantResults array!";
        }
        if (!"".equals(error)) {
            Log.e(tag, error);
            return;
        }

        String permission = permissions[0];
        int grantResult = grantResults[0];
        String log = "[SA][onPermRes] Received results for requestCode:" + requestCode +
                ", Permissions:" + permission + ", with results:" + grantResult + ", that ";
        // Call SDK processPermissionsResult with the given parameters.
        boolean wasSkylinkRequest = PermissionService.processPermissionsResult(requestCode, permissions, grantResults);

        if (wasSkylinkRequest) {
            log += "originates ";
        } else {
            log += "does NOT originate ";
            // If result is false, process the results in the app
            // (permission request was not from SDK).
        }
        log += "from the Skylink SDK.";
        Log.d(tag, log);
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
     *
     * @param info As given in OsListener method.
     */
    public static void onPermissionGrantedHandler(PermRequesterInfo info) {
        String log = "[SA][onPermGrant] Permission has been GRANTED for " + info.getPermissions()[0] +
                ", infoCode:" + info.getInfoCode() + " (" + getInfoString(info.getInfoCode()) + ").";
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
        switch (info.getInfoCode()) {
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
                new AlertDialog.Builder(context);
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
            String log = "[SA][PR][procPermReq] SDK requesting permission for " + requester.getPermissions()[0] +
                    ", which ";

            // For permission already granted,
            // call Sylink SDK processPermissionsResult with PERMISSION_GRANTED as the result.
            int permissionState = ContextCompat.checkSelfPermission(context, requester.getPermissions()[0]);
            if (permissionState == PackageManager.PERMISSION_GRANTED) {

                log += "has already been granted, no need to request again. " +
                        "infoCode:" + requester.getInfoCode() + " (" + getInfoString(requester.getInfoCode()) + ")";

                int[] grantResults = new int[]{PackageManager.PERMISSION_GRANTED};
                if (!PermissionService.processPermissionsResult(requester.getRequestCode(), requester.getPermissions(),
                        grantResults)) {
                    // If result is false, an error has occurred.
                    log += "\r\n[ERROR] The SDK should but does not recognise permission requestCode: "
                            + " " + requester.getRequestCode() + "!";
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
            switch (requester.getInfoCode()) {
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
            log += alertText + "\r\ninfoCode:" + requester.getInfoCode() + " (" + getInfoString(requester.getInfoCode()) + ")";

            // Explain rationale for permission request if the user
            // has denied this Permission before, but did not indicate to never ask again.
            if (fragment.shouldShowRequestPermissionRationale(requester.getPermissions()[0])) {

                // Create AlertDialog to present Permission rationale message.
                AlertDialog.Builder permissionRationaleDialogBuilder =
                        new AlertDialog.Builder(context);
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

                            // Call Sylink SDK processPermissionsResult with
                            // PERMISSION_DENIED as the result.
                            int[] grantResults = new int[]{PackageManager.PERMISSION_DENIED};
                            if (!PermissionService.processPermissionsResult(requester.getRequestCode(), requester.getPermissions(),
                                    grantResults)) {
                                // If result is false, an error has occurred.
                                logDeny += "\r\n[ERROR] The SDK should but does not recognise "
                                        + "permission requestCode: " + requester.getRequestCode() + "!";
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
                            fragment.requestPermissions(requester.getPermissions(), requester.getRequestCode());
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

            fragment.requestPermissions(requester.getPermissions(), requester.getRequestCode());
            return;
        }
    }
}
