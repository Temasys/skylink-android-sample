package sg.com.temasys.skylink.sdk.sampleapp.data.model;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class PermRequesterInfor {
    private String[] permissions;
    private int requestCode;
    private int infoCode;

    public PermRequesterInfor() {
    }

    public PermRequesterInfor(String[] permissions, int requestCode, int infoCode) {
        this.permissions = permissions;
        this.requestCode = requestCode;
        this.infoCode = infoCode;
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

    public int getInfoCode() {
        return infoCode;
    }

    public void setInfoCode(int infoCode) {
        this.infoCode = infoCode;
    }
}
