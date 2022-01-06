package sg.com.temasys.skylink.sdk.sampleapp.setting;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.KeyInfo;
import sg.com.temasys.skylink.sdk.sampleapp.utils.RecyclerViewAdapter;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static android.content.Context.MODE_PRIVATE;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.jsonArrayRemove;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;


/**
 * A simple {@link Fragment} subclass.
 */
public class ConfigKeyFragment extends Fragment {

    private static final String TAG = ConfigKeyFragment.class.getName();
    public static final String APP_KEY_LIST_SMR = "appKeyListSmr";
    public static final String APP_KEY_LIST_NO_SMR = "appKeyListNoSmr";
    private KeyInfo keyInfoSmrDefault;
    private KeyInfo keyInfoNoSmrDefault;

    private ActionBar actionBar;

    RadioGroup rGroup;
    EditText keyText;
    EditText secretText;
    EditText descText;
    Button btnSave;
    Button btnCancel;
    RadioButton rbSmr;
    RadioButton rbNoSmr;
    RadioGroup rgSmrorNot;
    CheckBox cbValidate;
    Dialog dialog;
    RecyclerView recyclerView;

    boolean addKeySmr; // Whether App key is SMR enabled.
    private boolean validateKeySecret; // Whether to do simple validation for App Key and Secret.

    static TextView currentKey;
    static TextView description;

    RecyclerView.Adapter recyclerViewAdapter;
    RecyclerView.LayoutManager recyclerViewLayoutManager;

    final private String PREFS_NAME = "KeyInfo";

    List keyInfoList = new ArrayList();
    public static boolean smrSelect = Boolean.parseBoolean(null);

    public static JSONArray appKeyListNoSmr = new JSONArray();
    public static JSONArray appKeyListSmr = new JSONArray();

    public ConfigKeyFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_config_key, container, false);

        // Load the default App keys from config.xml and App key list from SharedPreferences.
        loadAvailableAppKeyLists();

        currentKey = (TextView) view.findViewById(R.id.currentKey);
        description = (TextView) view.findViewById(R.id.description);
        setSelectedKeyViews();

        recyclerView = (RecyclerView) view.findViewById(R.id.rvKeyList);
        recyclerViewLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(recyclerViewLayoutManager);
        rGroup = (RadioGroup) view.findViewById(R.id.radio_grp_manage_smr);
        rGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radio_btn_manage_smr) {
                    smrSelect = true;
                    keyInfoList = Utils.convertJSONArrayToKeyInfoList(appKeyListSmr);
                } else if (checkedId == R.id.radio_btn_manage_no_smr) {
                    smrSelect = false;
                    keyInfoList = Utils.convertJSONArrayToKeyInfoList(appKeyListNoSmr);
                }
                recyclerViewAdapter = new RecyclerViewAdapter(getContext(), keyInfoList,
                        getActivity(), ConfigKeyFragment.this);
                recyclerView.setAdapter(recyclerViewAdapter);
            }
        });
        // Default to show App key List of Selected key.
        setRbAppKeyListSmr(Config.isAppKeySmr());

        FloatingActionButton createKeyInfo = (FloatingActionButton) view.findViewById(R.id.fbtn_create_key);
        createKeyInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createKey();
            }
        });

        // setup the action bar
        setActionBar();

        return view;
    }

    //----------------------------------------------------------------------------------------------
    // Internal methods
    //----------------------------------------------------------------------------------------------

    private void setActionBar() {
        actionBar = ((SettingActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        actionBar.setTitle("Keys setting");

        setHasOptionsMenu(true);
    }

    private void createKey() {
        getDialogBox();
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();

                String key = keyText.getText().toString();
                String secret = secretText.getText().toString();
                String description = descText.getText().toString();
                if (validateKeySecret &&
                        !Utils.checkAppKeyAndSecret(key, secret)) {
                    String log = "Incorrect key, secret and/or you need to choose SMR status!";
                    toastLogLong(TAG, getContext(), log);
                    return;
                }

                if (addAppKey(new KeyInfo(key, secret, description, addKeySmr), true)) {
                    // Update the UI for changes to keyInfo.
                    setSelectedKeyViews();
                    // Set radioButton and AppKeyList to reflect added key.
                    setRbAppKeyListSmr(addKeySmr);
                    if (addKeySmr) {
                        keyInfoList = Utils.convertJSONArrayToKeyInfoList(appKeyListSmr);
                    } else {
                        keyInfoList = Utils.convertJSONArrayToKeyInfoList(appKeyListNoSmr);
                    }
                    recyclerViewAdapter = new RecyclerViewAdapter(getContext(), keyInfoList,
                            getActivity(), ConfigKeyFragment.this);
                    recyclerView.setAdapter(recyclerViewAdapter);
                    String log = "Key added.";
                    toastLog(TAG, getContext(), log);
                } else {
                    String log = "Key could not be added! Default keys cannot be added again.";
                    toastLogLong(TAG, getContext(), log);
                }
            }
        });

    }

    private void getDialogBox() {
        Context context = getContext();
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_key_info);
        dialog.setTitle("Create New Key");
        //Prepare TextBox Hint Text
        TextInputLayout keyInputLayout = (TextInputLayout) dialog.findViewById(R.id.keyTextLayout);
        keyInputLayout.setHint("Key");
        TextInputLayout secretInputLayout = (TextInputLayout) dialog.findViewById(R.id.secretTextLayout);
        secretInputLayout.setHint("Secret");
        TextInputLayout descInputLayout = (TextInputLayout) dialog.findViewById(R.id.descTextLayout);
        descInputLayout.setHint("Description");
        // Get The Elements
        keyText = (EditText) dialog.findViewById(R.id.keyEditText);
        secretText = (EditText) dialog.findViewById(R.id.secretEditText);
        descText = (EditText) dialog.findViewById(R.id.descEditText);
        btnSave = (Button) dialog.findViewById(R.id.btnSave);
        btnCancel = (Button) dialog.findViewById(R.id.btnCancel);

        rbSmr = (RadioButton) dialog.findViewById(R.id.radioSmr);
        rbNoSmr = (RadioButton) dialog.findViewById(R.id.radioNoSmr);
        rgSmrorNot = (RadioGroup) dialog.findViewById(R.id.radio_grp_manage);
        rgSmrorNot.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                if (checkedId == rbSmr.getId()) addKeySmr = true;
                else if (checkedId == rbNoSmr.getId()) addKeySmr = false;
            }
        });
        // Default is no SMR.
        rgSmrorNot.check(rbNoSmr.getId());

        cbValidate = (CheckBox) dialog.findViewById(R.id.cbCheckKey);
        cbValidate.setOnCheckedChangeListener(
                new CheckBox.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        validateKeySecret = cbValidate.isChecked();
                    }
                }
        );
        // Default is to validate.
        cbValidate.setChecked(true);

        dialog.show();
    }


    /**
     * Load the available App keys into appKeyListSmr and appKeyListNoSmr.
     * Set first element with default key (from config.xml).
     * Load remaining list from Preferences, if any.
     * If any of the keys from Preferences are the same as default key, abandon those.
     */
    private void loadAvailableAppKeyLists() {

        // Load the default App keys from config.xml
        keyInfoNoSmrDefault = new KeyInfo(
                getResources().getString(R.string.app_key_no_smr),
                getResources().getString(R.string.app_key_secret_no_smr),
                getResources().getString(R.string.app_key_desc_no_smr),
                false);
        keyInfoSmrDefault = new KeyInfo(
                getResources().getString(R.string.app_key_smr),
                getResources().getString(R.string.app_key_secret_smr),
                getResources().getString(R.string.app_key_desc_smr),
                true);

        try {
            // Put default keys into App key lists.
            appKeyListNoSmr.put(0, keyInfoNoSmrDefault.getJson());
            appKeyListSmr.put(0, keyInfoSmrDefault.getJson());

            // Put keys from SharedPreferences into App key lists.
            SharedPreferences pref = getActivity().getPreferences(MODE_PRIVATE);
            String prefStrNoSmr = pref.getString(APP_KEY_LIST_NO_SMR, null);
            String prefStrSmr = pref.getString(APP_KEY_LIST_SMR, null);

            if (prefStrNoSmr != null) {
                JSONArray prefListNoSmr = new JSONArray(prefStrNoSmr);
                addAppKeyList(prefListNoSmr, false);
            }
            if (prefStrSmr != null) {
                JSONArray prefListSmr = new JSONArray(prefStrSmr);
                addAppKeyList(prefListSmr, false);
            }
            storeAppKeyLists(getActivity());

        } catch (JSONException e) {
            Log.e(TAG, "[loadAppKeyLists] Error: " + e.getMessage());
        }
    }

    //----------------------------------------------------------------------------------------------
    // APIs
    //----------------------------------------------------------------------------------------------

    /**
     * Add App key into List of AppKey for the same SMR status.
     * If App key already existed in either of AppKeyList, it will either:
     * - Not be added if the existing App key is a default key.
     * - Replace the existing App key if the existing one is not a default key.
     *
     * @param keyInfo
     * @param store   If true, store App Key List to SharedPreferences.
     * @return True is App key was added, false if it could not be added.
     */
    public boolean addAppKey(KeyInfo keyInfo, boolean store) {
        String appKey = keyInfo.getKey();
        String appKeySecret = keyInfo.getSecret();
        String appKeyDesc = keyInfo.getDesc();
        boolean isSmr = keyInfo.isSmr();

        int posNoSmr = Utils.getKeyPosition(appKey, appKeyListNoSmr);
        int posSmr = Utils.getKeyPosition(appKey, appKeyListSmr);

        // Do not add if it's either of the default keys.
        if (posNoSmr == 0 || posSmr == 0) {
            return false;
        }

        // Prepare to add to List of the same SMR state.
        int posThis = posNoSmr;
        int posOther = posSmr;
        JSONArray listThis = appKeyListNoSmr;
        if (isSmr) {
            posThis = posSmr;
            posOther = posNoSmr;
            listThis = appKeyListSmr;
        }

        // Replace existing key if already present.
        if (posThis > 0) {
            try {
                listThis.put(posThis, keyInfo.getJson());
            } catch (JSONException e) {
                Log.e(TAG, "[addAppKey] Error: " + e.getMessage() + ".");
                return false;
            }
        } else {
            // Add additional key if not already present.
            listThis.put(keyInfo.getJson());
        }

        // Remove non-default key of other List if existed.
        if (posOther > 0) {
            removeFromConfigAppKeyList(posOther, !isSmr);
        }

        // If replaced key is Selected key, set to Config.
        if (appKey.equals(Config.getAppKey())) {
            Config.setAppKey(appKey, getActivity());
            Config.setAppKeySecret(appKeySecret, getActivity());
            Config.setAppKeyDescription(appKeyDesc, getActivity());
            Config.setAppKeySmr(isSmr, getActivity());
        }

        if (store) {
            storeAppKeyLists(getActivity());
        }
        return true;
    }

    /**
     * Add the App Keys from the given list into Config.
     *
     * @param fromList List of App Keys to be added.
     * @param store    If true, store App Key List to SharedPreferences.
     */
    public void addAppKeyList(JSONArray fromList, boolean store) {
        for (int i = 0; i < fromList.length(); ++i) {
            JSONObject json = null;
            try {
                json = fromList.getJSONObject(i);
            } catch (JSONException e) {
                Log.e(TAG, "[addAppKeyList] Error: " + e.getMessage() + ".");
            }
            if (json != null) {
                KeyInfo keyInfo = new KeyInfo(json);
                addAppKey(keyInfo, false);
            }
        }
        // Store to SharedPreferences only at the end, if required.
        if (store) {
            storeAppKeyLists(getActivity());
        }
    }

    public static boolean editAppKey(KeyInfo keyInfoNew, int pos, boolean wasSmr, Activity activity) {

        String appKeyNew = keyInfoNew.getKey();
        String appKeySecretNew = keyInfoNew.getSecret();
        String appKeyDescNew = keyInfoNew.getDesc();
        boolean isSmr = keyInfoNew.isSmr();

        int posNoSmr = Utils.getKeyPosition(appKeyNew, appKeyListNoSmr);
        int posSmr = Utils.getKeyPosition(appKeyNew, appKeyListSmr);

        // Do not add if it's either of the default keys.
        if (posNoSmr == 0 || posSmr == 0) {
            return false;
        }

        JSONArray listOld = appKeyListNoSmr;
        if (wasSmr) {
            listOld = appKeyListSmr;
        }
        KeyInfo keyInfoOld = null;
        try {
            keyInfoOld = new KeyInfo(listOld.getJSONObject(pos));
        } catch (JSONException e) {
            Log.e(TAG, "[editAppKey] Error: " + e.getMessage() + ".");
            return false;
        }

        // Save edited Key into SharedPreferences and Config (if is selected key).
        try {
            // JSONObject KeyInfo stored in Config.
            JSONObject keyInfoJson;

            // Write into existing App key.
            keyInfoJson = listOld.getJSONObject(pos);
            KeyInfo.jsonPutKeyInfo(keyInfoJson, appKeyNew, appKeySecretNew, appKeyDescNew, isSmr);

            // Remove any existing App key that is the same as the new App key.
            boolean toRemoveSmr = false;
            int toRemovePos = -1;
            if (posNoSmr > 0) {
                toRemovePos = posNoSmr;
                toRemoveSmr = false;
            } else if (posSmr > 0) {
                toRemovePos = posSmr;
                toRemoveSmr = true;
            }
            // Remove existing App key.
            if (toRemovePos > 0) {
                removeFromConfigAppKeyList(toRemovePos, toRemoveSmr);

                // Find position of the old App key again as it might have changed.
                pos = Utils.getKeyPosition(appKeyNew, listOld);

                // Note if App key removed was Selected key,
                // Selected key details will be overwritten as below.
            }

            // If SMR value changed, key has to be switched to the other Config array.
            if (isSmr != wasSmr) {
                if (wasSmr) {
                    // Key was SMR but is now not SMR.
                    appKeyListNoSmr.put(keyInfoJson);
                    removeFromConfigAppKeyList(pos, true);
                } else {
                    // Key was not SMR but is now SMR.
                    appKeyListSmr.put(keyInfoJson);
                    removeFromConfigAppKeyList(pos, false);
                }
            }

            // If edited key or replaced key is Selected key, set to Config.
            if (keyInfoOld.getKey().equals(Config.getAppKey()) ||
                    appKeyNew.equals(Config.getAppKey())) {
                Config.setAppKey(appKeyNew, activity);
                Config.setAppKeySecret(appKeySecretNew, activity);
                Config.setAppKeyDescription(appKeyDescNew, activity);
                Config.setAppKeySmr(isSmr, activity);
            }

            // Update the SharedPreferences.
            storeAppKeyLists(activity);
            return true;

        } catch (JSONException e) {
            Log.e(TAG, "[editAppKey] Error: " + e.getMessage() + ".");
            return false;
        }
    }

    /**
     * Deletes a specific AppKey.
     * Switch Selected key to default key if the same SMR status is deleted key was Selected key.
     *
     * @param pos
     * @param wasSmr
     * @param activity
     */
    public static void deleteAppKey(int pos, boolean wasSmr, Activity activity) {
        // Switch to default key for same SMR value if this is selected key.
        JSONArray appKeyList = appKeyListNoSmr;
        if (wasSmr) {
            appKeyList = appKeyListSmr;
        }
        KeyInfo keyInfoOld = null;
        try {
            keyInfoOld = new KeyInfo(appKeyList.getJSONObject(pos));
            if (keyInfoOld.getKey().equals(Config.getAppKey())) {
                // Get and set new keyInfo
                KeyInfo keyInfoNew = new KeyInfo(appKeyList.getJSONObject(0));
                Config.setAppKey(keyInfoNew.getKey(), activity);
                Config.setAppKeySecret(keyInfoNew.getSecret(), activity);
                Config.setAppKeyDescription(keyInfoNew.getDesc(), activity);
                Config.setAppKeySmr(keyInfoNew.isSmr(), activity);
            }
        } catch (JSONException e) {
            Log.e(TAG, "[deleteAppKey] Error: " + e.getMessage() + ".");
        }

        removeFromConfigAppKeyList(pos, wasSmr);

        // Update the SharePreferences.
        storeAppKeyLists(activity);
    }

    /**
     * Set the RadioButtons for AppKeyLists' SMR state.
     *
     * @param isSmr
     */
    public void setRbAppKeyListSmr(boolean isSmr) {
        int rbId = R.id.radio_btn_manage_no_smr;
        if (isSmr) {
            rbId = R.id.radio_btn_manage_smr;
        }
        rGroup.check(rbId);
    }

    /**
     * Removes the App Key at the given AppKeyList in ConfigKeyFragment.
     *
     * @param position
     * @param isSmr
     * @return
     */
    public static void removeFromConfigAppKeyList(int position, boolean isSmr) {
        if (isSmr) {
            appKeyListSmr = jsonArrayRemove(appKeyListSmr, position);
        } else {
            appKeyListNoSmr = jsonArrayRemove(appKeyListNoSmr, position);
        }
    }

    /**
     * Gets the selected key from Config.
     */
    public static void setSelectedKeyViews() {
        currentKey.setText(Config.getAppKey());
        String desc = "[No SMR] ";
        if (Config.isAppKeySmr()) {
            desc = "[SMR] ";
        }
        desc += Config.getAppKeyDescription();
        description.setText(desc);
    }

    /**
     * Put App Key lists appKeyListNoSmr and appKeyListSmr into SharePreferences.
     *
     * @param activity
     */
    public static void storeAppKeyLists(Activity activity) {
        SharedPreferences userInfo = activity.getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = userInfo.edit();

        editor.putString(APP_KEY_LIST_NO_SMR, appKeyListNoSmr.toString());
        editor.putString(APP_KEY_LIST_SMR, appKeyListSmr.toString());
        editor.commit();
        Log.i(TAG, appKeyListSmr.toString());
        Log.i(TAG, appKeyListNoSmr.toString());
        Log.i(TAG, "App Key Lists stored into SharedPreferences.");
    }

}