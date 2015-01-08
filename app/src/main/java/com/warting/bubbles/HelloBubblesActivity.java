package com.warting.bubbles;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ListView;

import java.util.List;
import java.util.Random;

import sg.com.temasys.skylink.sdk.sample.R;
import sg.com.temasys.skylink.sdk.rtc.SkyLinkException;
import sg.com.temasys.skylink.sdk.sample.RoomManager;

public class HelloBubblesActivity extends Activity {

    final static private String TAG = "HelloBubblesActivity";

    final static public String EXTRA_PEER_ID = "com.warting.bubbles.HelloBubblesActivity.peerId";

    private static Random random;

    private com.warting.bubbles.DiscussArrayAdapter adapter;
    private EditText editText1;
    private ListView lv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore state if rotated.
        if (savedInstanceState != null) {
            RoomManager.get().setChatPeerId(
                    savedInstanceState.getString(EXTRA_PEER_ID));
            RoomManager.get().setChatAdapter(
                    RoomManager.get().getTempChatAdapter());
            RoomManager.get().setTempChatAdapter(null);
        }

        setContentView(R.layout.activity_discuss);
        random = new Random();

        lv = (ListView) findViewById(R.id.listView1);

        adapter = new DiscussArrayAdapter(getApplicationContext(),
                R.layout.listitem_discuss);
        RoomManager.get().setChatAdapter(adapter);

        lv.setAdapter(adapter);

        String peerId = getIntent().getStringExtra(EXTRA_PEER_ID);
        if (peerId == null)
            setTitle(R.string.title_chat_group);
        else
            setTitle(String.format(getString(R.string.title_chat_private),
                    RoomManager.get().getDisplayName(peerId)));

        editText1 = (EditText) findViewById(R.id.editText1);
        editText1.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN)
                        && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    String message = editText1.getText().toString();
                    OneComment comment = new OneComment(false, message);
                    adapter.add(comment);
                    String peerId = getIntent().getStringExtra(EXTRA_PEER_ID);
                    if (peerId == null) {
                        RoomManager.get().addGroupChat(comment);
                        RoomManager.get().getConnection()
                                .sendCustomMessage(peerId, message);
                    } else {
                        RoomManager.get().addPrivateChat(peerId, comment);
                        try {
                            RoomManager.get().getConnection()
                                    .sendPeerMessage(peerId, message);
                        } catch (SkyLinkException e) {
                            Log.w(TAG, e.getLocalizedMessage(), e);
                        }
                    }
                    editText1.setText("");
                    return true;
                }
                return false;
            }
        });

        addItems();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_PEER_ID, RoomManager.get().getChatPeerId());
        RoomManager.get()
                .setTempChatAdapter(RoomManager.get().getChatAdapter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RoomManager.get().setChatPeerId(null);
        RoomManager.get().setChatAdapter(null);
    }

    private void addItems() {
        String peerId = getIntent().getStringExtra(EXTRA_PEER_ID);
        List<OneComment> messageList = null;
        if (peerId == null)
            messageList = RoomManager.get().getGroupChat();
        else
            messageList = RoomManager.get().getPrivateChat(peerId);
        if (messageList != null)
            for (int i = 0; i < messageList.size(); i++)
                adapter.add(messageList.get(i));
    }

    @SuppressWarnings("unused")
    private static int getRandomInteger(int aStart, int aEnd) {
        if (aStart > aEnd) {
            throw new IllegalArgumentException("Start cannot exceed End.");
        }
        long range = (long) aEnd - (long) aStart + 1;
        long fraction = (long) (range * random.nextDouble());
        int randomNumber = (int) (fraction + aStart);
        return randomNumber;
    }

}