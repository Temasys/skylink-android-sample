package sg.com.temasys.skylink.sdk.rtc;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaConstraints;

import java.io.IOException;
import java.io.InputStream;


/**
 * Service class that will fetch room parameters from a given url
 */
class RoomParameterService extends AsyncTask<String, Void, AppRTCSignalingParameters> {

    public static final String TAG = RoomParameterService.class.getName();
    public static final String APP_OWNER = "apiOwner";
    public static final String CID = "cid";
    public static final String DISPLAY_NAME = "displayName";
    public static final String LEN = "len";
    public static final String ROOM_CRED = "roomCred";
    public static final String ROOM_KEY = "room_key";
    public static final String ROOM_START = "start";
    public static final String ROOM_TIME_STAMP = "timeStamp";
    public static final String USER_CRED = "userCred";
    public static final String USERNAME = "username";
    public static final String MEDIA_CONSTRAINTS = "media_constraints";
    public static final String MANDATORY = "mandatory";
    public static final String MAX_WIDTH = "maxWidth";
    public static final String MAX_HEIGHT = "maxHeight";
    public static final String IP_SIGSERVER = "ipSigserver";
    public static final String PORT_SIGSERVER = "portSigserver";
    public static final String ERROR_MESSAGES = "error_messages";
    public static final String SUCCESS = "success";
    public static final String INFO = "info";
    public static final String PROTOCOL = "protocol";
    public static final String ERROR = "error";

    private RoomParameterServiceListener roomParameterServiceListener;

    public RoomParameterService(RoomParameterServiceListener roomParameterServiceListener) {
        this.roomParameterServiceListener = roomParameterServiceListener;
    }

    @Override
    protected AppRTCSignalingParameters doInBackground(String... urls) {
        if (urls.length != 1) {
            throw new RuntimeException("Must be called with a single URL");
        }
        try {
            return getParametersForRoomUrl(urls[0]);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private AppRTCSignalingParameters getParametersForRoomUrl(String url)
            throws IOException, JSONException {

        Log.d(TAG, "getParametersForRoomUrl " + url);

        // Append json query
        String jsonURL = url + "&t=json";

        InputStream inputStream = null;
        String result = "";
        JSONObject roomJson = null;
        try {

            // Fetch room parameters
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse httpResponse = httpclient.execute(new HttpGet(jsonURL));

            inputStream = httpResponse.getEntity().getContent();

            if (inputStream != null) {
                result = Utils.convertInputStreamToString(inputStream);
                roomJson = new JSONObject(result);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

        if (roomJson == null) {
            roomParameterServiceListener.onRoomParameterError(ErrorCodes.UNEXPECTED_ERROR_ROOM_PARAMETERS);
            return null;
        }

        if (roomJson.has(ERROR) && !roomJson.getBoolean(SUCCESS)) {
            roomParameterServiceListener.onRoomParameterError(roomJson.getString(INFO));
            return null;
        }

        AppRTCSignalingParameters parameters = new AppRTCSignalingParameters();
        parameters.setAppOwner(roomJson.getString(APP_OWNER));
        Log.d(TAG, "apiOwner->" + parameters.getAppOwner());

        parameters.setCid(roomJson.getString(CID));
        Log.d(TAG, "cid->" + parameters.getCid());

        parameters.setDisplayName(roomJson.getString(DISPLAY_NAME));
        Log.d(TAG, "displayName->" + parameters.getDisplayName());

        parameters.setLen(roomJson.getString(LEN));
        Log.d(TAG, "len->" + parameters.getLen());

        parameters.setRoomCred(roomJson.getString(ROOM_CRED));
        Log.d(TAG, "roomCred->" + parameters.getRoomCred());

        parameters.setRoomId(roomJson.getString(ROOM_KEY));
        Log.d(TAG, "room_key->" + parameters.getRoomId());

        parameters.setStart(roomJson.getString(ROOM_START));
        Log.d(TAG, "start->" + parameters.getStart());

        parameters.setTimeStamp(roomJson.getString(ROOM_TIME_STAMP));
        Log.d(TAG, "timeStamp->" + parameters.getTimeStamp());

        parameters.setUserCred(roomJson.getString(USER_CRED));
        Log.d(TAG, "userCred->" + parameters.getUserCred());

        parameters.setUserId(roomJson.getString(USERNAME));
        Log.d(TAG, "username->" + parameters.getUserId());

        String mc = roomJson.getString(MEDIA_CONSTRAINTS);
        if (mc != null) {
            Log.d(TAG, "media_constraints JSON:\n" + mc);
            JSONObject mcDict = new JSONObject(mc);

            JSONObject manDict = mcDict.getJSONObject(MANDATORY);
            MediaConstraints.KeyValuePair maxWidth = new MediaConstraints.KeyValuePair
                    (MAX_WIDTH, manDict.getString(MAX_WIDTH));
            MediaConstraints.KeyValuePair maxHeight = new MediaConstraints.KeyValuePair
                    (MAX_HEIGHT, manDict.getString(MAX_HEIGHT));
            MediaConstraints videoConstraints = new MediaConstraints();
            videoConstraints.mandatory.add(maxWidth);
            videoConstraints.mandatory.add(maxHeight);
            parameters.setVideoConstraints(videoConstraints);
        }

        String ipSignalingServer = roomJson.getString(IP_SIGSERVER);
        Log.d(TAG, "ipSigserver->" + ipSignalingServer);
        parameters.setIpSigserver(ipSignalingServer);

        int portSignalingServer = roomJson.getInt(PORT_SIGSERVER);
        Log.d(TAG, "portSigserver->" + portSignalingServer);

        parameters.setPortSigserver(portSignalingServer);

        if (ipSignalingServer == null || portSignalingServer <= 0) {
            Log.d(TAG, "Invalid signaling server ip and port. Returning ...");
            roomParameterServiceListener.onRoomParameterError
                    ("Invalid signaling server ip and port. Returning ...");
            return null;
        }

        parameters.setProtocol(roomJson.getString(PROTOCOL));
        Log.d(TAG, "protocol->" + parameters.getProtocol());

        return parameters;
    }

    @Override
    protected void onPostExecute(AppRTCSignalingParameters params) {
        if (params != null) {
            roomParameterServiceListener.onRoomParameterSuccessful(params);
        }
    }
}

interface RoomParameterServiceListener {
    void onRoomParameterSuccessful(AppRTCSignalingParameters params);

    void onRoomParameterError(int message);

    void onRoomParameterError(String message);

}
