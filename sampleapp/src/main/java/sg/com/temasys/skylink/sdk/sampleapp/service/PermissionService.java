package sg.com.temasys.skylink.sdk.sampleapp.service;

import android.content.Intent;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class PermissionService {

    /**
     * Process Android Runtime permission result received by App via Android callback
     * onRequestPermissionsResult.
     *
     * @param requestCode  The requestCode in the Android callback.
     * @param permissions  The permissions in the Android callback.
     * @param grantResults The grantResults in the Android callback.
     * @return True if requestCode is recognised by SDK.
     */
    public static boolean processPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //
        SkylinkConnection skylinkConnection = SkylinkCommonService.getCurrentSkylinkConnection();

        if (skylinkConnection != null) {
            return skylinkConnection.processPermissionsResult(requestCode, permissions, grantResults);
        }

        return false;
    }

    /**
     * Process Android onActivityResult received by App.
     *
     * @param requestCode The int requestCode in the Android callback.
     * @param resultCode  The int resultCode in the Android callback.
     * @param data        The Intent data in the Android callback.
     * @return
     */
    public static boolean processActivityResult(int requestCode, int resultCode, Intent data) {
        //
        SkylinkConnection skylinkConnection = SkylinkCommonService.getCurrentSkylinkConnection();

        if (skylinkConnection != null) {
            return skylinkConnection.processActivityResult(requestCode, resultCode, data);
        }

        return false;
    }
}
