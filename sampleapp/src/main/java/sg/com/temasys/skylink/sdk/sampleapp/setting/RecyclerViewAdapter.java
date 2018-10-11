package sg.com.temasys.skylink.sdk.sampleapp.setting;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import sg.com.temasys.skylink.sdk.sampleapp.R;
import sg.com.temasys.skylink.sdk.sampleapp.service.model.KeyInfo;
import sg.com.temasys.skylink.sdk.sampleapp.utils.Utils;

import static sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigKeyFragment.appKeyListNoSmr;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigKeyFragment.appKeyListSmr;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigKeyFragment.deleteAppKey;
import static sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigKeyFragment.setSelectedKeyViews;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.checkAppKeyAndSecret;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLog;
import static sg.com.temasys.skylink.sdk.sampleapp.utils.Utils.toastLogLong;

/**
 * Created by phyo.pwint on 29/7/16.
 */


public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.rvViewHolder> {

    final private String TAG = RecyclerViewAdapter.class.getName();

    // The Fragment that starts this RecyclerViewAdapter.
    ConfigKeyFragment configKeyFragment;
    // List of AppKeyList from ConfigKeyFragment that populates RecyclerView.
    private List<KeyInfo> keyInfoListConfig;
    Activity activity;
    Context context;

    // UI on Dialog box for changing App Key.
    CardView listCardLayout;
    EditText keyText;
    EditText secretText;
    EditText descText;
    Button btnSave;
    Button btnCancel;
    RadioButton rbSMR;
    RadioButton rbNoSMR;
    RadioGroup rgSMRorNot;
    CheckBox cbValidate;
    Dialog dialog;


    // Values selected in Dialog box
    boolean isSmr; // Whether App key is SMR enabled.
    private boolean validateKeySecret; // Whether to do simple validation for App Key and Secret.

    public RecyclerViewAdapter(Context context, List<KeyInfo> keys, Activity activity, ConfigKeyFragment configKeyFragment) {
        this.context = context;
        this.activity = activity;
        keyInfoListConfig = keys;
        this.configKeyFragment = configKeyFragment;
    }

    @Override
    public rvViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View contactView = inflater.inflate(R.layout.recyclerview_items, parent, false);
        return new rvViewHolder(contactView);
    }

    @Override
    public void onBindViewHolder(rvViewHolder holder, int position) {
        final int pos = position;
        listCardLayout = holder.listCardView;
        final TextView tvKey = holder.tvKey;
        TextView tvDesc = holder.tvDesc;
        ImageView imgEdit = holder.imgEdit;
        ImageView imgDelete = holder.imgDelete;
        RadioButton rbSelect = holder.rbSelect;

        // Populate the UI with info from keyInfoListConfig based on position in the list.
        tvKey.setText(keyInfoListConfig.get(pos).getKey());
        tvDesc.setText(keyInfoListConfig.get(pos).getDesc());

        // The first spot is for the default key obtained from config.xml
        // Do not allow it to be changed through the UI.
        if (pos == 0) {
            holder.itemView.setSelected(true);
            imgEdit.setVisibility(View.INVISIBLE);
            imgDelete.setVisibility(View.INVISIBLE);
        }

        // On setting as the selected App key, update Config and SharePreferences.
        rbSelect.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                processSelectKeyOption(pos);
                rbSelect.setChecked(true);
                return true;
            }
        });

        listCardLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processSelectKeyOption(pos);
                rbSelect.setChecked(true);
            }
        });

        tvKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processSelectKeyOption(pos);
                rbSelect.setChecked(true);
            }
        });

        tvDesc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processSelectKeyOption(pos);
                rbSelect.setChecked(true);
            }
        });

        imgEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editKey(pos);
            }
        });

        imgDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        context);
                alertDialogBuilder.setTitle("Alert");
                alertDialogBuilder
                        .setMessage("Delete this App Key?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                KeyInfo keyInfoOld = keyInfoListConfig.get(pos);
                                boolean wasSmr = keyInfoOld.isSmr();
                                deleteAppKey(pos, wasSmr, activity);
                                // Update the UI for changes to keyInfo.
                                keyInfoListConfig.remove(pos);
                                notifyDataSetChanged();
                                setSelectedKeyViews();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });
    }

    public void editKey(int position) {
        final int pos = position;
        getDialogBox(pos);
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

                // KeyInfo before edits from SharePreferences.
                final KeyInfo keyInfoOld = keyInfoListConfig.get(pos);
                boolean wasSmr = keyInfoOld.isSmr();

                String appKeyNew = keyText.getText().toString();
                String appKeySecretNew = secretText.getText().toString();
                String appKeyDescNew = descText.getText().toString();
                KeyInfo keyInfoNew =
                        new KeyInfo(appKeyNew, appKeySecretNew, appKeyDescNew, isSmr);

                if (validateKeySecret &&
                        !checkAppKeyAndSecret(appKeyNew, appKeySecretNew)) {
                    String log = "Incorrect key, secret and/or you need to choose SMR or not";
                    toastLogLong(TAG, context, log);
                    return;
                }
                String log = "[editKey] App key edit ";
                if (ConfigKeyFragment.editAppKey(keyInfoNew, pos, wasSmr, activity)) {

                    // Update the keyInfoList currently on display.
                    configKeyFragment.setRbAppKeyListSmr(isSmr);
                    if (isSmr) {
                        keyInfoListConfig = Utils.convertJSONArrayToKeyInfoList(appKeyListSmr);
                    } else {
                        keyInfoListConfig = Utils.convertJSONArrayToKeyInfoList(appKeyListNoSmr);
                    }

                    // Update the UI for changes to keyInfo.
                    notifyDataSetChanged();
                    setSelectedKeyViews();

                    log += "succeeded.";
                } else {
                    log += "failed! Discarding edit.";
                }
                toastLog(TAG, context, log);
                Log.d(TAG, log);

            }
        });
    }

    public void getDialogBox(int position) {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_key_info);
        dialog.setTitle("Edit Key");
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

        rbSMR = (RadioButton) dialog.findViewById(R.id.radioSmr);
        rbNoSMR = (RadioButton) dialog.findViewById(R.id.radioNoSmr);
        rgSMRorNot = (RadioGroup) dialog.findViewById(R.id.radio_grp_manage);
        // Set default values
        final KeyInfo keyInfo = keyInfoListConfig.get(position);
        keyText.setText(keyInfo.getKey());
        secretText.setText(keyInfo.getSecret());
        descText.setText(keyInfo.getDesc());

        // Register changes in SMR status.
        rgSMRorNot.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == rbSMR.getId()) isSmr = true;
                else if (checkedId == rbNoSMR.getId()) isSmr = false;
            }
        });

        if (keyInfo.isSmr()) {
            rgSMRorNot.check(rbSMR.getId());
        } else {
            rgSMRorNot.check(rbNoSMR.getId());
        }

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

    @Override
    public int getItemCount() {
        return keyInfoListConfig.size();
    }

    public class rvViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        CardView listCardView;
        TextView tvKey;
        TextView tvDesc;
        ImageView imgEdit;
        ImageView imgDelete;
        RadioButton rbSelect;
        RadioGroup rgDefault;

        public rvViewHolder(View itemView) {
            super(itemView);
            View v = itemView;
            listCardView = (CardView) v.findViewById(R.id.list_card_layout);
            tvKey = (TextView) v.findViewById(R.id.subTextview);
            tvDesc = (TextView) v.findViewById(R.id.descTextview);
            // When Click Edit Button
            imgEdit = (ImageView) v.findViewById(R.id.editImage);
            imgDelete = (ImageView) v.findViewById(R.id.deleteImage);
            rbSelect = (RadioButton) v.findViewById(R.id.rbDefault);
            rgDefault = (RadioGroup) v.findViewById(R.id.rgDefault);
            // Setup the click listener
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

        }
    }

    public void processSelectKeyOption(int pos) {
        try {
            JSONObject selectedItem;
            boolean isSMR = ConfigKeyFragment.smrSelect;
            if (isSMR) {
                selectedItem = appKeyListSmr.getJSONObject(pos);
            } else {
                selectedItem = appKeyListNoSmr.getJSONObject(pos);
            }
            Config.setAppKey(KeyInfo.getKey(selectedItem), activity);
            Config.setAppKeySecret(KeyInfo.getSecret(selectedItem), activity);
            Config.setAppKeyDescription(KeyInfo.getDesc(selectedItem), activity);
            Config.setAppKeySmr(isSMR, activity);
            // Update UI
            ConfigKeyFragment.setSelectedKeyViews();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}


