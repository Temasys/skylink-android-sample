package com.sg.temasys.skylink.sdk.test.helper;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.sg.temasys.skylink.sdk.test.helper.SignalingParameterHelperListener;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import sg.com.temasys.skylink.sdk.server.SignalingServerClient;
import sg.com.temasys.skylink.sdk.utils.Utils;

/**
 * Created by janidu on 13/1/15.
 */
public class SignalingParameterHelper {

    public static final String TAG = SignalingServerClient.class.getName();

    public static final String API_SERVER = "http://api.temasys.com.sg/api/";
    public static final String API_KEY = "cff8ff52-ce29-4840-a489-0ceef3af81f0";
    public static final String API_SECRET = "lm230j4zrx6la";

    public static final String ROOM_NAME = "UnitTestRoom";
    public static final float CALL_DURATION = Float.MAX_VALUE;
    public static final String DATE_STRING = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:00.0'Z'")
            .format(new Date());

    public static final String JSON_KEY_SERVER = "ipSigserver";
    public static final String JSON_KEY_PORT = "portSigserver";

    /**
     * Retrieves Signaling parameters from the API server
     *
     * @param context
     * @param listener
     * @return
     * @throws UnsupportedEncodingException
     */
    public static void retrieveSignalingParameters(Context context,
                                                   SignalingParameterHelperListener listener)
            throws UnsupportedEncodingException {

        String credentials = null;
        credentials = Utils.calculateRFC2104HMAC(ROOM_NAME + "_" + CALL_DURATION + "_"
                + DATE_STRING, API_SECRET);
        credentials = URLEncoder.encode(credentials, "UTF-8");

        String url = "http://api.temasys.com.sg/api/" + API_KEY + "/"
                + ROOM_NAME + "/" + DATE_STRING + "/" + CALL_DURATION + "?cred="
                + credentials;

        getParameters(context, url, listener);
    }

    /**
     * Method used to fetch parameters from the API server
     *
     * @param context
     * @param url
     * @param listener
     */
    private static void getParameters(Context context,
                                      final String url,
                                      final SignalingParameterHelperListener listener) {

        Log.d(TAG, "getParameters");

        AsyncTask asyncTask = new AsyncTask<String, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(String... apiUrl) {
                InputStream inputStream = null;
                String result = "";
                JSONObject jsonObject = null;
                try {

                    HttpClient httpclient = new DefaultHttpClient();
                    HttpResponse httpResponse = httpclient.execute(new HttpGet(apiUrl[0]));

                    inputStream = httpResponse.getEntity().getContent();

                    if (inputStream != null) {
                        result = Utils.convertInputStreamToString(inputStream);
                        jsonObject = new JSONObject(result);
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }

                return jsonObject;
            }

            @Override
            protected void onPostExecute(JSONObject jsonObject) {
                Log.d(TAG, "onPostExecute");
                listener.onSignalingParametersReceived(jsonObject);
            }
        };

        String[] urlArray = {url};
        asyncTask.execute(urlArray);
    }
}
