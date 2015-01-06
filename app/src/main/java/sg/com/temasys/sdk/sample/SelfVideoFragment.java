package sg.com.temasys.sdk.sample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.temasys.skylink.sample.R;

public class SelfVideoFragment extends RoomFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_split_1, container, false);

        View child = RoomManager.get().getSelfVideo().getVideoView();
        addVideoView(rootView, child, null);

        return rootView;
    }

}
