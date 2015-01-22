package sg.com.temasys.skylink.sdk.sample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SelfVideoFragment extends RoomFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_split_1, container, false);

        View child = RoomManager.get().getSelfVideo().getVideoView();
        addVideoView(rootView, child, null);

        return rootView;
    }

}
