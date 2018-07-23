package sg.com.temasys.skylink.sdk.sampleapp.chat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import sg.com.temasys.skylink.sdk.sampleapp.R;

/**
 * Created by muoi.pham on 20/07/18.
 */
public class ChatActivity extends AppCompatActivity {

    private final String CHAT_FRAGMENT_TAG = "CHAT_FRAGMENT";

    private ChatPresenter mChatPresenter;
    private ChatFragment mChatFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //check previous state in case of screen rotation
        if (savedInstanceState == null) {
            mChatFragment = ChatFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.contentFrameChat, mChatFragment, CHAT_FRAGMENT_TAG)
                    .commit();
        } else {
            mChatFragment = (ChatFragment) getSupportFragmentManager()
                    .findFragmentByTag(CHAT_FRAGMENT_TAG);
        }

        //link between view and presenter
        mChatPresenter = new ChatPresenter(mChatFragment, this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the fragment's instance
        getSupportFragmentManager().putFragment(outState, CHAT_FRAGMENT_TAG, mChatFragment);
    }
}
