package sg.com.temasys.skylink.sdk.sampleapp.data.service;

import android.content.Context;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class PermissionService {

    public static boolean processPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //
        SkylinkConnection skylinkConnection = SDKService.getCurrentSkylinkConnection();

        if(skylinkConnection != null){
            return skylinkConnection.processPermissionsResult(requestCode, permissions, grantResults);
        }

        return false;
    }
}
