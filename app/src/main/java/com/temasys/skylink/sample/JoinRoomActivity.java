package com.temasys.skylink.sample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class JoinRoomActivity extends Activity {

    final static private String TAG = "JoinRoomActivity";

    final static public String EXTRA_RESULT_DISCONNECT_STATUS = "tools.skylink.sample.JoinRoomActivity.disconnnectStatus";

    public enum DisconnectStatus {
        ON_CONNECT_FALSE(0), ON_DISCONNECT(1);

        private final int value;

        private DisconnectStatus(int value) {
            this.value = value;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null)
            return;
        int disconnectStatus = data.getIntExtra(
                JoinRoomActivity.EXTRA_RESULT_DISCONNECT_STATUS, 0);

        if (disconnectStatus == DisconnectStatus.ON_CONNECT_FALSE.value) {
            AlertFragment
                    .newInstance(
                            getResources().getString(
                                    R.string.message_on_connect_false)).show(
                    JoinRoomActivity.this.getFragmentManager(), TAG);
        } else if (disconnectStatus == DisconnectStatus.ON_DISCONNECT.value) {
            AlertFragment.newInstance(
                    getResources().getString(R.string.message_on_disconnect))
                    .show(JoinRoomActivity.this.getFragmentManager(), TAG);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_room);
        // Set background to while.
        getWindow().getDecorView().setBackgroundColor(Color.WHITE);

        Button joinButton = (Button) findViewById(R.id.join_button);
        joinButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                String displayName = ((EditText) findViewById(R.id.display_name_editText))
                        .getText().toString().trim();
                String roomName = ((EditText) findViewById(R.id.room_name_editText))
                        .getText().toString().trim();

                if (displayName.length() <= 0) {
                    AlertFragment.newInstance(
                            getResources().getString(
                                    R.string.message_display_name_missing))
                            .show(JoinRoomActivity.this.getFragmentManager(),
                                    TAG);
                } else if (roomName.length() <= 0) {
                    AlertFragment.newInstance(
                            getResources().getString(
                                    R.string.message_room_name_missing)).show(
                            JoinRoomActivity.this.getFragmentManager(), TAG);
                } else {
                    Intent intent = new Intent(JoinRoomActivity.this,
                            RoomViewActivity.class);
                    intent.putExtra(RoomViewActivity.EXTRA_DISPLAY_NAME,
                            displayName);
                    intent.putExtra(RoomViewActivity.EXTRA_ROOM_NAME, roomName);
                    startActivityForResult(intent, 0);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        super.onBackPressed();
        finish();
    }

}
