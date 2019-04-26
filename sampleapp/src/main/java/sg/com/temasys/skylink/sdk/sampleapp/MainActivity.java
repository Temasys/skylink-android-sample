package sg.com.temasys.skylink.sdk.sampleapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import sg.com.temasys.skylink.sdk.sampleapp.audio.AudioCallActivity;
import sg.com.temasys.skylink.sdk.sampleapp.chat.ChatActivity;
import sg.com.temasys.skylink.sdk.sampleapp.datatransfer.DataTransferActivity;
import sg.com.temasys.skylink.sdk.sampleapp.filetransfer.FileTransferActivity;
import sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo.MultiPartyVideoCallActivity;
import sg.com.temasys.skylink.sdk.sampleapp.screensharing.ScreenSharingActivity;
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.setting.SettingActivity;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoCallActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private LinearLayout audioContainerBig, videoContainerBig, chatContainerBig, fileContainerBig, dataContainerBig, multiVideoContainerBig, screenShareContainerBig;
    private RelativeLayout audioContainer, videoContainer, chatContainer, fileContainer, dataContainer, multiVideoContainer, screenShareContainer;
    private ImageButton btnAudio, btnVideo, btnChat, btnFile, btnData, btnMultiVideo, btnScreenShare;
    private TextView tvAudio, tvVideo, tvChat, tvFile, tvData, tvMultiVideo, tvScreenShare;
    private ImageView imgLogo;

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

        // processScreenShare();
    }

    private void getControlWidgets() {
        audioContainerBig = findViewById(R.id.audioContainerBig);
        videoContainerBig = findViewById(R.id.videoContainerBig);
        chatContainerBig = findViewById(R.id.chatContainerBig);
        fileContainerBig = findViewById(R.id.fileContainerBig);
        dataContainerBig = findViewById(R.id.dataContainerBig);
        multiVideoContainerBig = findViewById(R.id.multiVideoContainerBig);
        screenShareContainerBig = findViewById(R.id.screenShareContainerBig);

        audioContainer = findViewById(R.id.audioContainer);
        videoContainer = findViewById(R.id.videoContainer);
        chatContainer = findViewById(R.id.chatContainer);
        fileContainer = findViewById(R.id.fileContainer);
        dataContainer = findViewById(R.id.dataContainer);
        multiVideoContainer = findViewById(R.id.multiVideoContainer);
        screenShareContainer = findViewById(R.id.screenShareContainer);

        btnAudio = findViewById(R.id.btnAudioCall);
        btnVideo = findViewById(R.id.btnVideoCall);
        btnChat = findViewById(R.id.btnChat);
        btnFile = findViewById(R.id.btnFileTransfer);
        btnData = findViewById(R.id.btnDataTransfer);
        btnMultiVideo = findViewById(R.id.btnMultiVideoCall);
        btnScreenShare = findViewById(R.id.btnScreenShare);

        tvAudio = findViewById(R.id.tvAudio);
        tvVideo = findViewById(R.id.tvVideo);
        tvChat = findViewById(R.id.tvChat);
        tvFile = findViewById(R.id.tvFileTransfer);
        tvData = findViewById(R.id.tvDataTransfer);
        tvMultiVideo = findViewById(R.id.tvMultiVideoCall);
        tvScreenShare = findViewById(R.id.tvsSreenShare);

        imgLogo = findViewById(R.id.imgLogo);
    }

    private void initComponents() {
        audioContainerBig.setOnClickListener(this);
        videoContainerBig.setOnClickListener(this);
        chatContainerBig.setOnClickListener(this);
        fileContainerBig.setOnClickListener(this);
        dataContainerBig.setOnClickListener(this);
        multiVideoContainerBig.setOnClickListener(this);
        screenShareContainerBig.setOnClickListener(this);

        audioContainer.setOnClickListener(this);
        videoContainer.setOnClickListener(this);
        chatContainer.setOnClickListener(this);
        fileContainer.setOnClickListener(this);
        dataContainer.setOnClickListener(this);
        multiVideoContainer.setOnClickListener(this);
        screenShareContainer.setOnClickListener(this);

        btnAudio.setOnClickListener(this);
        btnVideo.setOnClickListener(this);
        btnChat.setOnClickListener(this);
        btnFile.setOnClickListener(this);
        btnData.setOnClickListener(this);
        btnMultiVideo.setOnClickListener(this);
        btnScreenShare.setOnClickListener(this);

        tvAudio.setOnClickListener(this);
        tvVideo.setOnClickListener(this);
        tvChat.setOnClickListener(this);
        tvFile.setOnClickListener(this);
        tvData.setOnClickListener(this);
        tvMultiVideo.setOnClickListener(this);
        tvScreenShare.setOnClickListener(this);

        //need check imgLogo in case of landscape orientation
        //because imgLogo is not available for landscape orientation
        if (imgLogo != null)
            imgLogo.setOnClickListener(this);
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
            case R.id.audioContainerBig:
            case R.id.audioContainer:
            case R.id.btnAudioCall:
            case R.id.tvAudio:
                processAudio();
                break;
            case R.id.videoContainerBig:
            case R.id.videoContainer:
            case R.id.btnVideoCall:
            case R.id.tvVideo:
                processVideo();
                break;
            case R.id.chatContainerBig:
            case R.id.chatContainer:
            case R.id.btnChat:
            case R.id.tvChat:
                processChat();
                break;
            case R.id.fileContainerBig:
            case R.id.fileContainer:
            case R.id.btnFileTransfer:
            case R.id.tvFileTransfer:
                processFileTransfer();
                break;
            case R.id.dataContainerBig:
            case R.id.dataContainer:
            case R.id.btnDataTransfer:
            case R.id.tvDataTransfer:
                processDataTransfer();
                break;
            case R.id.multiVideoContainerBig:
            case R.id.multiVideoContainer:
            case R.id.btnMultiVideoCall:
            case R.id.tvMultiVideoCall:
                processMultiVideo();
                break;
            case R.id.screenShareContainerBig:
            case R.id.screenShareContainer:
            case R.id.btnScreenShare:
            case R.id.tvsSreenShare:
                processScreenShare();
                break;
            case R.id.imgLogo:
                processImglogo();
                break;
        }
    }

    private void processAudio() {
        startActivity(new Intent(this, AudioCallActivity.class));
    }

    private void processVideo() {
        startActivity(new Intent(this, VideoCallActivity.class));
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

    private void processScreenShare() {
        startActivity(new Intent(this, ScreenSharingActivity.class));
    }

    private void processImglogo() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse("https://temasys.io/"));
        startActivity(intent);
    }
}
