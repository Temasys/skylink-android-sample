package sg.com.temasys.skylink.sdk.sampleapp.data.service;

import android.content.Context;

import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class PermissionService {

    private Context mContext;
    private SkylinkConnection skylinkConnection;
    private SdkConnectionManager sdkConnectionManager;

    public PermissionService(Context context){
        this.mContext = context;
        sdkConnectionManager = new SdkConnectionManager(mContext);
    }

    public boolean processPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        skylinkConnection = sdkConnectionManager.getCurrentSkylinkConnection();

        if(skylinkConnection != null){
            return skylinkConnection.processPermissionsResult(requestCode, permissions, grantResults);
        }

        return false;
    }
}
