package sg.com.temasys.skylink.sdk.sampleapp;

import android.widget.TextView;

public class Utils {

    private Utils() {
    }

    public static void setRoomDetails(boolean isPeerInRoom, TextView textView, String peerName,String roomName,String userName) {
        String roomDetails = "Room Name : " + roomName
                + "\nYou are signed in as : " + userName + "\n";
        if (isPeerInRoom) {
            roomDetails += "Peer Name : " + peerName;
        } else {
            roomDetails += "You are alone in this room";
        }
        textView.setText(roomDetails);
    }
}
