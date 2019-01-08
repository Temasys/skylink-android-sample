package sg.com.temasys.skylink.sdk.sampleapp.service;

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
}
