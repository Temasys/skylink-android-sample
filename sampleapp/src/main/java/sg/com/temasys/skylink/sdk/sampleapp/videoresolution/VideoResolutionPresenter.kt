package sg.com.temasys.skylink.sdk.sampleapp.videoresolution

import android.content.Context
import sg.com.temasys.skylink.sdk.rtc.SkylinkCaptureFormat
import sg.com.temasys.skylink.sdk.rtc.SkylinkMedia
import sg.com.temasys.skylink.sdk.sampleapp.BasePresenter
import sg.com.temasys.skylink.sdk.sampleapp.service.SkylinkCommonService
import sg.com.temasys.skylink.sdk.sampleapp.service.model.VideoResolution

/**
 * Created by muoi.pham on 27/02/19.
 * This class is responsible for implementing logic of video resolutions
 */
class VideoResolutionPresenter(private val context: Context) : BasePresenter(), VideoResolutionContract.Presenter {

    // The video view instance
    var videoResolutionView: VideoResolutionContract.View? = null

    // Service instance
    private var videoService: SkylinkCommonService? = null

    // the current video resolutions
    private val currentVideoResCam = VideoResolution()
    private val currentVideoResScreen = VideoResolution()
    private var currentMainVideoTypeSelected: SkylinkMedia.MediaType? = null

    // flags for first time informed the input video resolution obtained to update the range, min, max value for UI
    private var hasInformedWHFirstValueScreen = false
    private var hasInformedFpsFirstValueScreen = false
    private var hasInformedWHFirstValueCam = false
    private var hasInformedFpsFirstValueCam = false
    fun setView(view: VideoResolutionContract.View?) {
        videoResolutionView = view
        videoResolutionView!!.setPresenter(this)
    }

    fun setService(videoService: SkylinkCommonService?) {
        this.videoService = videoService
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for view to call
    // These methods are responsible for processing requests from view
    //----------------------------------------------------------------------------------------------
    override fun processGetVideoResolutions() {
        videoService!!.getVideoResolutions(currentMainVideoTypeSelected!!, 1)
    }

    override fun processChooseVideoCamera() {
        currentMainVideoTypeSelected = SkylinkMedia.MediaType.VIDEO_CAMERA
        processGetVideoResolutions()
    }

    override fun processChooseVideoScreen() {
        currentMainVideoTypeSelected = SkylinkMedia.MediaType.VIDEO_SCREEN
        processGetVideoResolutions()
    }

    // camera video resolution processing
    override fun processWHProgressChangedCamera(progress: Int) {
        if (progress < 0 || progress > currentVideoResCam.captureFormats.size) return
        val format = currentVideoResCam.captureFormats[progress]
        var inputValue = "N/A"
        if (format.width > 0 && format.height > 0) {
            inputValue = format.width.toString() + "x" + format.height
        }
        videoResolutionView!!.updateUIOnCameraInputWHProgressValue(inputValue)
    }

    override fun processWHSelectedCamera(progress: Int) {
        if (progress < 0 || progress > currentVideoResCam.captureFormats.size) return
        val format = currentVideoResCam.captureFormats[progress]
        videoService!!.setInputVideoResolution(SkylinkMedia.MediaType.VIDEO_CAMERA, format.width, format.height, currentVideoResCam.fps)
    }

    override fun processFpsProgressChangedCamera(progress: Int) {
        if (progress < 0 || progress > 60) return
        videoResolutionView!!.updateUIOnCameraInputFpsProgressValue(progress.toString())
    }

    override fun processFpsSelectedCamera(progress: Int) {
        if (progress < 0 || progress > 60) return
        videoService!!.setInputVideoResolution(SkylinkMedia.MediaType.VIDEO_CAMERA, currentVideoResCam.width, currentVideoResCam.height, progress)
    }

    // screen video resolution processing
    override fun processWHProgressChangedScreen(progress: Int) {
        if (progress < 0 || progress > currentVideoResScreen.captureFormats.size) return
        val format = currentVideoResScreen.captureFormats[progress]
        var inputValue = "N/A"
        if (format.width > 0 && format.height > 0 && progress >= 0) {
            inputValue = format.width.toString() + "x" + format.height
        }
        videoResolutionView!!.updateUIOnScreenInputWHProgressValue(inputValue)
    }

    override fun processWHSelectedScreen(progress: Int) {
        if (progress < 0 || progress > currentVideoResScreen.captureFormats.size) return
        val format = currentVideoResScreen.captureFormats[progress]
        videoService!!.setInputVideoResolution(SkylinkMedia.MediaType.VIDEO_SCREEN, format.width, format.height, currentVideoResScreen.fps)
    }

    override fun processFpsProgressChangedScreen(progress: Int) {
        if (progress < 0 || progress > 60) return
        videoResolutionView!!.updateUIOnScreenInputFpsProgressValue(progress.toString())
    }

    override fun processFpsSelectedScreen(progress: Int) {
        if (progress < 0 || progress > 60) return
        videoService!!.setInputVideoResolution(SkylinkMedia.MediaType.VIDEO_SCREEN, currentVideoResScreen.width, currentVideoResScreen.height, progress)
    }

    //----------------------------------------------------------------------------------------------
    // Override methods from BasePresenter for service to call
    // These methods are responsible for processing requests from service
    //----------------------------------------------------------------------------------------------
    override fun processMediaTypeSelected(mediaType: SkylinkMedia.MediaType) {
        currentMainVideoTypeSelected = mediaType
        videoResolutionView!!.updateUIChangeMediaType(mediaType)
    }

    override fun processInputVideoResolutionObtained(mediaType: SkylinkMedia.MediaType?, width: Int, height: Int, fps: Int, captureFormat: SkylinkCaptureFormat?) {
        if (mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
            // update currentVideoResCam to the lastest resolution value
            val captureFormats = videoService!!.getCaptureFormats(null)
            currentVideoResCam.captureFormats = captureFormats
            currentVideoResCam.currentCaptureFormat = captureFormat
            currentVideoResCam.width = width
            currentVideoResCam.height = height
            currentVideoResCam.fps = fps

            // update input video res value on UI
            var inputValue = "N/A"
            if (width > 0 && height > 0 && fps > -1) {
                inputValue = """${width}x$height,
$fps Fps"""
            }
            videoResolutionView!!.updateUIOnCameraInputValue(inputValue)

            // update input video res on widthxheight in UI
            if (captureFormats != null && captureFormats.size > 0) {
                val len = captureFormats.size - 1
                var currentIndexWH = 0
                for (i in captureFormats.indices) {
                    if (captureFormats[i].width == width && captureFormats[i].height == height) {
                        currentIndexWH = i
                        break
                    }
                }
                if (!hasInformedWHFirstValueCam) {
                    val maxWHValue = captureFormats[0].width.toString() + "x" + captureFormats[0].height
                    val minWHValue = captureFormats[len].width.toString() + "x" + captureFormats[0].height
                    val WHValue = width.toString() + "x" + height
                    videoResolutionView!!.updateUIOnCameraInputWHValue(len, maxWHValue, minWHValue, WHValue, currentIndexWH)
                    hasInformedWHFirstValueCam = true
                }
            }

            // update input video res on Fps in UI
            if (captureFormat == null) {
                return
            }

            // update the range of fps seekbar
            videoResolutionView!!.updateUIOnCameraInputFpsValue(captureFormat.fpsMax.toString(), captureFormat.fpsMin.toString())

            // update the current value of the fps seekbar
            if (!hasInformedFpsFirstValueCam && fps > 0) {
                videoResolutionView!!.updateUIOnCameraInputFpsValue(captureFormat.fpsMax.toString(), captureFormat.fpsMin.toString(), fps.toString())
                hasInformedFpsFirstValueCam = true
            }
        } else if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            // manual create SkylinkCaptureFormat for screen
            // because the mobile screen height is normally double screen width, so create some demo resolutions
            var maxWidth = 800
            var maxHeight = 1600
            val minFps = 0
            val maxFps = 60

            // create 20 captureFormats as demo for screen
            val RANGE = 20
            val captureFormats = arrayOfNulls<SkylinkCaptureFormat>(RANGE)
            for (i in 0 until RANGE) {
                val widthFormat = maxWidth
                val heightFormat = maxHeight
                val format = SkylinkCaptureFormat(widthFormat, heightFormat, minFps, maxFps)
                captureFormats[i] = format
                maxWidth -= 30
                maxHeight -= 60
            }

            // save value to currentVideoResScreen
            currentVideoResScreen.captureFormats = captureFormats
            currentVideoResScreen.currentCaptureFormat = SkylinkCaptureFormat(width, height, minFps, maxFps)
            currentVideoResScreen.width = width
            currentVideoResScreen.height = height
            currentVideoResScreen.fps = fps

            // update input video res value on UI
            var inputValue = "N/A"
            if (width > 0 && height > 0 && fps > -1) {
                inputValue = """${width}x$height,
$fps Fps"""
            }
            videoResolutionView!!.updateUIOnScreenInputValue(inputValue)

            // update input video res on widthxheight on UI
            val len = captureFormats.size - 1
            var currentIndexWH = 0
            for (i in captureFormats.indices) {
                if (captureFormats[i]!!.width == width && captureFormats[i]!!.height == height) {
                    currentIndexWH = i
                    break
                }
            }
            if (!hasInformedWHFirstValueScreen) {
                val maxWHValue = captureFormats[0]!!.width.toString() + "x" + captureFormats[0]!!.height
                val minWHValue = captureFormats[len]!!.width.toString() + "x" + captureFormats[0]!!.height
                val WHValue = width.toString() + "x" + height
                videoResolutionView!!.updateUIOnScreenInputWHValue(len, maxWHValue, minWHValue, WHValue, currentIndexWH)
                hasInformedWHFirstValueScreen = true
            }

            // update input video res on Fps on UI
            if (!hasInformedFpsFirstValueScreen) {
                videoResolutionView!!.updateUIOnScreenInputFpsValue(maxFps.toString(), minFps.toString(), fps.toString())
                hasInformedFpsFirstValueScreen = true
            }
        }
    }

    override fun processReceivedVideoResolutionObtained(peerId: String?, mediaType: SkylinkMedia.MediaType?, width: Int, height: Int, fps: Int) {
        if (mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
            // Update UI about received resolution from remote peer
            videoResolutionView!!.updateUIOnCameraReceivedValue(width, height, fps)
        } else if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            videoResolutionView!!.updateUIOnScreenReceivedValue(width, height, fps)
        }
    }

    override fun processSentVideoResolutionObtained(peerId: String?, mediaType: SkylinkMedia.MediaType?, width: Int, height: Int, fps: Int) {
        if (mediaType == SkylinkMedia.MediaType.VIDEO_CAMERA) {
            // Update UI about sent resolution to remote peer
            videoResolutionView!!.updateUIOnCameraSentValue(width, height, fps)
        } else if (mediaType == SkylinkMedia.MediaType.VIDEO_SCREEN) {
            videoResolutionView!!.updateUIOnScreenSentValue(width, height, fps)
        }
    }

}