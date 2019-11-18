package sg.com.temasys.skylink.sdk.sampleapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import sg.com.temasys.skylink.sdk.sampleapp.audio.AudioActivity;
import sg.com.temasys.skylink.sdk.sampleapp.chat.ChatActivity;
import sg.com.temasys.skylink.sdk.sampleapp.datatransfer.DataTransferActivity;
import sg.com.temasys.skylink.sdk.sampleapp.filetransfer.FileTransferActivity;
import sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo.MultiPartyVideoCallActivity;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.setting.SettingActivity;
import sg.com.temasys.skylink.sdk.sampleapp.utils.MainDemoButton;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageView imgBanner;
    private MainDemoButton btnMainAudio, btnMainVideo, btnMainChat, btnMainFile, btnMainData, btnMainMulti;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get the UI controls from layout
        getControlWidgets();

        initComponents();

        // Load selected App key and Room User setting details
        Config.loadSelectedAppKey(this);
        Config.loadRoomUserNames(this);

        //init utils
        Utils utils = new Utils(this);
    }

    private void getControlWidgets() {
        btnMainAudio = findViewById(R.id.btnMainAudio);
        btnMainVideo = findViewById(R.id.btnMainVideo);
        btnMainChat = findViewById(R.id.btnMainChat);
        btnMainFile = findViewById(R.id.btnMainFile);
        btnMainData = findViewById(R.id.btnMainData);
        btnMainMulti = findViewById(R.id.btnMainMulti);

        btnMainAudio.setType(MainDemoButton.ButtonType.AUDIO);
        btnMainVideo.setType(MainDemoButton.ButtonType.VIDEO);
        btnMainChat.setType(MainDemoButton.ButtonType.CHAT);
        btnMainFile.setType(MainDemoButton.ButtonType.FILE);
        btnMainData.setType(MainDemoButton.ButtonType.DATA);
        btnMainMulti.setType(MainDemoButton.ButtonType.MULTI);

        imgBanner = findViewById(R.id.imgBanner);
    }

    private void initComponents() {
        btnMainAudio.setOnClickListener(this);
        btnMainVideo.setOnClickListener(this);
        btnMainChat.setOnClickListener(this);
        btnMainFile.setOnClickListener(this);
        btnMainData.setOnClickListener(this);
        btnMainMulti.setOnClickListener(this);

        //need check imgBanner in case of landscape orientation
        //because imgBanner is not available for landscape orientation
        if (imgBanner != null)
            imgBanner.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingActivity.class));

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnMainAudio:
                processAudio();
                break;
            case R.id.btnMainChat:
                processChat();
                break;
            case R.id.btnMainFile:
                processFileTransfer();
                break;
            case R.id.btnMainData:
                processDataTransfer();
                break;
            case R.id.btnMainMulti:
                processMultiVideo();
                break;
            case R.id.btnMainVideo:
                processVideo();
                break;
            case R.id.imgBanner:
                processImglogo();
                break;
        }
    }

    private void processAudio() {
        startActivity(new Intent(this, AudioActivity.class));
    }

    private void processChat() {
        startActivity(new Intent(this, ChatActivity.class));
    }

    private void processFileTransfer() {
        startActivity(new Intent(this, FileTransferActivity.class));
    }

    private void processDataTransfer() {
        startActivity(new Intent(this, DataTransferActivity.class));
    }

    private void processMultiVideo() {
        startActivity(new Intent(this, MultiPartyVideoCallActivity.class));
    }

    private void processVideo() {
        startActivity(new Intent(this, VideoActivity.class));
    }

    private void processImglogo() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse("https://temasys.io/"));
        startActivity(intent);
    }
}
