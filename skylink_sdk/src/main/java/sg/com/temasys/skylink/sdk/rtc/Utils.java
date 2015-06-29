package sg.com.temasys.skylink.sdk.rtc;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;

class Utils {

    private static final String TAG = Utils.class.getName();
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    public static final String TIME_ZONE_UTC = "UTC";
    public static final String ISO_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZ";
    private static final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Returns the SkylinkConnectionString
     *
     * @param roomName  Name of the room
     * @param appKey    App Key
     * @param secret    App secret
     * @param startTime Room Start Time
     * @param duration  Duration of the room in Hours
     * @return
     */
    public static String getSkylinkConnectionString(String roomName, String appKey,
                                                    String secret,
                                                    Date startTime, int duration) {

        Log.d(TAG, "Room name " + roomName);
        Log.d(TAG, "App Key " + appKey);
        Log.d(TAG, "startTime " + startTime);
        Log.d(TAG, "duration " + duration);

        // Convert the date in to ISO format
        String dateString = Utils.getISOTimeStamp(startTime);

        // Compute RFC 2104-compliant HMAC signature
        String cred = calculateRFC2104HMAC(roomName + "_" + duration + "_"
                + dateString, secret);
        try {
            cred = URLEncoder.encode(cred, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return appKey + "/"
                + roomName + "/" + dateString + "/" + duration + "?cred="
                + cred;
    }

    /**
     * Computes RFC 2104-compliant HMAC signature.
     *
     * @param data data The data to be signed.
     * @param key  The signing key.
     * @return The Base64-encoded RFC 2104-compliant HMAC signature.
     */
    public static String calculateRFC2104HMAC(String data, String key) {
        String result = null;
        try {

            // Get an hmac_sha1 key from the raw key bytes
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(),
                    HMAC_SHA1_ALGORITHM);

            // Get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);

            // Compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data.getBytes());

            // Base64-encode the hmac
            result = Base64
                    .encodeToString(rawHmac, android.util.Base64.DEFAULT);

        } catch (Exception e) {
            Log.e(TAG, "Failed to generate HMAC : " + e.getMessage(), e);
        }
        return result.substring(0, result.length() - 1);
    }

    /**
     * Returns the date in ISO time format
     *
     * @param date
     * @return ISO timestamp
     */
    public static String getISOTimeStamp(Date date) {
        TimeZone tz = TimeZone.getTimeZone(TIME_ZONE_UTC);
        DateFormat df = new SimpleDateFormat(ISO_TIME_FORMAT);
        df.setTimeZone(tz);
        return df.format(date);
    }

    /**
     * Converts a given input stream to String
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while ((line = bufferedReader.readLine()) != null) {
            result += line;
        }
        inputStream.close();
        return result;
    }

    /**
     * Returns a string from input stream
     *
     * @param in
     * @return
     */
    public static String drainStream(InputStream in) {
        Scanner s = new Scanner(in).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    /**
     * Runs the specified action on the UI thread
     *
     * @param action
     */
    public static void runOnUiThread(Runnable action) {
        handler.post(action);
    }

    /**
     * Mangle SDP to have recvonly for audio.
     *
     * @param sdpDescription
     * @return Mangled or original SDP.
     */
    public static String sdpAudioSendOnly(String sdpDescription) {
        String sdp = sdpSendReceiveOnly(sdpDescription, true, true);
        return sdp;
    }

    /**
     * Mangle SDP to have sendonly for video.
     *
     * @param sdpDescription
     * @return Mangled or original SDP.
     */
    public static String sdpVideoSendOnly(String sdpDescription) {
        String sdp = sdpSendReceiveOnly(sdpDescription, true, false);
        return sdp;
    }

    /**
     * Mangle SDP to have sendonly for audio.
     *
     * @param sdpDescription
     * @return Mangled or original SDP.
     */
    public static String sdpAudioReceiveOnly(String sdpDescription) {
        String sdp = sdpSendReceiveOnly(sdpDescription, false, true);
        return sdp;
    }

    /**
     * Mangle SDP to have recvonly for video.
     *
     * @param sdpDescription
     * @return Mangled or original SDP.
     */
    public static String sdpVideoReceiveOnly(String sdpDescription) {
        String sdp = sdpSendReceiveOnly(sdpDescription, false, false);
        return sdp;
    }

    /**
     * Mangle SDP to have sendonly or recvonly for audio or video.
     *
     * @param sdpDescription
     * @param isSendOnly
     * @param isAudio
     * @return Mangled or original SDP.
     */
    public static String sdpSendReceiveOnly(String sdpDescription, boolean isSendOnly, boolean isAudio) {
        String strSR = "sendrecv";
        String strS = "sendonly";
        String strR = "recvonly";

        String[] segments;
        String segment;

        String mediaType = "audio";
        if (!isAudio) {
            mediaType = "video";
        }

        String regex = "^a=(" + strSR + "|" + strS + "|" + strR + ")$";

        String newStr = "a=" + strS;
        if (!isSendOnly) {
            newStr = "a=" + strR;
        }

        // Get segment to search and replace.
        segments = sdpSegment(sdpDescription, mediaType);
        segment = segments[1];

        // Segment was found, try to search and replace with sendrecv.
        if (!"".equals(segment)) {
            segment = sdpReplace(segment, regex, newStr, true);
        } else {
            // Return original sdp
            return sdpDescription;
        }

        // If replacement was done
        if (!"".equals(segment)) {
            // Add it back to remaining segments and return.
            String sdp = segments[0] + segment + segments[2];
            return sdp;
        } else {
            // Return original sdp
            return sdpDescription;
        }
    }

    /**
     * Extracts a particular segment of the SDP.
     *
     * @param sdpDescription SDP description string.
     * @param mediaType      The type of media segment to return, for e.g. audio, video
     * @return An array of the SDP, where [1] is the segment desired, [0] is the segment before, and
     * [2] is the segment after. If segment not found return SDP in [0], and "" in the remaining
     * elements.
     */

    public static String[] sdpSegment(String sdpDescription, String mediaType) {
        String marker = "a=mid:";
        String markerStart = "a=mid:" + mediaType;
        String[] segments = new String[3];
        Arrays.fill(segments, "");

        String[] lines = sdpDescription.split("\r\n");
        ArrayList<String> linesBefore = new ArrayList<String>();
        ArrayList<String> linesFound = new ArrayList<String>();
        ArrayList<String> linesAfter = new ArrayList<String>();
        boolean started = false;
        boolean over = false;

        // Collect segment in linesFound
        for (String lineCur : lines) {
            // Stop if already started and found marker again.
            if (lineCur.startsWith(marker) && started || over) {
                over = true;
                linesAfter.add(lineCur);
                continue;
            }
            // Collect lines once started or if markerStart found. 
            if (markerStart.equals(lineCur) || started) {
                started = true;
                linesFound.add(lineCur);
                continue;
            }
            // Collect lines before started.
            linesBefore.add(lineCur);
        }

        // Return segments found
        String segment = "";
        if (started) {
            for (String line : linesBefore) {
                segments[0] += line + "\r\n";
            }
            for (String line : linesFound) {
                segments[1] += line + "\r\n";
            }
            for (String line : linesAfter) {
                segments[2] += line + "\r\n";
            }
        } else {
            segments[0] = sdpDescription;
            segments[1] = "";
            segments[2] = "";
        }
        return segments;
    }

    /**
     * Replaces a particular SDP line with another one.
     *
     * @param sdpDescription     SDP description string.
     * @param regex              Regex string specifying the line to be replaced.
     * @param newLine            String to be replace the line matching regex. Newline characters
     *                           will be automatically provided, so do not add these unless you
     *                           really want multiple newline characters.
     * @param firstOccurenceOnly Boolean for replacing first occurrence only. Replace all occurences
     *                           if false.
     * @return The replaced SDP or "" if no replacement occurred.
     */

    public static String sdpReplace(String sdpDescription, String regex, String newLine, boolean firstOccurenceOnly) {
        String lineReturn = "";
        String[] lines = sdpDescription.split("\r\n");
        boolean found = false;

        Pattern sdpPattern = Pattern.compile(regex);
        for (String lineCur : lines) {
            // If only first occurrence needed, and it has been found, simply add line to return line.
            if (firstOccurenceOnly && found) {
                lineReturn += lineCur + "\r\n";
                continue;
            }
            // Check if it is a line to replace.
            Matcher sdoMatcher = sdpPattern.matcher(lineCur);
            if (sdoMatcher.matches()) {
                found = true;
                lineReturn += newLine + "\r\n";
            } else {
                lineReturn += lineCur + "\r\n";
            }
        }

        // Return replaced (or not) string.
        String segment = "";
        if (found) {
            return lineReturn;
        } else {
            return "";
        }
    }

    /**
     * Mangle SDP to prefer the given codec over any other audio/video codec.
     *
     * @param sdpDescription
     * @param codec
     * @param isAudio
     * @return
     */
    public static String preferCodec(
            String sdpDescription, String codec, boolean isAudio) {
        String[] lines = sdpDescription.split("\r\n");
        int mLineIndex = -1;
        String codecRtpMap = null;
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
        Pattern codecPattern = Pattern.compile(regex);
        String mediaDescription = "m=video ";
        if (isAudio) {
            mediaDescription = "m=audio ";
        }
        for (int i = 0; (i < lines.length) &&
                (mLineIndex == -1 || codecRtpMap == null); i++) {
            if (lines[i].startsWith(mediaDescription)) {
                mLineIndex = i;
                continue;
            }
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                codecRtpMap = codecMatcher.group(1);
                continue;
            }
        }
        if (mLineIndex == -1) {
            Log.w(TAG, "No " + mediaDescription + " line, so can't prefer " + codec);
            return sdpDescription;
        }
        if (codecRtpMap == null) {
            Log.w(TAG, "No rtpmap for " + codec + ", so can't prefer " + codec);
            return sdpDescription;
        }
        Log.d(TAG, "Found " + codec + " rtpmap " + codecRtpMap + ", prefer at " +
                lines[mLineIndex]);
        String[] origMLineParts = lines[mLineIndex].split(" ");
        StringBuilder newMLine = new StringBuilder();
        int origPartIndex = 0;
        // Format is: m=<media> <port> <proto> <fmt> ...
        newMLine.append(origMLineParts[origPartIndex++]).append(" ");
        newMLine.append(origMLineParts[origPartIndex++]).append(" ");
        newMLine.append(origMLineParts[origPartIndex++]).append(" ");
        newMLine.append(codecRtpMap);
        for (; origPartIndex < origMLineParts.length; origPartIndex++) {
            if (!origMLineParts[origPartIndex].equals(codecRtpMap)) {
                newMLine.append(" ").append(origMLineParts[origPartIndex]);
            }
        }
        lines[mLineIndex] = newMLine.toString();
        Log.d(TAG, "Change media description: " + lines[mLineIndex]);
        StringBuilder newSdpDescription = new StringBuilder();
        for (String line : lines) {
            newSdpDescription.append(line).append("\r\n");
        }
        return newSdpDescription.toString();
    }

    /**
     * Mangle SDP to prefer stereo audio for Opus
     *
     * @param sdpDescription
     * @param skylinkConfig
     * @return
     */
    public static String modifyStereoAudio(String sdpDescription, SkylinkConfig skylinkConfig) {

        if (skylinkConfig.getPreferredAudioCodec() != SkylinkConfig.AudioCodec.OPUS) {
            Log.d(TAG, "Cannot add stereo configuration due to preferred audio codec is not opus");
            return sdpDescription;
        }

        String[] lines = sdpDescription.split("\r\n");
        Pattern codecPattern = Pattern.compile("^a=fmtp:(111) ");

        boolean sdpModified = false;

        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = codecPattern.matcher(lines[i]);
            if (matcher.find()) {
                if (skylinkConfig.isStereoAudio() && !lines[i].contains("stereo=1")) {
                    // If the user requires stereo but if its not on SDP, add it to the end
                    lines[i] += ";stereo=1";
                    Log.d(TAG, "Added stereo to the sdp");
                    sdpModified = true;
                } else if (!skylinkConfig.isStereoAudio() && lines[i].contains("stereo=1")) {
                    // If the user does not require stereo but if its on SDP, replace it
                    lines[i] = lines[i].replace("stereo=1;", "");
                    // Include this if stereo is at the end
                    lines[i] = lines[i].replace("stereo=1", "");
                    Log.d(TAG, "Removed stereo from the sdp");
                    sdpModified = true;
                }
                break;
            }
        }

        if (sdpModified) {
            // If the SDP is modified use the new modified string
            StringBuilder newSdpDescription = new StringBuilder();
            for (String line : lines) {
                newSdpDescription.append(line).append("\r\n");
            }
            sdpDescription = newSdpDescription.toString();
        }

        return sdpDescription;
    }
}
