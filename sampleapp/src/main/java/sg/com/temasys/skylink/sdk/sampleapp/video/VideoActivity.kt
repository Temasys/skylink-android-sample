package sg.com.temasys.skylink.sdk.sampleapp.video

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import org.webrtc.SurfaceViewRenderer
import sg.com.temasys.skylink.sdk.sampleapp.R
import sg.com.temasys.skylink.sdk.sampleapp.utils.Constants.VIDEO_TYPE
import sg.com.temasys.skylink.sdk.sampleapp.utils.SmallVideoViewFragment
import sg.com.temasys.skylink.sdk.sampleapp.videoresolution.VideoResolutionFragment
import sg.com.temasys.skylink.sdk.sampleapp.videoresolution.VideoResolutionPresenter

/**
 * Created by muoi.pham on 04/04/19.
 */
class VideoActivity : AppCompatActivity() {
    private val VIDEO_MAIN_FRAGMENT_TAG = "VIDEO_MAIN_FRAGMENT"
    private val VIDEO_RES_FRAGMENT_TAG = "VIDEO_RES_FRAGMENT"
    private val VIDEO_LOCAL_CAMERA_VIEW_TAG = "VIDEO_LOCAL_CAMERA_VIEW_FRAGMENT"
    private val VIDEO_LOCAL_SCREEN_VIEW_TAG = "VIDEO_LOCAL_SCREEN_VIEW_FRAGMENT"
    private val VIDEO_REMOTE_CAMERA_VIEW_TAG = "VIDEO_REMOTE_CAMERA_VIEW_FRAGMENT"
    private val VIDEO_REMOTE_SCREEN_VIEW_TAG = "VIDEO_REMOTE_SCREEN_VIEW_FRAGMENT"

    // main presenter instance for video {both video camera and video screen}
    private var videoPresenter: VideoPresenter? = null

    // video resolution presenter instance
    private var videoResPresenter: VideoResolutionPresenter? = null

    // main video call fragment
    private var videoMainFragment: VideoFragment? = null

    // video resolution fragment to be added to the main view
    private var videoResolutionFragment: VideoResolutionFragment? = null

    // local video view fragment to be added to the main view
    var localVideoCameraFragment: SmallVideoViewFragment? = null
        private set
    var localVideoScreenFragment: SmallVideoViewFragment? = null
        private set
    var remoteVideoCameraFragment: SmallVideoViewFragment? = null
        private set
    var remoteVideoScreenFragment: SmallVideoViewFragment? = null
        private set
    var contentFrameLocalCameraView: FrameLayout? = null
        private set
    var contentFrameLocalScreenView: FrameLayout? = null
        private set
    var contentFrameRemoteCameraView: FrameLayout? = null
        private set
    var contentFrameRemoteScreenView: FrameLayout? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        contentFrameLocalCameraView = findViewById(R.id.contentFrameLocalCameraView)
        contentFrameLocalScreenView = findViewById(R.id.contentFrameLocalScreenView)
        contentFrameRemoteCameraView = findViewById(R.id.contentFrameRemoteCameraView)
        contentFrameRemoteScreenView = findViewById(R.id.contentFrameRemoteScreenView)

        //create presenters
        videoResPresenter = VideoResolutionPresenter(this)
        videoPresenter = VideoPresenter(this)

        // check previous state in case of screen rotation/configuration change
        // if it is new state, then create view instance
        // otherwise reuse the view instance and keep states
        if (savedInstanceState == null) {
            videoResolutionFragment = VideoResolutionFragment.newInstance()
            videoMainFragment = VideoFragment.newInstance()
            localVideoCameraFragment = SmallVideoViewFragment.newInstance()
            localVideoCameraFragment!!.setVideoType(VIDEO_TYPE.LOCAL_CAMERA)
            localVideoScreenFragment = SmallVideoViewFragment.newInstance()
            localVideoScreenFragment!!.setVideoType(VIDEO_TYPE.LOCAL_SCREEN)
            remoteVideoCameraFragment = SmallVideoViewFragment.newInstance()
            remoteVideoCameraFragment!!.setVideoType(VIDEO_TYPE.REMOTE_CAMERA)
            remoteVideoScreenFragment = SmallVideoViewFragment.newInstance()
            remoteVideoScreenFragment!!.setVideoType(VIDEO_TYPE.REMOTE_SCREEN)
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.contentFrameVideoRes, videoResolutionFragment!!, VIDEO_RES_FRAGMENT_TAG)
                    .add(R.id.contentFrameMainVideo, videoMainFragment!!, VIDEO_MAIN_FRAGMENT_TAG)
                    .add(R.id.contentFrameLocalCameraView, localVideoCameraFragment!!, VIDEO_LOCAL_CAMERA_VIEW_TAG)
                    .add(R.id.contentFrameLocalScreenView, localVideoScreenFragment!!, VIDEO_LOCAL_SCREEN_VIEW_TAG)
                    .add(R.id.contentFrameRemoteCameraView, remoteVideoCameraFragment!!, VIDEO_REMOTE_CAMERA_VIEW_TAG)
                    .add(R.id.contentFrameRemoteScreenView, remoteVideoScreenFragment!!, VIDEO_REMOTE_SCREEN_VIEW_TAG)
                    .commit()
        } else {
            videoResolutionFragment = supportFragmentManager
                    .findFragmentByTag(VIDEO_RES_FRAGMENT_TAG) as VideoResolutionFragment?
            videoMainFragment = supportFragmentManager
                    .findFragmentByTag(VIDEO_MAIN_FRAGMENT_TAG) as VideoFragment?
            localVideoCameraFragment = supportFragmentManager
                    .findFragmentByTag(VIDEO_LOCAL_CAMERA_VIEW_TAG) as SmallVideoViewFragment?
            localVideoScreenFragment = supportFragmentManager
                    .findFragmentByTag(VIDEO_LOCAL_SCREEN_VIEW_TAG) as SmallVideoViewFragment?
            remoteVideoCameraFragment = supportFragmentManager
                    .findFragmentByTag(VIDEO_REMOTE_CAMERA_VIEW_TAG) as SmallVideoViewFragment?
            remoteVideoScreenFragment = supportFragmentManager
                    .findFragmentByTag(VIDEO_REMOTE_SCREEN_VIEW_TAG) as SmallVideoViewFragment?
        }

        // hide the small views at the first time
        supportFragmentManager.beginTransaction()
                .hide(videoResolutionFragment!!)
                .hide(localVideoCameraFragment!!)
                .hide(localVideoScreenFragment!!)
                .hide(remoteVideoCameraFragment!!)
                .hide(remoteVideoScreenFragment!!)
                .commit()

        //link between view and presenter
        videoResPresenter!!.setView(videoResolutionFragment)
        videoPresenter!!.setVideoResPresenter(videoResPresenter)
        videoMainFragment!!.setPresenter(videoPresenter!!)
        videoPresenter!!.setMainView(videoMainFragment)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //Save the fragment's instance when changing configuration
        supportFragmentManager.putFragment(outState, VIDEO_MAIN_FRAGMENT_TAG, videoMainFragment!!)
        supportFragmentManager.putFragment(outState, VIDEO_RES_FRAGMENT_TAG, videoResolutionFragment!!)
        supportFragmentManager.putFragment(outState, VIDEO_LOCAL_CAMERA_VIEW_TAG, localVideoCameraFragment!!)
        supportFragmentManager.putFragment(outState, VIDEO_LOCAL_SCREEN_VIEW_TAG, localVideoScreenFragment!!)
        supportFragmentManager.putFragment(outState, VIDEO_REMOTE_CAMERA_VIEW_TAG, remoteVideoCameraFragment!!)
        supportFragmentManager.putFragment(outState, VIDEO_REMOTE_SCREEN_VIEW_TAG, remoteVideoScreenFragment!!)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.let { videoPresenter!!.processActivityResult(requestCode, resultCode, it) }
    }

    fun onShowHideVideoResFragment(isVisible: Boolean) {
        if (isVisible) {
            supportFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                    .show(videoResolutionFragment!!)
                    .commit()
        } else {
            supportFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                    .hide(videoResolutionFragment!!)
                    .commit()
        }
    }

    fun onShowHideLocalCameraViewFragment(isVisible: Boolean, isFullscreenMode: Boolean) {
        if (localVideoCameraFragment!!.view == null) return
        if (isVisible) {
            if (isFullscreenMode) {
                localVideoCameraFragment!!.displayView()
                contentFrameLocalCameraView!!.visibility = View.VISIBLE
            } else {
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .show(localVideoCameraFragment!!)
                        .commit()
                localVideoCameraFragment!!.displayView()
                contentFrameLocalCameraView!!.visibility = View.VISIBLE
            }
        } else {
            if (isFullscreenMode) {
                localVideoCameraFragment!!.hide()
                contentFrameLocalCameraView!!.visibility = View.GONE
            } else {
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .hide(localVideoCameraFragment!!)
                        .commit()
                localVideoCameraFragment!!.hide()
                contentFrameLocalCameraView!!.visibility = View.GONE
            }
        }
    }

    fun onShowHideLocalScreenViewFragment(isVisible: Boolean, isFullscreenMode: Boolean) {
        if (localVideoScreenFragment!!.view == null) return
        if (isVisible) {
            if (isFullscreenMode) {
                localVideoScreenFragment!!.displayView()
                contentFrameLocalScreenView!!.visibility = View.VISIBLE
            } else {
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .show(localVideoScreenFragment!!)
                        .commit()
                localVideoScreenFragment!!.displayView()
                contentFrameLocalScreenView!!.visibility = View.VISIBLE
            }
        } else {
            if (isFullscreenMode) {
                localVideoScreenFragment!!.hide()
                contentFrameLocalScreenView!!.visibility = View.GONE
            } else {
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .hide(localVideoScreenFragment!!)
                        .commit()
                localVideoScreenFragment!!.hide()
                contentFrameLocalScreenView!!.visibility = View.GONE
            }
        }
    }

    fun onShowHideRemoteCameraViewFragment(isVisible: Boolean, isFullscreenMode: Boolean) {
        if (remoteVideoCameraFragment!!.view == null) return
        if (isVisible) {
            if (isFullscreenMode) {
                remoteVideoCameraFragment!!.displayView()
                contentFrameRemoteCameraView!!.visibility = View.VISIBLE
            } else {
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .show(remoteVideoCameraFragment!!)
                        .commit()
                remoteVideoCameraFragment!!.displayView()
                contentFrameRemoteCameraView!!.visibility = View.VISIBLE
            }
        } else {
            if (isFullscreenMode) {
                remoteVideoCameraFragment!!.hide()
                contentFrameRemoteCameraView!!.visibility = View.GONE
            } else {
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .hide(remoteVideoCameraFragment!!)
                        .commit()
                remoteVideoCameraFragment!!.hide()
                contentFrameRemoteCameraView!!.visibility = View.GONE
            }
        }
    }

    fun onShowHideRemoteScreenViewFragment(isVisible: Boolean, isFullscreenMode: Boolean) {
        if (remoteVideoScreenFragment!!.view == null) return
        if (isVisible) {
            if (isFullscreenMode) {
                remoteVideoScreenFragment!!.displayView()
                contentFrameRemoteScreenView!!.visibility = View.VISIBLE
            } else {
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .show(remoteVideoScreenFragment!!)
                        .commit()
                remoteVideoScreenFragment!!.displayView()
                contentFrameRemoteScreenView!!.visibility = View.VISIBLE
            }
        } else {
            if (isFullscreenMode) {
                remoteVideoScreenFragment!!.hide()
                contentFrameRemoteScreenView!!.visibility = View.GONE
            } else {
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .hide(remoteVideoScreenFragment!!)
                        .commit()
                remoteVideoScreenFragment!!.hide()
                contentFrameRemoteScreenView!!.visibility = View.GONE
            }
        }
    }

    fun changeViewPosition(view: View, X: Int, Y: Int, _xDelta: Int, _yDelta: Int) {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val layoutParams = view.layoutParams as RelativeLayout.LayoutParams
        layoutParams.leftMargin = Math.max(X - _xDelta, 0) - resources.getDimension(R.dimen.dp_50dp).toInt()
        if (layoutParams.leftMargin < 0) {
            layoutParams.leftMargin = 0
        }
        if (layoutParams.leftMargin > screenWidth - layoutParams.width) {
            layoutParams.leftMargin = screenWidth - layoutParams.width
        }
        layoutParams.topMargin = Math.max(Y - _yDelta, 0) - resources.getDimension(R.dimen.dp_70dp).toInt()
        if (layoutParams.topMargin < 0) {
            layoutParams.topMargin = 0
        }
        if (layoutParams.topMargin > screenHeight - layoutParams.height) {
            layoutParams.topMargin = screenHeight - layoutParams.height
        }
        layoutParams.rightMargin = 0
        layoutParams.bottomMargin = 0
        view.layoutParams = layoutParams
    }

    fun processBringSmallViewToMainView(type: VIDEO_TYPE?) {
        videoMainFragment!!.bringSmallViewToMainView(type)
        when (type) {
            VIDEO_TYPE.LOCAL_CAMERA -> onShowHideLocalCameraViewFragment(false, false)
            VIDEO_TYPE.LOCAL_SCREEN -> onShowHideLocalScreenViewFragment(false, false)
            VIDEO_TYPE.REMOTE_CAMERA -> onShowHideRemoteCameraViewFragment(false, false)
            VIDEO_TYPE.REMOTE_SCREEN -> onShowHideRemoteScreenViewFragment(false, false)
        }
    }

    fun setLocalCameraView(localView: SurfaceViewRenderer?) {
        localVideoCameraFragment!!.view = localView
        localVideoCameraFragment!!.setVideoType(VIDEO_TYPE.LOCAL_CAMERA)
        onShowHideLocalCameraViewFragment(true, false)
    }

    fun setLocalScreenView(localView: SurfaceViewRenderer?) {
        localVideoScreenFragment!!.view = localView
        localVideoScreenFragment!!.setVideoType(VIDEO_TYPE.LOCAL_SCREEN)
        onShowHideLocalScreenViewFragment(true, false)
    }

    fun setRemoteCameraView(remoteView: SurfaceViewRenderer?) {
        remoteVideoCameraFragment!!.view = remoteView
        remoteVideoCameraFragment!!.setVideoType(VIDEO_TYPE.REMOTE_CAMERA)
        onShowHideRemoteCameraViewFragment(true, false)
    }

    fun setRemoteScreenView(remoteView: SurfaceViewRenderer?) {
        remoteVideoScreenFragment!!.view = remoteView
        remoteVideoScreenFragment!!.setVideoType(VIDEO_TYPE.REMOTE_SCREEN)
        onShowHideRemoteScreenViewFragment(true, false)
    }

    fun detachSmallView(smallVideoViewFragment: SmallVideoViewFragment) {
        if (smallVideoViewFragment === localVideoCameraFragment) {
            onShowHideLocalCameraViewFragment(false, false)
        } else if (smallVideoViewFragment === localVideoScreenFragment) {
            onShowHideLocalScreenViewFragment(false, false)
        } else if (smallVideoViewFragment === remoteVideoCameraFragment) {
            onShowHideRemoteCameraViewFragment(false, false)
        } else if (smallVideoViewFragment === remoteVideoScreenFragment) {
            onShowHideRemoteScreenViewFragment(false, false)
        }
    }

    fun attachSmallView(smallVideoViewFragment: SmallVideoViewFragment) {
        if (smallVideoViewFragment === localVideoCameraFragment) {
            onShowHideLocalCameraViewFragment(true, false)
        } else if (smallVideoViewFragment === localVideoScreenFragment) {
            onShowHideLocalScreenViewFragment(true, false)
        } else if (smallVideoViewFragment === remoteVideoCameraFragment) {
            onShowHideRemoteCameraViewFragment(true, false)
        } else if (smallVideoViewFragment === remoteVideoScreenFragment) {
            onShowHideRemoteScreenViewFragment(true, false)
        }
    }

    fun resetSmallRemoteViews() {
        onShowHideRemoteCameraViewFragment(false, false)
        onShowHideRemoteScreenViewFragment(false, false)
    }

    fun removeView(videoType: VIDEO_TYPE?) {
        when (videoType) {
            VIDEO_TYPE.LOCAL_CAMERA -> {
                onShowHideLocalCameraViewFragment(false, false)
                localVideoCameraFragment!!.view = null
            }
            VIDEO_TYPE.LOCAL_SCREEN -> {
                onShowHideLocalScreenViewFragment(false, false)
                localVideoScreenFragment!!.view = null
            }
            VIDEO_TYPE.REMOTE_CAMERA -> {
                onShowHideRemoteCameraViewFragment(false, false)
                remoteVideoCameraFragment!!.view = null
            }
            VIDEO_TYPE.REMOTE_SCREEN -> {
                onShowHideRemoteScreenViewFragment(false, false)
                remoteVideoScreenFragment!!.view = null
            }
        }
    }
}