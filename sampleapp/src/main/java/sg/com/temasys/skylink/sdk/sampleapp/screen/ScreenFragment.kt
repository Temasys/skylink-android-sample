package sg.com.temasys.skylink.sdk.sampleapp.screen

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import sg.com.temasys.skylink.sdk.sampleapp.R
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer
import sg.com.temasys.skylink.sdk.sampleapp.setting.Config
import sg.com.temasys.skylink.sdk.sampleapp.setting.ConfigRoomFragment
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants
import sg.com.temasys.skylink.sdk.sampleapp.utils.CustomActionBar

/**
 * A simple [CustomActionBar] subclass.
 * This class is responsible for display UI and get user interaction
 */
class ScreenFragment : CustomActionBar(), ScreenContract.View, View.OnClickListener {
    private val TAG = ScreenFragment::class.java.name

    // The view widgets
    private var llTool: LinearLayout? = null
    private var btnAudioSpeaker: ImageButton? = null
    private var btnAudioEnd: ImageButton? = null
    private var img: ImageView? = null

    // presenter instance to implement app logic
    private var presenter: ScreenContract.Presenter? = null
    override fun setPresenter(presenter: ScreenContract.Presenter?) {
        this.presenter = presenter
    }

    //----------------------------------------------------------------------------------------------
    // Fragment life cycle methods
    //----------------------------------------------------------------------------------------------
    override fun onAttach(context: Context) {
        super.onAttach(context)
        super.context = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Allow volume to be controlled using volume keys
        (context as ScreenActivity).volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.d(TAG, "[SA][Audio][onCreateView] ")
        val rootView = inflater.inflate(R.layout.fragment_screen, container, false)

        // get the UI controls from layout
        getControlWidgets(rootView)

        // setup the action bar
        setActionBar()

        // init values for view controls
        initControls()

        //request an initiative connection
        requestViewLayout()
        return rootView
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        // delegate presenter to implement the permission results
        presenter!!.processPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        //when changing configuration, do change layout view to fit with screen
        changeLayout(newConfig.orientation)
    }

    override fun onClick(view: View) {
        //Defining a click event listener for the buttons in the action bar.
        when (view.id) {
            R.id.btnBack -> processBack()
            R.id.btnLocalPeer -> {
                changeLocalPeerUI(true)
                displayPeerInfo(0)
            }
            R.id.btnRemotePeer1 -> {
                changeRemotePeerUI(1, true)
                displayPeerInfo(1)
            }
            R.id.btnRemotePeer2 -> {
                changeRemotePeerUI(2, true)
                displayPeerInfo(2)
            }
            R.id.btnRemotePeer3 -> {
                changeRemotePeerUI(3, true)
                displayPeerInfo(3)
            }
            R.id.btnRemotePeer4 -> {
                changeRemotePeerUI(4, true)
                displayPeerInfo(4)
            }
            R.id.btnRemotePeer5 -> {
                changeRemotePeerUI(5, true)
                displayPeerInfo(5)
            }
            R.id.btnRemotePeer6 -> {
                changeRemotePeerUI(6, true)
                displayPeerInfo(6)
            }
            R.id.btnRemotePeer7 -> {
                changeRemotePeerUI(7, true)
                displayPeerInfo(7)
            }
            R.id.btnAudioSpeaker -> presenter!!.processChangeAudioOutput()
            R.id.btnAudioEnd -> processEndAudio()
        }
    }

    override fun onDetach() {
        super.onDetach()

        // Close the room connection when this sample app is finished, so the streams can be closed.
        // I.e. already isConnected() and not changing orientation.
        // in case of changing screen orientation, do not close the connection
        if (!(context as ScreenActivity).isChangingConfigurations) {
            presenter!!.processExit()
        }
    }
    //----------------------------------------------------------------------------------------------
    // Methods called from the Presenter to update UI
    //----------------------------------------------------------------------------------------------
    /**
     * Get the instance of the view for implementing runtime permission
     */
    override val instance: Fragment
        get() = this

    /**
     * Update GUI into connected state when connected to room
     * Show room id and local peer button and display local avatar by the first character of the local username
     *
     * @param roomId the id of the connected room that generated by SDK
     */
    override fun updateUIConnected(roomId: String?) {

        // update the room id on the action bar
        updateRoomInfo(roomId)

        // update the local peer button in the action bar
        updateUILocalPeer(Config.getPrefString(ConfigRoomFragment.PREF_USER_NAME_AUDIO_SAVED, Constants.USER_NAME_AUDIO_DEFAULT, context))

        // update the image to show local peer in the room
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            img!!.setImageDrawable(context.getDrawable(R.drawable.ic_local_32))
        }
    }

    override fun updateUIDisconnected() {
//        if(context == null)
//            return;
//
//        updateRoomInfo(getResources().getString(R.string.guide_room_id));
//
//        btnLocalPeer.setVisibility(GONE);
    }

    /**
     * Update information about new remote peer joining the room at a specific index
     *
     * @param newPeer remote peer joining the room
     * @param index   specific index
     */
    override fun updateUIRemotePeerConnected(newPeer: SkylinkPeer?, index: Int) {
        //add new remote peer button in the action bar
        updateUiRemotePeerJoin(newPeer, index)

        // update the image to show peers in the room
        // there are 2 peers in room (including local peer) according to the config
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            img!!.setImageDrawable(context.getDrawable(R.drawable.ic_peers_32))
        }
    }

    /**
     * Display information about remote peer left from the room
     * refill the left peer(s) in the buttons to make sure the peer displayed in correct order
     *
     * @param peersList the list of left peer(s) in the room
     */
    override fun updateUIRemotePeerDisconnected(peersList: List<SkylinkPeer?>?) {
        // re fill the peers buttons in the action bar to show the peer correctly order
        processFillPeers(peersList)

        // update the image to show only local peer left in the room
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            img!!.setImageDrawable(context.getDrawable(R.drawable.ic_local_32))
        }
    }

    /**
     * Update button UI when audio output state changed
     *
     * @param isSpeakerOn the state of speaker {on/off}
     */
    override fun updateUIAudioOutputChanged(isSpeakerOn: Boolean) {

        //change the button background and icon
        var backgroundSrcBtn: Drawable? = null
        if (isSpeakerOn) {
            btnAudioSpeaker!!.background = context.resources.getDrawable(R.drawable.button_circle_press)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                backgroundSrcBtn = context.resources.getDrawable(R.drawable.ic_audio_speaker, null)
            }
        } else {
            btnAudioSpeaker!!.background = context.resources.getDrawable(R.drawable.button_circle)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                backgroundSrcBtn = context.resources.getDrawable(R.drawable.icon_speaker_mute, null)
            }
        }
        if (backgroundSrcBtn != null) btnAudioSpeaker!!.setImageDrawable(backgroundSrcBtn)
    }
    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------
    /**
     * Get the widget controls from layout
     */
    private fun getControlWidgets(rootView: View) {
        llTool = rootView.findViewById(R.id.ll_tool)
        btnAudioSpeaker = rootView.findViewById(R.id.btnAudioSpeaker)
        btnAudioEnd = rootView.findViewById(R.id.btnAudioEnd)
        img = rootView.findViewById(R.id.img)
    }

    /**
     * Setup the custom action bar
     * And get the view widgets in the action bar
     */
    private fun setActionBar() {
        val actionBar = (activity as ScreenActivity?)!!.supportActionBar
        super.setActionBar(actionBar)
    }

    /**
     * Init the view widgets for the fragment
     */
    private fun initControls() {
        // init setting value for room name in action bar
        txtRoomName.text = Config.getPrefString(ConfigRoomFragment.PREF_ROOM_NAME_AUDIO_SAVED, Constants.ROOM_NAME_AUDIO_DEFAULT, context)

        // set onClick event for buttons in layout
        btnBack.setOnClickListener(this)
        btnLocalPeer.setOnClickListener(this)
        btnRemotePeer1.setOnClickListener(this)
        btnRemotePeer2.setOnClickListener(this)
        btnRemotePeer3.setOnClickListener(this)
        btnRemotePeer4.setOnClickListener(this)
        btnRemotePeer5.setOnClickListener(this)
        btnRemotePeer6.setOnClickListener(this)
        btnRemotePeer7.setOnClickListener(this)
        btnAudioSpeaker!!.setOnClickListener(this)
        btnAudioEnd!!.setOnClickListener(this)

        // show the connecting animation
        val backgroundSrc = context.resources.getDrawable(R.drawable.img_blink) as AnimationDrawable
        if (backgroundSrc != null) {
            img!!.setImageDrawable(backgroundSrc)
            val frameAnimation = img!!.drawable as AnimationDrawable
            frameAnimation?.start()
        }
    }

    /**
     * request init connection at the first time
     * try to connect to room if not connected
     */
    private fun requestViewLayout() {
        if (presenter != null) {
            presenter!!.processConnectedLayout()
        }

        //changing layout to fit with screen
        changeLayout(resources.configuration.orientation)
    }

    /**
     * changing the view layout when changing screen orientation
     *
     * @param orientation portrait or landscape mode
     */
    private fun changeLayout(orientation: Int) {
        // Setting different bottom margins to different screen to have a better UI
        val llParams = llTool!!.layoutParams as LinearLayout.LayoutParams
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            llParams.bottomMargin = context.resources.getDimension(R.dimen.dp_50dp).toInt()
        } else {
            llParams.bottomMargin = context.resources.getDimension(R.dimen.dp_5dp).toInt()
        }
        llTool!!.layoutParams = llParams
    }

    /**
     * Display the dialog of peer info including peer username and peer id
     * when the user click into the peer button in action bar
     */
    private fun displayPeerInfo(index: Int) {
        val peer = presenter!!.processGetPeerByIndex(index)
        if (index == 0) {
            processDisplayLocalPeer(peer)
        } else {
            processDisplayRemotePeer(peer)
        }
    }

    /**
     * process closing the connection and activity when ending the audio call
     */
    private fun processEndAudio() {

        // Inform the presenter to implement closing the connection
        presenter!!.processExit()

        //close UI
        if (activity != null) {
            activity!!.finish()
        }
    }

    companion object {
        fun newInstance(): ScreenFragment {
            return ScreenFragment()
        }
    }
}