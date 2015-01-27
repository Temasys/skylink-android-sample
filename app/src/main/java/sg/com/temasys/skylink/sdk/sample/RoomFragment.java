package sg.com.temasys.skylink.sdk.sample;

import android.app.Fragment;
import android.opengl.GLSurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.TextView;

import sg.com.temasys.skylink.sdk.sample.RoomManager.VideoInfo;

public abstract class RoomFragment extends Fragment {

    final static private String TAG = "RoomFragment";

    protected void addVideoView(ViewGroup parent, final View child,
                                final String peerId) {
        TextView noVideotxtVw;
        String noVideoStr;
        final View viewToAdd;
        if (child == null) {
            noVideotxtVw = new TextView(parent.getContext());
            if (peerId == null) noVideoStr = "Self video";
            else {
                noVideoStr = "Video of Peer " + RoomManager.get().getDisplayName(peerId) + " (" +
                        peerId + ")";
            }
            noVideoStr += " is not available.";
            noVideotxtVw.setText(noVideoStr);
            viewToAdd = noVideotxtVw;
            parent.addView(noVideotxtVw);
        } else {
            viewToAdd = child;
            ViewGroup existingParent = (ViewGroup) child.getParent();
            if (existingParent != null)
                existingParent.removeView(child);
            parent.addView(child);
            ViewTreeObserver vto = child.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @SuppressWarnings("deprecation")
                @Override
                public void onGlobalLayout() {
                    child.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    Utility.layoutSubviews((GLSurfaceView) child, null);
                }
            });
        }


        if (peerId != null) {
            viewToAdd.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    OptionAlertFragment.newInstance(peerId).show(
                            RoomFragment.this.getFragmentManager(), TAG);
                }
            });
        }
    }

    protected void setChatNotif(ViewGroup viewGroup, VideoInfo videoInfo,
                                int notifPrivateId, int notifGroupId) {
        String peerId = videoInfo.getPeerId();

        TextView txtNotifPrivate = (TextView) viewGroup
                .findViewById(notifPrivateId);
        RoomManager.get().putPrivateTextView(peerId, txtNotifPrivate);
        txtNotifPrivate.setText(RoomManager.get().getPrivateNotif(peerId));
        txtNotifPrivate.setBackgroundColor(0X33FFFFFF); // 20% white
        txtNotifPrivate.setTextColor(0XFF000000); // 100% black

        TextView txtNotifGroup = (TextView) viewGroup
                .findViewById(notifGroupId);
        RoomManager.get().putGroupTextView(peerId, txtNotifGroup);
        txtNotifGroup.setText(RoomManager.get().getGroupNotif(peerId));
        txtNotifGroup.setBackgroundColor(0X33FFFFFF); // 20% white
        txtNotifGroup.setTextColor(0XFF000000); // 100% black
    }

}