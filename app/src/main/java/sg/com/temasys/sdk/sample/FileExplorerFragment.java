package sg.com.temasys.sdk.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.temasys.skylink.sample.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class FileExplorerFragment extends ListFragment {

    final static private String BUNDLE_TID = "tools.skylink.sample.FileExplorerFragment.tid";
    final static private String BUNDLE_IS_PRIVATE =
            "tools.skylink.sample.FileExplorerFragment.isPrivate";
    final static private String BUNDLE_OPERATION =
            "tools.skylink.sample.FileExplorerFragment.operation";
    final static private String BUNDLE_SAVE_FILE_NAME =
            "tools.skylink.sample.FileExplorerFragment.saveFileName";
    final static private String BUNDLE_DIR_CUR = "tools.skylink.sample.FileExplorerFragment.dirCur";
    final static private String BUNDLE_FILE_NAME_CUR =
            "tools.skylink.sample.FileExplorerFragment.fileNameCur";

    // Parent activity, set at onAttach.
    protected RoomViewActivity parentActivity;

    // Instantiate views
    // The root view of FileExplorerFragment
    // Set at onCreateView.
    private View rootView;
    public LinearLayout feFragmentVG;

    // private static String TAG = "FileExplorerFragment";

    // List of operation types
    public enum Ops {
        NONE, SAVE1, SAVE2, SEND
    }

    private List<String> fileNameList = null;
    private List<String> filePathList = null;
    private TextView txtVwLabTarget;
    private TextView txtVwLabPath;
    private TextView txtVwLabFile;
    private TextView txtVwCurPath;
    private EditText edtVwCurFile;
    private Button btnSelect;
    private Button btnCancel;
    private Ops operation = Ops.NONE;
    private String root;
    private String fileNameCur = "";
    private String dirCur = "";
    private String filePath = "";
    private LayoutInflater inflater;

    private String saveFileName = "";
    private String tid = "";
    private boolean isPrivate;

    // Default constructor.
    public FileExplorerFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            tid = savedInstanceState.getString(BUNDLE_TID);
            isPrivate = savedInstanceState.getBoolean(BUNDLE_IS_PRIVATE);
            operation = (Ops) savedInstanceState.getSerializable(BUNDLE_OPERATION);
            saveFileName = savedInstanceState.getString(BUNDLE_SAVE_FILE_NAME);
            dirCur = savedInstanceState.getString(BUNDLE_DIR_CUR);
            fileNameCur = savedInstanceState.getString(BUNDLE_FILE_NAME_CUR);
            filePath = dirCur + "/" + fileNameCur;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fe_list, container, false);

        this.inflater = inflater;

        // Get Views
        feFragmentVG = (LinearLayout) rootView.findViewById(R.id.linearLayoutFE);
        txtVwLabTarget = (TextView) rootView.findViewById(R.id.textViewLabTarget);
        txtVwLabPath = (TextView) rootView.findViewById(R.id.textViewLabPath);
        txtVwLabFile = (TextView) rootView.findViewById(R.id.textViewLabFile);
        txtVwCurPath = (TextView) rootView.findViewById(R.id.textViewCurPath);
        edtVwCurFile = (EditText) rootView.findViewById(R.id.editTextCurFile);
        btnSelect = (Button) rootView.findViewById(R.id.buttonSelect);
        btnCancel = (Button) rootView.findViewById(R.id.buttonCancel);

        // Set starting location as SD card directory.
        root = Environment.getExternalStorageDirectory().getPath();
        // If previous directory value exist, start from that, else start from root.
        if (dirCur.equals("")) dirCur = root;
        getDir(dirCur);

        // Set View properties
        // Set specific contents.
        setTarget(tid, isPrivate, operation);
        setUI();

        txtVwLabPath.setText(getString(R.string.label_file_path));
        txtVwLabFile.setText(getString(R.string.label_file_file));
        btnCancel.setText(getString(R.string.label_file_button_cancel));
      /*// Set background colours
        // Format: Hexadecimal AARRGGBB each value can be from 0-255 (0-F).
        // Set background colours
          // 20% white background.
    txtVwLabPath.setBackgroundColor( 0X33FFFFFF );
    txtVwLabFile.setBackgroundColor( 0X33FFFFFF );
    txtVwCurPath.setBackgroundColor( 0X33FFFFFF );
    edtVwCurFile.setBackgroundColor( 0X33FFFFFF );
          // Set 100% Black text.
    txtVwLabPath.setTextColor( 0XFF000000 );
    txtVwLabFile.setTextColor( 0XFF000000 );
    txtVwCurPath.setTextColor( 0XFF000000 );
    edtVwCurFile.setTextColor( 0XFF000000 );*/

        btnSelect.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        if (operation == Ops.SEND) {
                            // Send a file from File Explorer if selection is a file.
                            File file = new File(filePath);
                            if (file.isFile()) {
                                String fileName = file.getName();
                                // Send a DC file.
                                // Trigger callback.
                                feDidClickSend(fileName, filePath, tid, isPrivate);
                                // Reset FE state.
                                operation = Ops.NONE;
                            } else {
                                feDidSendNonFile();
                            }
                        } else if (operation == Ops.SAVE1 || operation == Ops.SAVE2) {
                            // Save a file in given Path.
                            // Check if edtVwCurFile is populated.
                            if (edtVwCurFile.getText().equals("")) {
                                // If not, alert user to input fileName.
                                new AlertDialog.Builder(parentActivity)
                                        .setTitle(getString(R.string.message_file_save_no_name))
                                        .setPositiveButton(getString(R.string.label_file_button_ok),
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        // Nothing to do, just return to UI.
                                                    }
                                                }).show();
                                return;
                            } else {
                                // Set filePath
                                filePath = txtVwCurPath.getText() + "/" + edtVwCurFile.getText();
                                // Trigger callback.
                                feDidClickSave(tid, filePath, saveFileName);
                                // Reset FE state.
                                operation = Ops.NONE;
                            }
                        }
                    }
                }
        );

        btnCancel.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        if (operation == Ops.SEND) {
                            // Exit from File Explorer.
                            // Trigger callback
                            feDidCancelSend();
                            // Set FE states
                            operation = Ops.NONE;
                        } else if (operation == Ops.SAVE2) {
                            // Decline file.
                            // Trigger callback
                            feDidCancelSave(saveFileName, tid);
                            // Set FE states
                            operation = Ops.NONE;
                        }
                    }
                }
        );
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        parentActivity = (RoomViewActivity) activity;
    }

    @Override
    public void onPause() {
        super.onPause();
        // Save View values (if any)
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    class FeArrayAdapter extends ArrayAdapter<String> {
        List<String> list;

        FeArrayAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
            list = objects;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = inflater.inflate(R.layout.fe_row, parent, false);
            TextView txtVwRow = (TextView) rowView.findViewById(R.id.textViewRow);
            txtVwRow.setText(list.get(position));
      /*// Set background colours
        // Format: Hexadecimal AARRGGBB each value can be from 0-255 (0-F).
        // Set background colours
          // 20% white background.
      txtVwRow.setBackgroundColor( 0X33FFFFFF );
          // Set 100% Black text.
      txtVwRow.setTextColor( 0XFF000000 );*/
            return (rowView);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        File file = new File(filePathList.get(position));
        if (file.isDirectory()) {
            if (file.canRead()) {
                dirCur = filePathList.get(position);
                txtVwCurPath.setText(dirCur);
                // For Ops.SEND operation only, clear previous file name when directory selected.
                if (operation == Ops.SEND) edtVwCurFile.setText("");
                getDir(dirCur);
            } else {
                new AlertDialog.Builder(parentActivity)
                        // .setIcon( R.drawable.temasys )
                        .setTitle(
                                String.format(getString(R.string.message_file_folder_cant_read), file.getName()))
                        .setPositiveButton(getString(R.string.label_file_button_ok),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // TODO Auto-generated method stub
                                    }
                                }).show();
            }
        } else {
            filePath = filePathList.get(position);
            String fileDirPath = file.getParent();
            fileNameCur = file.getName();
            txtVwCurPath.setText(fileDirPath);
            edtVwCurFile.setText(fileNameCur);
        }
    }

    // Get methods
    public Ops getOperation() {
        return operation;
    }

    public boolean getIsPrivate() {
        return isPrivate;
    }

    public String getTid() {
        return tid;
    }

    public String getSaveFileName() {
        return saveFileName;
    }


    // Set methods
    public void setOperation(Ops operation) {
        this.operation = operation;
    }

    public void setIsPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public void setSaveFileName(String saveFileName) {
        this.saveFileName = saveFileName;
    }

// -------------------------------------------------------------------------------------------------
// Set UI methods
// -------------------------------------------------------------------------------------------------

    public boolean setTarget(String tid, boolean isPrivate, Ops operation) {
        String nick = "";
        if (isPrivate) nick = RoomManager.get().getDisplayName(tid);
        if (nick == null) return false;

        setTid(tid);
        setIsPrivate(isPrivate);
        setOperation(operation);
        if (txtVwLabTarget == null) return false;

        // Set file target
        switch (operation) {
            case SAVE1:
            case SAVE2:
                if (isPrivate)
                    txtVwLabTarget.setText(String.format(
                            getString(R.string.title_file_save_private), nick, saveFileName));
                else
                    txtVwLabTarget.setText(String.format(
                            getString(R.string.title_file_save_group), nick, saveFileName));
                break;
            case SEND:
                if (isPrivate)
                    txtVwLabTarget.setText(String.format(
                            getString(R.string.title_file_send_private), nick));
                else
                    txtVwLabTarget.setText(getString(R.string.title_file_send_group));
                break;
            default:
                break;
        }
        return true;
    }

    // Set UI according to operation.
    public void setUI() {
        // Set current directory and file name if there are values in record.
        if (!dirCur.equals("")) txtVwCurPath.setText(dirCur);
        if (!fileNameCur.equals("")) edtVwCurFile.setText(fileNameCur);

        // Set Operation specific labels.
        switch (operation) {
            case NONE:
                // Reset values to default case.
                tid = null;
                isPrivate = false;
                saveFileName = "";
                dirCur = "";
                fileNameCur = "";
                break;
            case SEND:
                setSendFile();
                break;
            case SAVE1:
                setSaveFile1();
                break;
            case SAVE2:
                setSaveFile2();
                break;
            default:
                break;
        }
    }

    // Start Send file dialogue.
    private void setSendFile() {
        parentActivity.runOnUiThread(new Runnable() {
            public void run() {
                btnSelect.setText("Send file.");
            }
        });
    }

    // Start Save file 1 UI - Ask if accept save file.
    private void setSaveFile1() {
        // Set UI strings.
        String nick = RoomManager.get().getDisplayName(tid);
        // If Peer disconnected before we process this request,
        // do nothing and wait to be redirected to videoUI.
        if (nick == null) return;

        String strShareTemp =
                String.format(
                        getString(R.string.message_file_request_group), nick, saveFileName);
        if (isPrivate) strShareTemp =
                String.format(
                        getString(R.string.message_file_request_private), nick, saveFileName);

        final String strShare = strShareTemp;

        parentActivity.runOnUiThread(new Runnable() {
            public void run() {
                new AlertDialog.Builder(parentActivity)
                        .setTitle(strShare)
                        .setPositiveButton(getString(R.string.label_file_request_button_pos),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        setSaveFile2();
                                        // Set Views.
                                        edtVwCurFile.setText(saveFileName);
                                        btnSelect.setText(getString(R.string.label_file_request_button_save));
                                        // Trigger callback.
                                        feDidAcceptSave(operation, tid, isPrivate, saveFileName);
                                    }
                                })
                        .setNegativeButton(getString(R.string.label_file_request_button_neg),
                                new DialogInterface.OnClickListener() {
                                    // Set FE states.
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        FileExplorerFragment.this.setOperation(Ops.NONE);
                                        // Trigger callback.
                                        feDidDeclineSave(saveFileName, tid);
                                        FileExplorerFragment.this.saveFileName = "";
                                    }
                                }).show();
            }
        });
    }

    // Start Save file 2 UI - Select location to save file.
    private void setSaveFile2() {
        operation = Ops.SAVE2;
        // Auto populate save file name.
        edtVwCurFile.setText(saveFileName);
        btnSelect.setText(getString(R.string.label_file_request_button_save));
    }

// -------------------------------------------------------------------------------------------------
// File Explorer callbacks
// -------------------------------------------------------------------------------------------------

    // After deciding to Save file.
    public abstract void feDidAcceptSave(
            Ops operation, String tid, boolean isPrivate, String fileName);

    // After clicking cancel, in the sendFileDialogue.
    public abstract void feDidCancelSend();

    // After clicking cancel, after clicking accept Save file.
    public abstract void feDidCancelSave(String saveFileName, String tid);

    // After deciding to not Save file.
    public abstract void feDidDeclineSave(String saveFileName, String tid);

    // After clicking Send file button when a non-file is selected.
    public abstract void feDidSendNonFile();

    // After clicking Send file button.
    public abstract void feDidClickSend(
            String fileName, String filePath, String tid, boolean isPrivate);

    // After clicking Save file button.
    public abstract void feDidClickSave(String tid, String filePath, String saveFileName);

// -------------------------------------------------------------------------------------------------
// Helper methods
// -------------------------------------------------------------------------------------------------

    // Display the contents of a directory
    private void getDir(String dirPath) {
        fileNameList = new ArrayList<String>();
        filePathList = new ArrayList<String>();
        List<String> fileNameListTemp = new ArrayList<String>();
        List<String> filePathListTemp = new ArrayList<String>();

        File f = new File(dirPath);
        File[] files = f.listFiles();

        if (!dirPath.equals(root)) {
            fileNameList.add(root);
            filePathList.add(root);
            fileNameList.add("../");
            filePathList.add(f.getParent());
        }

        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            filePathListTemp.add(file.getPath());
            if (file.isDirectory())
                fileNameListTemp.add(file.getName() + "/");
            else
                fileNameListTemp.add(file.getName());
        }

        // Sort file names
        Collections.sort(fileNameListTemp);
        Collections.sort(filePathListTemp);
        // Assemble sorted file names temp array into actual array.
        fileNameList.addAll(fileNameListTemp);
        filePathList.addAll(filePathListTemp);

        FeArrayAdapter fileList = new FeArrayAdapter(parentActivity, R.layout.fe_row, fileNameList);
        setListAdapter(fileList);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_TID, tid);
        outState.putBoolean(BUNDLE_IS_PRIVATE, isPrivate);
        outState.putSerializable(BUNDLE_OPERATION, operation);
        outState.putString(BUNDLE_SAVE_FILE_NAME, saveFileName);
        outState.putString(BUNDLE_DIR_CUR, dirCur);
        outState.putString(BUNDLE_FILE_NAME_CUR, fileNameCur);
    }

}