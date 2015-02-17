package sg.com.temasys.skylink.sdk.rtc;

import android.os.AsyncTask;
import android.util.Log;

import java.util.Date;

/**
 * Retrieves date time information from a NTP server and converts from
 * local time zone to GMT
 */
class GMTService extends AsyncTask<Void, Integer, Long> {

    private static final String NTP_SERVER = "0.amazon.pool.ntp.org";
    private static final int TIME_OUT = 10000;

    private GMTServiceListener listener;
    private static final String TAG = GMTService.class.getCanonicalName();

    public GMTService(GMTServiceListener listener) {
        this.listener = listener;
    }

    @Override
    protected Long doInBackground(Void... params) {
        SntpClient sntpClient = new SntpClient();
        long currentGmtTime = 0;

        // Time out of 10 seconds
        if (sntpClient.requestTime(NTP_SERVER, TIME_OUT)) {
            // Time request is successful
            currentGmtTime = sntpClient.getNtpTime();
            Log.d(TAG, "Fetched time " + currentGmtTime);
        }
        
        return currentGmtTime;
    }

    @Override
    protected void onPostExecute(Long currentGmtTime) {
        if (currentGmtTime != 0) {
            // Notify that the time is successfully fetched
            // Convert the received time from the local TimeZone to GMT
            listener.onCurrentTimeFetched(Utils.convertTimeStampToGMT(currentGmtTime));
        } else {
            // Notify that the time could not be successfully fetched
            listener.onCurrentTimeFetchedFailed();
        }
    }
}

/**
 * Listener Interface for GMTService
 */
interface GMTServiceListener {
    void onCurrentTimeFetched(Date date);

    void onCurrentTimeFetchedFailed();
}
