package sg.com.temasys.skylink.sdk.sampleapp;

import android.widget.TextView;

public class Utils {

    private Utils() {
    }

    public static void setRoomDetails(boolean isPeerInRoom, TextView textView, String peerName) {
        String roomDetails = "Room Name : " + Constants.ROOM_NAME
                + "\nYou are signed in as : " + Constants.MY_USER_NAME + "\n";
        if (isPeerInRoom) {
            roomDetails += "Peer Name : " + peerName;
        } else {
            roomDetails += "You are alone in this room";
        }
        textView.setText(roomDetails);
    }
}
