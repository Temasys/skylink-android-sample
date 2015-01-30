package sg.com.temasys.skylink.sdk.sample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import sg.com.temasys.skylink.sdk.sample.RoomManager.VideoInfo;

public class Split3Fragment extends RoomFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_split_3, container, false);

        if (RoomManager.get().getRemoteVideoList().size() == 3) {
            ViewGroup parent = (ViewGroup) rootView.findViewById(R.id.video1);
            VideoInfo videoInfo = RoomManager.get().getRemoteVideoList().get(0);
            addVideoView(parent, videoInfo.getVideoView(),
                    videoInfo.getPeerId());
            setChatNotif(rootView, videoInfo, R.id.notifPrivate1,
                    R.id.notifGroup1);

            parent = (ViewGroup) rootView.findViewById(R.id.video2);
            videoInfo = RoomManager.get().getRemoteVideoList().get(1);
            addVideoView(parent, videoInfo.getVideoView(),
                    videoInfo.getPeerId());
            setChatNotif(rootView, videoInfo, R.id.notifPrivate2,
                    R.id.notifGroup2);

            parent = (ViewGroup) rootView.findViewById(R.id.video3);
            videoInfo = RoomManager.get().getRemoteVideoList().get(2);
            addVideoView(parent, videoInfo.getVideoView(),
                    videoInfo.getPeerId());
            setChatNotif(rootView, videoInfo, R.id.notifPrivate3,
                    R.id.notifGroup3);
        }

        return rootView;
    }

}
