package sg.com.temasys.sdk.sample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.temasys.skylink.sample.R;


public class Split4Fragment extends RoomFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_split_4, container, false);

        if (RoomManager.get().getRemoteVideoList().size() == 4) {
            ViewGroup parent = (ViewGroup) rootView.findViewById(R.id.video1);
            RoomManager.VideoInfo videoInfo = RoomManager.get().getRemoteVideoList().get(0);
            addVideoView(parent, videoInfo.getVideoView(),
                    videoInfo.getPeerId());
            // Set chat alerts.
            RoomManager.get().setChatAlert(rootView, videoInfo,
                    R.id.chatPrivate1, R.id.chatGroup1);

            parent = (ViewGroup) rootView.findViewById(R.id.video2);
            videoInfo = RoomManager.get().getRemoteVideoList().get(1);
            addVideoView(parent, videoInfo.getVideoView(),
                    videoInfo.getPeerId());
            // Set chat alerts.
            RoomManager.get().setChatAlert(rootView, videoInfo,
                    R.id.chatPrivate2, R.id.chatGroup2);

            parent = (ViewGroup) rootView.findViewById(R.id.video3);
            videoInfo = RoomManager.get().getRemoteVideoList().get(2);
            addVideoView(parent, videoInfo.getVideoView(),
                    videoInfo.getPeerId());
            // Set chat alerts.
            RoomManager.get().setChatAlert(rootView, videoInfo,
                    R.id.chatPrivate3, R.id.chatGroup3);

            parent = (ViewGroup) rootView.findViewById(R.id.video4);
            videoInfo = RoomManager.get().getRemoteVideoList().get(3);
            addVideoView(parent, videoInfo.getVideoView(),
                    videoInfo.getPeerId());
            // Set chat alerts.
            RoomManager.get().setChatAlert(rootView, videoInfo,
                    R.id.chatPrivate4, R.id.chatGroup4);
        }

        return rootView;
    }

}
