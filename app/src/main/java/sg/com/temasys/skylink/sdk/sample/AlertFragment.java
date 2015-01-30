package sg.com.temasys.skylink.sdk.sample;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

public class AlertFragment extends DialogFragment {

    final static private String BUNDLE_MESSAGE = "sg.com.temasys.skylink.sdk.sample.AlertFragment.message";

    private String mMessage;

    public AlertFragment() {

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            mMessage = savedInstanceState.getString(BUNDLE_MESSAGE);
        else
            mMessage = getArguments().getString(BUNDLE_MESSAGE);
        return new AlertDialog.Builder(getActivity()).setMessage(mMessage)
                .setPositiveButton(android.R.string.ok, null).create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_MESSAGE, mMessage);
    }

    public static AlertFragment newInstance(String message) {
        AlertFragment fragment = new AlertFragment();
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_MESSAGE, message);
        fragment.setArguments(bundle);
        return fragment;
    }

}
