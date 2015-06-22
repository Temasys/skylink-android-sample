package sg.com.temasys.skylink.sdk.rtc;

/**
 * Created by xiangrong on 12/6/15.
 */

/**
 * Implementation will extract room parameters from an App server response string. Room parameters
 * and other interactions will be returned via a RoomParameterListener.
 */
interface RoomParameterProcessor {
    public void processRoomParameters(String serverResponse);

    public void setRoomParameterListener(RoomParameterListener roomParameterListener);
}

/**
 * Implementations will return RoomParameters and errors from a RoomParameterProcessor.
 */
interface RoomParameterListener {
    public void onRoomParameterSuccessful(RoomParameters params);

    public void onRoomParameterError(int message);

    public void onRoomParameterError(String message);
}
