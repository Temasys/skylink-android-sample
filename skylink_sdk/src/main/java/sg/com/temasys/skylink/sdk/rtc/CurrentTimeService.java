package sg.com.temasys.skylink.sdk.rtc;

import android.os.AsyncTask;

import java.util.Date;

import static sg.com.temasys.skylink.sdk.rtc.SkylinkLog.logD;

/**
 * Retrieves date time information from a NTP server
 */
class CurrentTimeService extends AsyncTask<Void, Integer, Long> {

    private static final String NTP_SERVER = "0.amazon.pool.ntp.org";
    private static final int TIME_OUT = 10000;

    private CurrentTimeServiceListener listener;
    private static final String TAG = CurrentTimeService.class.getCanonicalName();

    public CurrentTimeService(CurrentTimeServiceListener listener) {
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
            logD(TAG, "Fetched time: " + currentGmtTime);
        }

        return currentGmtTime;
    }

    @Override
    protected void onPostExecute(Long currentGmtTime) {
        if (currentGmtTime != 0) {
            // Notify that the time is successfully fetched
            listener.onCurrentTimeFetched(new Date(currentGmtTime));
        } else {
            // Notify that the time could not be successfully fetched
            listener.onCurrentTimeFetchedFailed();
        }
    }
}

/**
 * Listener Interface for CurrentTimeService
 */
interface CurrentTimeServiceListener {
    void onCurrentTimeFetched(Date date);

    void onCurrentTimeFetchedFailed();
}
