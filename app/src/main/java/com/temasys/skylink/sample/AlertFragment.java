package com.temasys.skylink.sample;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

public class AlertFragment extends DialogFragment {

    final static private String BUNDLE_MESSAGE = "tools.skylink.sample.AlertFragment.message";

    private String mMessage;

    public AlertFragment() {

    }

	/*public AlertFragment(String message) {
        super();
		mMessage = message;
	}*/

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            // For automatic recreation.
            mMessage = savedInstanceState.getString(BUNDLE_MESSAGE);
        else
            // For manual creation.
            mMessage = getArguments().getString(BUNDLE_MESSAGE);

        return new AlertDialog.Builder(getActivity()).setMessage(mMessage)
                .setPositiveButton(android.R.string.ok, null).create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_MESSAGE, mMessage);
    }

    public static AlertFragment newInstance(String message) {
        AlertFragment fragment = new AlertFragment();
        Bundle args = new Bundle();
        args.putString(BUNDLE_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

}
