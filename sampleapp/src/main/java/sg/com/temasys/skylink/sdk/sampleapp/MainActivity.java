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

import sg.com.temasys.skylink.sdk.sampleapp.setting.Config;
import sg.com.temasys.skylink.sdk.sampleapp.audio.AudioCallActivity;
import sg.com.temasys.skylink.sdk.sampleapp.chat.ChatActivity;
import sg.com.temasys.skylink.sdk.sampleapp.datatransfer.DataTransferActivity;
import sg.com.temasys.skylink.sdk.sampleapp.filetransfer.FileTransferActivity;
import sg.com.temasys.skylink.sdk.sampleapp.multipartyvideo.MultiPartyVideoCallActivity;
import sg.com.temasys.skylink.sdk.sampleapp.setting.SettingActivity;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoCallActivity;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener{

    private LinearLayout audioContainerBig, videoContainerBig, chatContainerBig, fileContainerBig, dataContainerBig, multiVideoContainerBig;
    private RelativeLayout audioContainer, videoContainer, chatContainer, fileContainer, dataContainer, multiVideoContainer;
    private ImageButton btnAudio, btnVideo, btnChat, btnFile, btnData, btnMultiVideo;
    private TextView tvAudio, tvVideo, tvChat, tvFile, tvData, tvMultiVideo;
    private ImageView imgLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getControlWidgets();

        initComponents();

        // Load selected App key details
        Config.loadSelectedAppKey(this);
        Config.loadRoomUserNames(this);

        //init utils
        Utils utils = new Utils(this);
    }

    private void getControlWidgets() {
        audioContainerBig = (LinearLayout) findViewById(R.id.audioContainerBig);
        videoContainerBig = (LinearLayout) findViewById(R.id.videoContainerBig);
        chatContainerBig = (LinearLayout) findViewById(R.id.chatContainerBig);
        fileContainerBig = (LinearLayout) findViewById(R.id.fileContainerBig);
        dataContainerBig = (LinearLayout) findViewById(R.id.dataContainerBig);
        multiVideoContainerBig = (LinearLayout) findViewById(R.id.multiVideoContainerBig);

        audioContainer = (RelativeLayout) findViewById(R.id.audioContainer);
        videoContainer = (RelativeLayout) findViewById(R.id.videoContainer);
        chatContainer = (RelativeLayout) findViewById(R.id.chatContainer);
        fileContainer = (RelativeLayout) findViewById(R.id.fileContainer);
        dataContainer = (RelativeLayout) findViewById(R.id.dataContainer);
        multiVideoContainer = (RelativeLayout) findViewById(R.id.multiVideoContainer);

        btnAudio = (ImageButton) findViewById(R.id.btnAudioCall);
        btnVideo = (ImageButton) findViewById(R.id.btnVideoCall);
        btnChat = (ImageButton) findViewById(R.id.btnChat);
        btnFile = (ImageButton) findViewById(R.id.btnFileTransfer);
        btnData = (ImageButton) findViewById(R.id.btnDataTransfer);
        btnMultiVideo = (ImageButton) findViewById(R.id.btnMultiVideoCall);

        tvAudio = (TextView) findViewById(R.id.tvAudio);
        tvVideo = (TextView) findViewById(R.id.tvVideo);
        tvChat = (TextView) findViewById(R.id.tvChat);
        tvFile = (TextView) findViewById(R.id.tvFileTransfer);
        tvData = (TextView) findViewById(R.id.tvDataTransfer);
        tvMultiVideo = (TextView) findViewById(R.id.tvMultiVideoCall);

        imgLogo = (ImageView) findViewById(R.id.imgLogo);
    }

    private void initComponents() {
        audioContainerBig.setOnClickListener(this);
        videoContainerBig.setOnClickListener(this);
        chatContainerBig.setOnClickListener(this);
        fileContainerBig.setOnClickListener(this);
        dataContainerBig.setOnClickListener(this);
        multiVideoContainerBig.setOnClickListener(this);

        audioContainer.setOnClickListener(this);
        videoContainer.setOnClickListener(this);
        chatContainer.setOnClickListener(this);
        fileContainer.setOnClickListener(this);
        dataContainer.setOnClickListener(this);
        multiVideoContainer.setOnClickListener(this);

        btnAudio.setOnClickListener(this);
        btnVideo.setOnClickListener(this);
        btnChat.setOnClickListener(this);
        btnFile.setOnClickListener(this);
        btnData.setOnClickListener(this);
        btnMultiVideo.setOnClickListener(this);

        tvAudio.setOnClickListener(this);
        tvVideo.setOnClickListener(this);
        tvChat.setOnClickListener(this);
        tvFile.setOnClickListener(this);
        tvData.setOnClickListener(this);
        tvMultiVideo.setOnClickListener(this);

        //need check imgLogo incase of landscape orientation
        //because imgLogo is not available for landscape orientation
        if(imgLogo != null)
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

    private void processImglogo(){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse("https://temasys.io/"));
        startActivity(intent);
    }
}
