package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import sg.com.temasys.skylink.sdk.BuildConfig;
import sg.com.temasys.skylink.sdk.config.SkylinkConfig;

/**
 * Created by xiangrong on 1/3/16.
 */
public class SkylinkLog {

    /**
     * Log verbose (internal)
     *
     * @param tag
     * @param strLog
     */
    static void logV(String tag, String strLog) {
        Log.v(tag, strLog);
    }

    /**
     * Log debug (internal)
     *
     * @param tag
     * @param strLog
     */
    static void logD(String tag, String strLog) {
        Log.d(tag, strLog);
    }

    /**
     * Log info (external, i.e. for SDK user) only if SkylinkConfig.enableLogs is true.
     *
     * @param tag
     * @param strLog
     */
    static void logI(String tag, String strLog) {
        if (!shouldLog()) {
            return;
        }
        Log.i(tag, strLog);
    }

    /**
     * Log warning (external, i.e. for SDK user) only if SkylinkConfig.enableLogs is true.
     *
     * @param tag
     * @param strLog
     */
    static void logW(String tag, String strLog) {
        if (!shouldLog()) {
            return;
        }
        Log.w(tag, strLog);
    }

    /**
     * Log error (external, i.e. for SDK user) only if SkylinkConfig.enableLogs is true.
     *
     * @param tag
     * @param strLog
     */
    public static void logE(String tag, String strLog) {
        if (!shouldLog()) {
            return;
        }
        Log.e(tag, strLog);
    }

    /**
     * Checks SkylinkConfig of the SkylinkConnection instance to see if logging is enable,
     * via the enableLogs member.
     *
     * @return true/false if logging is enable/disabled.
     */
    private static boolean shouldLog() {
        SkylinkConnection skylinkConnection = SkylinkConnection.getInstance();
        if (skylinkConnection != null) {
            SkylinkConfig config = skylinkConnection.getSkylinkConfig();
            if (config != null) {
                return config.isEnableLogs();
            }
        }
        // If SkylinkConfig has not been set, return true only if in debug.
        // All other cases, return false as logging is false by default.
        return BuildConfig.DEBUG;
    }
}
