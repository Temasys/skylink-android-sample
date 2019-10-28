package sg.com.temasys.skylink.sdk.sampleapp.service.model;

import sg.com.temasys.skylink.sdk.rtc.SkylinkInfo;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class PermRequesterInfo {
    private String[] permissions;
    private int requestCode;

    private SkylinkInfo skylinkInfo;

    public PermRequesterInfo() {
    }

    public PermRequesterInfo(String[] permissions, int requestCode, SkylinkInfo skylinkInfo) {
        this.permissions = permissions;
        this.requestCode = requestCode;
        this.skylinkInfo = skylinkInfo;
    }

    public String[] getPermissions() {
        return permissions;
    }

    public void setPermissions(String[] permissions) {
        this.permissions = permissions;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }

    public SkylinkInfo getSkylinkInfo() {
        return skylinkInfo;
    }

    public void setSkylinkInfo(SkylinkInfo skylinkInfo) {
        this.skylinkInfo = skylinkInfo;
    }

}
