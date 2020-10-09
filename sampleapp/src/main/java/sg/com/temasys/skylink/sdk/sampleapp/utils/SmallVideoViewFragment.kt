package sg.com.temasys.skylink.sdk.sampleapp.utils

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.widget.*
import androidx.fragment.app.Fragment
import org.webrtc.SurfaceViewRenderer
import sg.com.temasys.skylink.sdk.sampleapp.R
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.VIDEO_TYPE
import sg.com.temasys.skylink.sdk.sampleapp.video.VideoActivity

/**
 * A simple [CustomActionBar] subclass.
 * This class is responsible for display UI and get user interaction
 */
class SmallVideoViewFragment : Fragment(), View.OnClickListener, OnTouchListener {
    private val TAG = SmallVideoViewFragment::class.java.name
    private var mContext: Context? = null

    // view widgets
    private var localVideoViewLayout: LinearLayout? = null
    private var btnLocalOption: Button? = null
    private var btnBringToMain: ImageButton? = null

    // the type of this small view, can be one of Constants.VIDEO_TYPE
    private var type = VIDEO_TYPE.LOCAL_CAMERA
    private var currentView: SurfaceViewRenderer? = null
    fun setVideoType(videoType: VIDEO_TYPE) {
        type = videoType
    }

    //----------------------------------------------------------------------------------------------
    // Fragment life cycle methods
    //----------------------------------------------------------------------------------------------
    override fun onAttach(context: Context) {
        this.mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.d(TAG, "[SA][Video][onCreateView] ")
        val rootView = inflater.inflate(R.layout.local_video_view_layout, container, false)

        // get the UI controls from layout
        getControlWidgets(rootView)

        // init the UI controls
        initComponents()
        return rootView
    }

    override fun onClick(view: View) {
        //Defining a click event actions for the buttons
        when (view.id) {
            R.id.ll_local_video_view -> {
            }
            R.id.btn_bring_to_main -> processBringViewToMain()
        }
    }

    override fun onDetach() {
        super.onDetach()
    }

    //----------------------------------------------------------------------------------------------
    // private methods for internal process
    //----------------------------------------------------------------------------------------------
    private fun getControlWidgets(rootView: View) {
        localVideoViewLayout = rootView.findViewById(R.id.ll_local_video_view)
        btnLocalOption = rootView.findViewById(R.id.btnLocalVideoOption)
        btnBringToMain = rootView.findViewById(R.id.btn_bring_to_main)
    }

    /**
     * Init value for view components
     */
    private fun initComponents() {
        // set onClick event for buttons in layout
        btnLocalOption!!.setOnClickListener(this)
        localVideoViewLayout!!.setOnClickListener(this)
        localVideoViewLayout!!.setOnTouchListener(this)
        btnBringToMain!!.setOnClickListener(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btnBringToMain!!.tooltipText = "Bring view to main view"
        }
    }

    override fun onTouch(v: View, e: MotionEvent): Boolean {
        val X = e.rawX.toInt()
        val Y = e.rawY.toInt()
        var _xDelta = 0
        var _yDelta = 0
        when (e.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                val lParams = v.layoutParams as RelativeLayout.LayoutParams
                _xDelta = X - lParams.leftMargin
                _yDelta = Y - lParams.topMargin
            }
            MotionEvent.ACTION_UP -> {
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
            }
            MotionEvent.ACTION_POINTER_UP -> {
            }
            MotionEvent.ACTION_MOVE -> {
                // send data for the fragment to move
                val activity = activity as VideoActivity?
                activity?.changeViewPosition(getContentFrameContainer(type)!!, X, Y, _xDelta, _yDelta)
            }
        }
        return false
    }

    private fun getContentFrameContainer(videoType: VIDEO_TYPE): FrameLayout? {
        var activity: VideoActivity? = null
        if (mContext != null && mContext is VideoActivity) {
            activity = getActivity() as VideoActivity?
        }
        return when (videoType) {
            VIDEO_TYPE.LOCAL_CAMERA -> activity!!.contentFrameLocalCameraView
            VIDEO_TYPE.LOCAL_SCREEN -> activity!!.contentFrameLocalScreenView
            VIDEO_TYPE.REMOTE_CAMERA -> activity!!.contentFrameRemoteCameraView
            VIDEO_TYPE.REMOTE_SCREEN -> activity!!.contentFrameRemoteScreenView
        }
        return null
    }

    fun setView(localView: SurfaceViewRenderer?) {
        if (localView == null) {
            val log = "[SA][addRemoteView] Not adding remote view as videoView is null!"
            Log.d(TAG, log)
            localVideoViewLayout!!.visibility = View.GONE
            btnBringToMain!!.visibility = View.GONE
            currentView = localView
            return
        }

        // If previous self video exists,
        // Set new video to size of previous self video
        // And remove old self video.
        val self = localVideoViewLayout!!.findViewWithTag<View>("self")
        if (self != null) {
            // Remove the old self video.
            localVideoViewLayout!!.removeView(self)
        }
        Utils.removeViewFromParent(localView)

        // Tag new video as self and add onClickListener.
        localView.tag = "self"
        // And new self video.
        val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        params.gravity = Gravity.CENTER
        params.weight = 1f
        localView.layoutParams = params

        //alway set self video as vertical orientation
        localVideoViewLayout!!.orientation = LinearLayout.VERTICAL
        localVideoViewLayout!!.addView(localView)
        localVideoViewLayout!!.invalidate()
        currentView = localView
        localVideoViewLayout!!.visibility = View.VISIBLE
        btnBringToMain!!.visibility = View.VISIBLE
    }

    override fun getView(): SurfaceViewRenderer? {
        return currentView
    }

    fun displayView() {
        if (currentView == null) {
            val log = "[SA][addRemoteView] Not adding remote view as videoView is null!"
            Log.d(TAG, log)
            localVideoViewLayout!!.visibility = View.GONE
            btnBringToMain!!.visibility = View.GONE
            return
        }

        // If previous self video exists,
        // Set new video to size of previous self video
        // And remove old self video.
        val self = localVideoViewLayout!!.findViewWithTag<View>("self")
        if (self != null) {
            // Remove the old self video.
            localVideoViewLayout!!.removeView(self)
        }

        // Tag new video as self and add onClickListener.
        currentView!!.tag = "self"
        // And new self video.
        val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        params.gravity = Gravity.CENTER
        params.weight = 1f
        currentView!!.layoutParams = params

        //alway set self video as vertical orientation
        localVideoViewLayout!!.orientation = LinearLayout.VERTICAL
        Utils.removeViewFromParent(currentView)
        localVideoViewLayout!!.addView(currentView)
        localVideoViewLayout!!.invalidate()
        localVideoViewLayout!!.visibility = View.VISIBLE
        btnBringToMain!!.visibility = View.VISIBLE
    }

    private fun processBringViewToMain() {
        if (mContext != null && mContext is VideoActivity) {
            (activity as VideoActivity?)!!.processBringSmallViewToMainView(type)
        }
    }

    fun hide() {
        val self = localVideoViewLayout!!.findViewWithTag<View>("self")
        if (self != null) {
            // Remove the old self video.
            localVideoViewLayout!!.removeView(self)
        }
        localVideoViewLayout!!.visibility = View.GONE
        btnBringToMain!!.visibility = View.GONE
    }

    companion object {
        fun newInstance(): SmallVideoViewFragment {
            return SmallVideoViewFragment()
        }
    }
}