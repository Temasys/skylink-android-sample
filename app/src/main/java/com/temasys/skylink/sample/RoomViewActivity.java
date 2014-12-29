package com.temasys.skylink.sample;

import java.io.File;
import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.temasys.skylink.rtc.SkyLinkConnection;
import com.temasys.skylink.rtc.SkyLinkConnection.SkyLinkConfig;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.ipaulpro.afilechooser.utils.FileUtils;
import com.warting.bubbles.DiscussArrayAdapter;
import com.warting.bubbles.OneComment;

public class RoomViewActivity extends Activity implements
		SkyLinkConnection.LifeCycleDelegate,
		SkyLinkConnection.RemotePeerDelegate, SkyLinkConnection.MediaDelegate,
		SkyLinkConnection.MessagesDelegate,
		SkyLinkConnection.FileTransferDelegate {

	final static private String TAG = "RoomViewActivity";

	final static private String BUNDLE_IS_CONNECTED = "com.temasys.skylink.sample.RoomViewActivity.isConnected";
	final static private String BUNDLE_CONNECTION_CONFIG = "com.temasys.skylink.sample.RoomViewActivity.connectionConfig";
	final static private String BUNDLE_CONTROL_PANEL_WEIGHT = "com.temasys.skylink.sample.RoomViewActivity.controlPanelWeight";
	final static private String BUNDLE_IS_RUNNING = "com.temasys.skylink.sample.RoomViewActivity.isRunning";
	final static public String EXTRA_DISPLAY_NAME = "com.temasys.skylink.sample.RoomViewActiivty.displayName";
	final static public String EXTRA_ROOM_NAME = "com.temasys.skylink.sample.RoomViewActivity.roomName";

	private boolean mIsExplicitlyTerminated = false;

	private boolean mIsAlreadyConnected = false;
	private SkyLinkConfig mConnectionConfig;

	private String mDisplayName;
  private String mRoomName;
  private boolean curDropped = false;
	private String cancelMessage;

	private SkyLinkConnection mConnection;

	private float mCpWeight = 0.25f;

	private boolean mIsRunning = false;

  // Audio
  private HeadSetReceiver mHeadSetReceiver;
  private IntentFilter headSetFilter;
  private MediaPlayer mMediaPlayer;

  private class HeadSetReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
      if( intent.getAction().equals( Intent.ACTION_HEADSET_PLUG ) ) {
        int state = intent.getIntExtra( "state", -1 );
        switch( state ) {
        case 0:
          Log.d( TAG, "Headset: Unplugged" );
          break;
        case 1:
          Log.d( TAG, "Headset: Plugged" );
          break;
        default:
          Log.d( TAG, "Headset: Error determining state!" );
        }
        // Reset audio path
        setAudioPath();
      }
    }
  }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			mIsAlreadyConnected = savedInstanceState.getBoolean(
					BUNDLE_IS_CONNECTED, false);
			mConnectionConfig = (SkyLinkConfig) savedInstanceState
					.get(BUNDLE_CONNECTION_CONFIG);
			mCpWeight = savedInstanceState
					.getFloat(BUNDLE_CONTROL_PANEL_WEIGHT);
			mIsRunning = savedInstanceState.getBoolean(BUNDLE_IS_RUNNING);
		}
		mDisplayName = getIntent().getStringExtra(EXTRA_DISPLAY_NAME);
		mRoomName = getIntent().getStringExtra(EXTRA_ROOM_NAME);

		/*AudioManager audioManager = ((AudioManager) getSystemService(android.content.Context.AUDIO_SERVICE));

		@SuppressWarnings("deprecation")
		boolean isWiredHeadsetOn = audioManager.isWiredHeadsetOn();
		audioManager.setMode(isWiredHeadsetOn ? AudioManager.MODE_IN_CALL
				: AudioManager.MODE_IN_COMMUNICATION);
		audioManager.setSpeakerphoneOn(!isWiredHeadsetOn);*/



		if (!mIsAlreadyConnected) {
			SkyLinkConfig config = new SkyLinkConnection.SkyLinkConfig();
			config.setHasAudio(true);
			config.setHasVideo(true);
			config.setHasPeerMessaging(true);
			config.setHasFileTransfer(true);
      config.setTimeout( 60 );
			// config.setTimeout( 3 );
			mConnection = new SkyLinkConnection(getString(R.string.app_key),
					getString(R.string.app_secret), config, this);
			mConnectionConfig = config;
			RoomManager.getInstance(mConnection);
		} else {
      // New Activity has been generated, so reset the delegates to new activity.
			mConnection = RoomManager.get().getConnection();
			mConnection.resetContext(this);
		}

		mConnection.setFileTransferDelegate(this);
		mConnection.setLifeCycleDelegate(this);
		mConnection.setMediaDelegate(this);
		mConnection.setMessagesDelegate(this);
		mConnection.setRemotePeerDelegate(this);

		LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rootView = layoutInflater.inflate(R.layout.activity_room_view,
				null);
		View view = rootView.findViewById(R.id.control_panel_container);
		LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
		layoutParams.weight = mCpWeight;
		view.setLayoutParams(layoutParams);

		setContentView(rootView);
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		try {
			/*
			 * findViewById(R.id.split_container).setBackgroundColor(Color.BLUE);
			 * findViewById(R.id.control_panel_container).setBackgroundColor(
			 * Color.YELLOW);
			 */
			if (!mIsAlreadyConnected) {
				JSONObject userData = new JSONObject();
				userData.put("displayName", mDisplayName);
				RoomManager.get().setMyDisplayName(mDisplayName);
				mConnection.connectToRoom(mRoomName, userData, new Date(), 200);
			}
		} catch (JSONException e) {
			Log.w(TAG, e.getLocalizedMessage(), e);
		} catch (SignatureException e) {
			Log.w(TAG, e.getLocalizedMessage(), e);
		} catch (IOException e) {
			Log.w(TAG, e.getLocalizedMessage(), e);
		}

    // Prepare to set audio path
    mHeadSetReceiver = new HeadSetReceiver();
    headSetFilter = new IntentFilter( Intent.ACTION_HEADSET_PLUG );
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mIsRunning = true;
		if (mConnection != null)
			mConnection.onResume();
		if (RoomManager.get().isSplitChanged()) {
			setVideoUIFromRoomManager();
			RoomManager.get().setSplitChanged(false);
		}
    // Get ready to set audio path when headset state changes.
    registerReceiver( mHeadSetReceiver, headSetFilter );
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mIsRunning = false;
		if (mConnection != null)
			mConnection.onPause();
    // Do not set audio path when app is not forefront.
    unregisterReceiver(mHeadSetReceiver);
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(BUNDLE_IS_CONNECTED, mIsAlreadyConnected);
		outState.putSerializable(BUNDLE_CONNECTION_CONFIG, mConnectionConfig);
		outState.putFloat(BUNDLE_CONTROL_PANEL_WEIGHT, mCpWeight);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = fragmentManager.findFragmentById( R.id.split_container );
		if (RoomFragment.class.isAssignableFrom(fragment.getClass())) {
			super.onBackPressed();
			mIsExplicitlyTerminated = true;
			RoomManager.get().destroy();
			finish();
		} else {
      // Create the video UI state recorded in RoomManager.
      setVideoUIFromRoomManager();
    }
  }

// -------------------------------------------------------------------------------------------------
// LifeCycleDelegate callbacks
// -------------------------------------------------------------------------------------------------
	@Override
	public void onConnect(boolean isSuccess, String message) {
		// TODO Auto-generated method stub
		if (!isSuccess) {
			Log.d(TAG, "onConnect()::message->" + message);
			Intent intent = new Intent();
			intent.putExtra(JoinRoomActivity.EXTRA_RESULT_DISCONNECT_STATUS,
					JoinRoomActivity.DisconnectStatus.ON_CONNECT_FALSE);
			setResult(RESULT_OK, intent);
			RoomManager.get().destroy();
			finish();
		} else {
			mIsAlreadyConnected = true;
		}
	}

	@Override
	public void onGetUserMedia(GLSurfaceView videoView, Point size) {
		// TODO Auto-generated method stub
		RoomManager.get().putVideo(null, videoView);
		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = fragmentManager
				.findFragmentById(R.id.split_container);
		if (fragment == null) {
			fragment = new SelfVideoFragment();
			fragmentManager.beginTransaction()
					.add(R.id.split_container, fragment).commit();
			RoomManager.get().setSplitFragmentClass(fragment.getClass());
		}
	}

	@Override
	public void onWarning(String message) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onWarning()::message->" + message);
	}

	@Override
	public void onDisconnect(String message) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onDisconnect()::message->" + message);
		if (!mIsExplicitlyTerminated) {
			Intent intent = new Intent();
			intent.putExtra(JoinRoomActivity.EXTRA_RESULT_DISCONNECT_STATUS,
					JoinRoomActivity.DisconnectStatus.ON_CONNECT_FALSE);
			setResult(RESULT_OK, intent);
			RoomManager.get().destroy();
			finish();
		}
	}

	@Override
	public void onReceiveLog(String message) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onReceiveLog()::message->" + message);
	}

// -------------------------------------------------------------------------------------------------
// RemotePeerDelegate callbacks
// -------------------------------------------------------------------------------------------------
  @Override
  public void onUserData(String peerId, Object userData) {
    // TODO Auto-generated method stub
    String oldNick = RoomManager.get().getDisplayName(peerId);
    RoomManager.get().putDisplayName(peerId, (String) userData);
    Utility.showShortToast(getApplicationContext(),
        R.string.message_on_user_data, oldNick, (String) userData);
  }

  @Override
  public void onOpenDataConnection(String peerId) {
    // TODO Auto-generated method stub
    Log.d(TAG, "onOpenDataConnection()::peerId->" + peerId);
  }

	@Override
	public void onPeerJoin(String peerId, Object userData) {
    String userDataStr = getDisplayNameFromUserData( userData );
    RoomManager.get().putDisplayName( peerId, userDataStr );
		Utility.showShortToast(getApplicationContext(), R.string.message_on_peer_join, userDataStr );
	}

	@Override
	public void onGetPeerMedia(String peerId, GLSurfaceView videoView,
			Point size) {
		RoomManager.get().putVideo(peerId, videoView);
		// Set video UI either immediately or record the video UI required.
		if (mIsRunning)
			setVideoUI();
		else
			recordVideoUI();
	}

	@Override
	public void onPeerLeave(String peerId, String message) {
		Utility.showShortToast(getApplicationContext(),
				R.string.message_on_peer_leave, RoomManager.get().getDisplayName(peerId));
		RoomManager.get().removePeer(peerId);
    // Set video UI either immediately or record the video UI required.
    
		if (mIsRunning) {
			
    setVideoUI();

    // If we are currently in private chat or File UI with this Peer, or if this was the last peer,
      // return to video UI.
      // Check if fragment occupying split_container is a ChatFragment
    // For Chat
    Fragment fragment = getFragmentManager().findFragmentById(R.id.split_container);
    if( ChatFragment.class.isInstance( fragment ) ) {
      int totalPeerVideos = RoomManager.get().getRemoteVideoList().size();
      // Check if there are more peers
      if( totalPeerVideos > 0 ) {
        // and we are not having private chat with this Peer
        String chatTid = ( ( ChatFragment ) fragment ).getTid();
        if( chatTid == null || !chatTid.equals( peerId ) )
          // Do nothing special.
          return;
      }
      // Otherwise, exit from chat and go to video UI.
      setVideoUIFromRoomManager();
    }
    // For File
    if( FeFragment.class.isInstance( fragment ) ) {
      int totalPeerVideos = RoomManager.get().getRemoteVideoList().size();
      // Check if there are more peers
      if( totalPeerVideos > 0 ) {
        // and we are not having a file share process with this Peer
        String fileTid = ( ( FeFragment ) fragment ).getTid();
        if( fileTid == null || !fileTid.equals( peerId ) )
          // Do nothing special.
          return;
      }
      // Otherwise, exit from file and go to video UI.
      setVideoUIFromRoomManager();
    }
    
		} else {
			recordVideoUI();
		}
		
  }

// -------------------------------------------------------------------------------------------------
// MediaDelegate callbacks
// -------------------------------------------------------------------------------------------------
	@Override
	public void onVideoSize(GLSurfaceView videoView, Point size) {
		// TODO Auto-generated method stub
		RoomManager manager = RoomManager.get();
		if (manager != null) {
			RoomManager.get().putSize(videoView, size);
			Utility.layoutSubviews(videoView, size);
		}
	}

	@Override
	public void onToggleAudio(String peerId, boolean isMuted) {
		// TODO Auto-generated method stub
		int messageId = isMuted ? R.string.message_on_audio_muted
				: R.string.message_on_audio_unmuted;
		Utility.showShortToast(getApplicationContext(), messageId, RoomManager
				.get().getDisplayName(peerId));
	}

	@Override
	public void onToggleVideo(String peerId, boolean isMuted) {
		// TODO Auto-generated method stub
		int messageId = isMuted ? R.string.message_on_video_muted
				: R.string.message_on_video_unmuted;
		Utility.showShortToast(getApplicationContext(), messageId, RoomManager
				.get().getDisplayName(peerId));
	}

// -------------------------------------------------------------------------------------------------
// Helper methods
// -------------------------------------------------------------------------------------------------
  public String getDisplayNameFromUserData( Object userData ) {
    String displayName = "";
    if ( JSONObject.class.isAssignableFrom( userData.getClass() ) ) {
      try {
        displayName = ( ( JSONObject ) userData ).getString( "displayName" );
      } catch( JSONException e ) {
        Log.w(TAG, e.getLocalizedMessage(), e);
      }
    } else {
      displayName = ( String ) userData;
    }
    return displayName;    
  }

  // Set the audio path according to whether earphone is connected.
    // Use ear piece if earphone is connected.
    // Use speakerphone if no earphone is connected.
  private void setAudioPath() {
    AudioManager audioManager = 
      ( ( AudioManager ) getSystemService( android.content.Context.AUDIO_SERVICE ) );
    boolean isWiredHeadsetOn = audioManager.isWiredHeadsetOn();
    mMediaPlayer = new MediaPlayer();
    if( isWiredHeadsetOn ) {
      mMediaPlayer.setAudioStreamType( AudioManager.STREAM_VOICE_CALL );
      audioManager.setSpeakerphoneOn( false );
    } else {
      mMediaPlayer.setAudioStreamType( AudioManager.STREAM_MUSIC );
      audioManager.setSpeakerphoneOn( true );
    }
  }

  public void setVideoUI() {
    int totalPeerVideos = RoomManager.get().getRemoteVideoList().size();
    FragmentManager fragmentManager = getFragmentManager();
    Fragment fragmentOld = fragmentManager.findFragmentById(R.id.split_container);
    Fragment fragment = fragmentManager.findFragmentById(R.id.split_container);

    switch ( totalPeerVideos ) {
    case 0: {
      fragment = new SelfVideoFragment();
      // Remove control panel, if exists.
      setControlPanel( false );
      // Set audio and video to none muted.
        // NOTE XR: Are we sure we want to do this?
      RoomManager.get().getConnection().muteAudio(false);
      RoomManager.get().getConnection().muteVideo(false);
    }
      break;
    case 1: {
      fragment = new Split1Fragment();
      // Add or refresh control panel.
      setControlPanel( false );
    }
      break;
    case 2: {
      fragment = new Split2Fragment();
    }
      break;
    case 3: {
      fragment = new Split3Fragment();
    }
      break;
    case 4: {
      fragment = new Split4Fragment();
    }
      break;
    default:
      break;
    }

    // If this is first start, or currently in Video mode,
      // directly change the video fragment used.
    if ( fragmentOld == null || RoomFragment.class.isAssignableFrom( fragmentOld.getClass() ) ) {
      // Replace previous video fragment with current video fragment.
      fragmentManager.beginTransaction().replace( R.id.split_container, fragment ).commit();
    } else {
      // If this is in non-video fragment UI (e.g. chat, file transfer), simply set record below.
    }
    // Record the type of video UI that should be present.
    RoomManager.get().setSplitFragmentClass(fragment.getClass());
  }

	private void recordVideoUI() {
		int totalPeerVideos = RoomManager.get().getRemoteVideoList().size();
		switch (totalPeerVideos) {
		case 0:
			RoomManager.get().setSplitFragmentClass(SelfVideoFragment.class);
			break;
		case 1:
			RoomManager.get().setSplitFragmentClass(Split1Fragment.class);
			break;
		case 2:
			RoomManager.get().setSplitFragmentClass(Split2Fragment.class);
			break;
		case 3:
			RoomManager.get().setSplitFragmentClass(Split3Fragment.class);
			break;
		case 4:
			RoomManager.get().setSplitFragmentClass(Split4Fragment.class);
			break;
		default:
			break;
		}
		RoomManager.get().setSplitChanged(true);
	}
  
  // Create the video UI state recorded in RoomManager.
  public void setVideoUIFromRoomManager() {
    try {
      // Show control panel
      View view = (ViewGroup) findViewById(R.id.control_panel_container);
      LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
      layoutParams.weight = mCpWeight = 0.25f;
      view.setLayoutParams(layoutParams);
      // Refresh Control Panel contents if peer(s) exist.
      setControlPanel( true );
      // Show Video UI
      Fragment fragment = (Fragment) RoomManager.get().getSplitFragmentClass().newInstance();
      getFragmentManager().beginTransaction().replace(R.id.split_container, fragment).commit();
    } catch (InstantiationException e) {
      Log.w(TAG, e.getLocalizedMessage(), e);
    } catch (IllegalAccessException e) {
      Log.w(TAG, e.getLocalizedMessage(), e);
    }
  }

  // Set Control Panel contents if remote peers are present.
    // Set refresh to true if new control fragment is desired (even when one exists).
  public void setControlPanel( boolean refresh ) {
    int totalPeerVideos = RoomManager.get().getRemoteVideoList().size();
    FragmentManager fragmentManager = getFragmentManager();
    Fragment fragment = fragmentManager.findFragmentById(R.id.control_panel_container);

    if( totalPeerVideos == 0 ) {
      if( fragment != null ) fragmentManager.beginTransaction().remove(fragment).commit();
      return;
    } else {
      // If existing and no need to refresh, we are done.
      if( fragment != null && !refresh ) return;
      // Add new or refresh control panel.
      fragment = new ControlPanelFragment();
      fragmentManager.beginTransaction().replace(R.id.control_panel_container, fragment).commit();
    }
  }

// -------------------------------------------------------------------------------------------------
// MessagesDelegate callbacks
// -------------------------------------------------------------------------------------------------
	@Override
	@Deprecated
	public void onChatMessage( String peerId, String nick, String message, boolean isPrivate ) {
    String msgAlert = nick + ": ";
    if( isPrivate ) msgAlert +=  "[P2P] ";
    msgAlert += message.toString();
    // Add new chat to Video UI new chat textView
    addChatAlert( peerId, msgAlert, isPrivate );
    // Add message to chat UI if it is currently up, else add to chat container.
    addChatMsg( peerId, nick, message.toString(), isPrivate );
	}

	@Override
	public void onCustomMessage(String peerId, Object message, boolean isPrivate) {
    String nick = RoomManager.get().getDisplayName( peerId );
    String msgAlert = nick + ": ";
    if( isPrivate ) msgAlert +=  "[P2P] ";
    msgAlert += message.toString();
    // Add new chat to Video UI new chat textView
    addChatAlert( peerId, msgAlert, isPrivate );
    // Add message to chat UI if it is currently up, else add to chat container.
    addChatMsg( peerId, nick, message.toString(), isPrivate );
	}

	@Override
	public void onPeerMessage(String peerId, Object message, boolean isPrivate) {
    String nick = RoomManager.get().getDisplayName( peerId );
    String msgAlert = nick + ": ";
    if( isPrivate ) msgAlert +=  "[P2P] ";
    msgAlert += message.toString();
    // Add new chat to Video UI new chat textView
    addChatAlert( peerId, msgAlert, isPrivate );
    // Add message to chat UI if it is currently up, else add to chat container.
    addChatMsg( peerId, nick, message.toString(), isPrivate );
	}

// -------------------------------------------------------------------------------------------------
// FileTransferDelegate callbacks
// -------------------------------------------------------------------------------------------------
	@Override
  public void onRequest(String peerId, String fileName, boolean isPrivate ) {
    // Open file explorer in save mode.
    // setFileExplorer( FileExplorerFragment.Ops.SAVE1, peerId, isPrivate, fileName );
		String nick = RoomManager.get().getDisplayName(peerId);
		String strShareTemp = String.format(
				getString(R.string.message_file_request_group), nick, fileName);
		if (isPrivate)
			strShareTemp = String.format(
					getString(R.string.message_file_request_private), nick,
					fileName);
		/*FilePermissionAlertFragment.newInstance(strShareTemp, peerId, fileName)
				.show(RoomViewActivity.this.getFragmentManager(), TAG);*/

    // Put request into queue at last position
    String[] fileRequest = { strShareTemp, peerId, fileName };
    RoomManager.get().mFileRequestList.add( fileRequest );

    // Process request list.
    processFileRequest();
	}

	@Override
	public void onPermission(String peerId, String fileName, boolean isPermitted) {
    String nick = RoomManager.get().getDisplayName( peerId );
    if ( isPermitted ) {
      // Utility.showShortToast( this, R.string.message_permission_true, nick, fileName );
      String msg = String.format( getString( R.string.message_permission_true ), nick, fileName );
      AlertFragment.newInstance( msg ).show( RoomViewActivity.this.getFragmentManager(), TAG );
    } else {
      // Utility.showShortToast( this, R.string.message_permission_false, nick, fileName );
      String msg = String.format( getString( R.string.message_permission_false ), nick, fileName );
      AlertFragment.newInstance( msg ).show( RoomViewActivity.this.getFragmentManager(), TAG );
    }
	}

	@Override
	public void onDrop(String peerId, String fileName, String message, boolean isExplicit) {
	finishActivity(Utility.REQUEST_CODE_PICK_DIR);
    String msgAlert = "";
    String nick = RoomManager.get().getDisplayName( peerId );
    if (isExplicit) {
      // Utility.showRapidShortToast( this, R.string.message_drop_true, nick, fileName );
      msgAlert = String.format( getString( R.string.message_drop_true ), nick, fileName );
    } else {
      // Utility.showRapidShortToast( this, R.string.message_drop_false, nick, fileName, message );
      msgAlert = String.format( 
        getString( R.string.message_drop_false ), nick , fileName, message );
    }
    // Clear UI for dropped file if any.
    clearDroppedFileUI( peerId, fileName, msgAlert );
	}

  private void clearDroppedFileUI( String peerId, String fileName, String message ) {
    // Check if FilePermissionAlertFragment exists
    FilePermissionAlertFragment fragment = RoomManager.get().getFileAlertFragment();
    if(  fragment != null ) { 
      // If it is the dropped transaction, dismmiss this fragment
      if( fragment.getPeerId().equals( peerId ) && fragment.getFileName().equals( fileName ) ) {
        // RoomManager.get().getFileAlertFragment().saveTimeout();
        fragment.saveTimeout( message );
        cancelMessage = message;
        curDropped = true;
      }
      // FileBrowserActivity will be handled from fileAlertFragment.
    } else {
      // Check if FileBrowserActivity exists
      FileBrowserActivity fe = RoomManager.get().getFileBrowserActivity();
      if( fe != null ) {
        // If it the dropped transaction, dismmiss this file explorer activity
        if( fe.getPeerId().equals( peerId ) && fe.getFileName().equals( fileName ) ) {
          fe.saveTimeout( message );
          cancelMessage = message;
          curDropped = true;
        }
      }
    }
    // If current file transfer UI had been cleared
    if( curDropped ) {
      // Cancel UI and continue processing request queue. Done in onActivityResult.
    } else {
      // Check for a pending request and remove it silently if present.
      if( !clearDroppedPendingRequest( peerId, fileName ) )
        // The file dropped was not waiting for our permission, i.e.,
          // it could be an on going transfer, or one initiated by us.
          // Simply alert the user.
        AlertFragment.newInstance( message ).show( 
          RoomViewActivity.this.getFragmentManager(), TAG );
    }
  }

  // Check for pending request and remove them silently.
    // The earliest request that matches peerId and fileName will be removed.
    // Returns true if a request was removed and false otherwise.
  private boolean clearDroppedPendingRequest( String peerId, String fileName ) {
    String[] fileRequest = null;
    ArrayList<String[]> fileRequestList = RoomManager.get().mFileRequestList;
    int pending = fileRequestList.size();
    if( pending == 0 ) return false;

    // Find earliest request in queue that matches.
    for( int i = 0; i < pending; ++i ){
      String[] request = fileRequestList.get( i );
      if( request[ 1 ].equals( peerId ) && request[ 2 ].equals( fileName ) ) {
        fileRequest = request;
        break;
      }
    }
    // If found, remove it silently.
    if( fileRequest != null ) {
      fileRequestList.remove( fileRequest );
      return true;
    } else return false;
  }

	@Override
	public void onComplete(String peerId, String fileName, boolean isSending) {
    String nick = RoomManager.get().getDisplayName( peerId );
    // Add in the last 100% in case file progress showed less than 100%
      // (a result of using chunk for estimation of file transferred).
    if (isSending) {
      Utility.showRapidShortToast( this, R.string.message_progress_send, fileName, nick, "100" );
    } else if (!isSending) {
      Utility.showRapidShortToast( this, R.string.message_progress_save, fileName, nick, "100" );
    }
    // Announce completion. Do not use showRapidShortToast so as not to cancel 100% progress toast.
    if (isSending) {
      Utility.showShortToast( this, R.string.message_complete_send, nick, fileName );
    } else if (!isSending) {
      Utility.showShortToast( this, R.string.message_complete_save, nick, fileName );
    }
	}

	@Override
	public void onProgress(String peerId, String fileName, double percentage,
			boolean isSending) {
    String nick = RoomManager.get().getDisplayName( peerId );
    String pctStr = Double.toString(percentage);
    if (pctStr.length() > 4)
      pctStr = pctStr.substring(0, 4);
    if (isSending) {
      Utility.showRapidShortToast( this, R.string.message_progress_send, fileName, nick, pctStr );
    } else if (!isSending) {
      Utility.showRapidShortToast( this, R.string.message_progress_save, fileName, nick, pctStr );
    }
	}

// -------------------------------------------------------------------------------------------------
// DataChannel API redirection - Allow other classes to call API of DC
// manager
// -------------------------------------------------------------------------------------------------
	// Call to DC API in mConnection
  public void sendFile( String fileName, String filePath, String tid, boolean isPrivate ) {
    if( !isPrivate ) tid = null;
    mConnection.sendFileTransferRequest( tid, fileName, filePath );
	}

	// Call to DC API in mConnection
	public void saveFile( String tid, boolean accept, String filePath) {
    mConnection.acceptFileTransferRequest( tid, accept, filePath );
	}

// -------------------------------------------------------------------------------------------------
// Chat methods.
// -------------------------------------------------------------------------------------------------

	// Initialise Chat UI if not present.
	// Set to the right chat target (group or private peer).
	public void setChat( String tid, boolean isPrivate ) {
		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = fragmentManager
				.findFragmentById(R.id.split_container);
		Class<?> fragmentClass = fragment.getClass();
		if ( !ChatFragment.class.isInstance( fragment ) ) {
			ChatFragment chatFragment = new ChatFragment();
			chatFragment.setNickname("Me");
      chatFragment.setTid(tid);
      chatFragment.setIsPrivate( isPrivate );
      chatFragment.setConfig(mConnectionConfig);
      // If we are switching from Video to non-video UI,
      if (RoomFragment.class.isAssignableFrom(fragmentClass)) {
        // Remove control panel.
        View view = (ViewGroup) findViewById(R.id.control_panel_container);
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        layoutParams.weight = mCpWeight = 0;
        view.setLayoutParams(layoutParams);
      }
      fragmentManager.beginTransaction().replace(R.id.split_container, chatFragment).commit();
    } else {
      ( ( ChatFragment ) fragment ).setTarget( tid, isPrivate );
    }
	}

  // Add message to targeted chat UI if it is currently up, else add to chat record.
  public void addChatMsg( String peerId, String nick, String message, boolean isPrivate ) {
    String msgChat;
    OneComment groupInPrivate = null;
    OneComment comment = null;

    if( !isPrivate ) {
      groupInPrivate = new OneComment(true, "[GRP] " + message );
      msgChat = nick + ": " + message;
    } else {
      msgChat = message;
    }
    comment = new OneComment(true, msgChat);

    // Add to chat record.
		if( !isPrivate ) {
			RoomManager.get().addGroupChat( comment );
      RoomManager.get().addPrivateChat( peerId, groupInPrivate );
    } else {
      RoomManager.get().addPrivateChat( peerId, comment );
    }

    // If targeted Chat UI is up, try to add to chat UI.
		DiscussArrayAdapter chatAdapter = RoomManager.get().getChatAdapter();
		if (chatAdapter != null) {
			String chatPeerId = RoomManager.get().getChatPeerId();
      // For group chat
      if( !isPrivate ) {
        if( chatPeerId == null ) {
          // Add group chat to group chat UI
					chatAdapter.add( comment );
  			} else if ( chatPeerId.equals( peerId ) ) {
          // Add group chat to targeted private chat UI
  				chatAdapter.add( groupInPrivate );
        }
      }  else if ( chatPeerId.equals( peerId ) ) {
        // Add private chat to targeted private chat UI
        chatAdapter.add( comment );
			}
		}
	  
      /*Fragment fragment = getFragmentManager().findFragmentById(R.id.split_container);

    // Check if fragment occupying split_container is a ChatFragment
    if( ChatFragment.class.isInstance( fragment ) ) {
      ChatFragment chatFragment = (ChatFragment) fragment;
      // Add message to chat UI
      chatFragment.addChatMsg( msgChat, peerId, isPrivate );
    } else {
      // If chat fragment not available, add message to chat container for subsequent display.
      String target = "group";
      if( isPrivate ) target = peerId;
      ChatContent.chatMsgList.put( target, ChatContent.chatMsgList.get( target ) + msgChat + "\n" );
    }*/
  }

  // Add new chat to Video UI new chat textView
  public void addChatAlert( String peerId, String msgChat, boolean isPrivate ) {
    // Current chat UI target (if any)
    String chatPeerId = RoomManager.get().getChatPeerId();
    boolean chatUI = true;
    if( RoomManager.get().getChatAdapter() == null ) chatUI = false;
    TextView newChatTxtVw;

    if( isPrivate ) {
      // Do not add chat alert if already in targeted chat UI
      if( chatPeerId != null && chatPeerId.equals( peerId ) ) return;
      newChatTxtVw = RoomManager.get().getChatPrivateTextView( peerId );
      ChatContent.newChatPrivateMsgList.put( peerId, msgChat );
    } else {
      // Do not add chat alert if already in targeted chat UI
      if( chatPeerId == null && chatUI || 
        chatPeerId != null && chatPeerId.equals( peerId ) ) return;
      newChatTxtVw = RoomManager.get().getChatGroupTextView( peerId );
      ChatContent.newChatGroupMsgList.put( peerId, msgChat );
    }
    
    if( newChatTxtVw != null ) newChatTxtVw.setText( msgChat );
  }

// -------------------------------------------------------------------------------------------------
// File Explorer methods.
// -------------------------------------------------------------------------------------------------

  public void processFileRequest() {
    // Proceed only if not currently in file explorer UI, and there are requests.
    if( RoomManager.get().getFileActive() || 
      RoomManager.get().mFileRequestList.size() == 0 ) return;

    // Get earliest request and process it.
    String[] fileRequest = RoomManager.get().mFileRequestList.remove( 0 );
      // Set File explorer state.
    RoomManager.get().setFileActive( true );
    FilePermissionAlertFragment.newInstance( fileRequest[0], fileRequest[1], fileRequest[2] )
      .show(RoomViewActivity.this.getFragmentManager(), TAG);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case Utility.REQUEST_CODE_PICK_FILE:
        // If the file selection was successful
        if (resultCode == RESULT_OK) {
          if (data != null) {
            // Get the URI of the selected file
            final Uri uri = data.getData();
            Log.i(TAG, "Uri = " + uri.toString());
            try {
              // Get the file path from the URI
              final String path = FileUtils.getPath(this, uri);
              final String fileName = new File(path).getName();
              mConnection.sendFileTransferRequest(RoomManager.get()
                  .getFileTransferPeerId(), fileName, path);
            } catch (Exception e) {
              Log.e("FileSelectorTestActivity", "File select error",
                  e);
            }
          }
        }
        break;
      case Utility.REQUEST_CODE_PICK_DIR: {
        if( data != null ) {
          String newDir = data
              .getStringExtra(com.temasys.skylink.sample.FileBrowserActivity.returnDirectoryParameter);
          String peerId = data
              .getStringExtra(FileBrowserActivity.EXTRA_PEER_ID);
          String fileName = data
              .getStringExtra(FileBrowserActivity.EXTRA_FILE_NAME);
          String cancelSave = data
              .getStringExtra(FileBrowserActivity.EXTRA_CANCEL_SAVE);
          if (resultCode == RESULT_OK) {
            if( cancelSave != null && cancelSave.equals( FileBrowserActivity.EXTRA_TIMEOUT ) ) {
              // File save was cancelled explicitly or due to timeout at FilePermissionAlert stage.
            } else {
              mConnection.acceptFileTransferRequest(peerId, true, newDir + File.separator + fileName);
            }
          } else {
            // We cancelled file save ourselves.
            Utility.showShortToast(this, R.string.message_on_decline_save,
              fileName, RoomManager.get().getDisplayName(peerId));
            mConnection.acceptFileTransferRequest(peerId, false, fileName);
          }// END } else {//if(resultCode == this.RESULT_OK) {
        } else {
          // File save was cancelled explicitly or due to timeout at File explorer UI stage.
            // For some reason, perhaps due to an uncaught exception in File explorer,
            // resultCode would be 0 (RESULT_CANCEL) and data null.
        }
      }
      break;
    }
    // Handle file save that was cancelled explicitly or due to timeout at File save UI.
    if( curDropped ) {
      // Inform user
      AlertFragment.newInstance( cancelMessage ).show( 
        RoomViewActivity.this.getFragmentManager(), TAG );
      // Reset state
      curDropped = false;
    }
    // Reset FileBrowserActivity reference in RoomManager
    RoomManager.get().setFileBrowserActivity( null );
    // Reset File explorer state and process next request.
    RoomManager.get().setFileActive( false );
    processFileRequest();
  }

	// Set visibility of FileExplorer.
	// true/false for VISIBLE/GONE
	public void setFileExplorer( FileExplorerFragment.Ops operation, String tid, boolean isPrivate,
    String fileName ) {
		if (mConnectionConfig.hasFileTransfer()) {
			FragmentManager fragmentManager = getFragmentManager();
			Fragment fragment = fragmentManager
					.findFragmentById(R.id.split_container);
			Class<?> fragmentClass = fragment.getClass();
			if (!fragmentClass.isInstance(FeFragment.class)) {
				FeFragment feFragment = new FeFragment();
        feFragment.setTid(tid);
        feFragment.setIsPrivate( isPrivate );
        feFragment.setOperation( operation );
        feFragment.setSaveFileName( fileName );

				if (RoomFragment.class.isAssignableFrom(fragmentClass)) {
					// RoomManager.get().setSplitFragmentClass(fragmentClass);
					View view = (ViewGroup) findViewById(R.id.control_panel_container);
					LayoutParams layoutParams = (LayoutParams) view
							.getLayoutParams();
					layoutParams.weight = mCpWeight = 0;
					view.setLayoutParams(layoutParams);
				}
				fragmentManager.beginTransaction()
						.replace(R.id.split_container, feFragment).commit();
			} else {
        // If already in FeFragment, then target and UI.
        ( ( FeFragment ) fragment ).setTarget( tid, isPrivate, operation );
        ( ( FeFragment ) fragment ).setUI();
      }
		}
	}

// -------------------------------------------------------------------------------------------------
// File Explorer callbacks.
// -------------------------------------------------------------------------------------------------
	public static class FeFragment extends FileExplorerFragment {

		public FeFragment() {
			super();
		}

		// After deciding to Save file.
		@Override
    public void feDidAcceptSave( 
      FileExplorerFragment.Ops operation, String tid, boolean isPrivate, String fileName ) {
      parentActivity.setFileExplorer( operation, tid, isPrivate, fileName );
		}

		// After clicking cancel, in the sendFileDialogue.
		@Override
		public void feDidCancelSend() {
      // Return to video UI.
      parentActivity.setVideoUIFromRoomManager();
		}

		// After clicking cancel, after clicking accept Save file.
		@Override
		public void feDidCancelSave(String saveFileName, String remotePeerId) {
      String nick = RoomManager.get().getDisplayName( remotePeerId );
			parentActivity.saveFile( remotePeerId, false, saveFileName );
			Utility.showShortToast(
        parentActivity, R.string.message_on_decline_save, saveFileName, nick );
      // Return to video UI.
      parentActivity.setVideoUIFromRoomManager();
		}

    // After deciding to not Save file.
    @Override
    public void feDidDeclineSave(String saveFileName, String remotePeerId) {
      String nick = RoomManager.get().getDisplayName( remotePeerId );
      parentActivity.saveFile(remotePeerId, false, saveFileName);
      Utility.showShortToast(
        parentActivity, R.string.message_on_decline_save, saveFileName, nick );
      // Return to video UI.
      parentActivity.setVideoUIFromRoomManager();
    }

		// After clicking Send file button when a non-file is selected.
		@Override
		public void feDidSendNonFile() {
			// Simply warn the user and return to UI.
			Utility.showShortToast( parentActivity, R.string.message_on_non_file_selected );
		}

		// After clicking Send file button.
		@Override
    public void feDidClickSend( String fileName, String filePath, String tid, boolean isPrivate ) {
      parentActivity.sendFile( fileName, filePath, tid, isPrivate );
      String nick = RoomManager.get().getDisplayName( tid );

      if( isPrivate )
        Utility.showShortToast( parentActivity,
          R.string.message_on_click_send_private, nick, fileName, filePath );
      else
        Utility.showShortToast( parentActivity,
          R.string.message_on_click_send_group, fileName, filePath );

      // Return to video UI.
      parentActivity.setVideoUIFromRoomManager();
		}

		// After clicking Save file button.
		@Override
		public void feDidClickSave( String tid, String filePath, String saveFileName) {
			parentActivity.saveFile( tid, true, filePath);
      String nick = RoomManager.get().getDisplayName( tid );

      Utility.showShortToast( 
        parentActivity, R.string.message_on_click_save, saveFileName, nick, filePath );
      // Return to video UI.
      parentActivity.setVideoUIFromRoomManager();
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
		}
	}
	
}
