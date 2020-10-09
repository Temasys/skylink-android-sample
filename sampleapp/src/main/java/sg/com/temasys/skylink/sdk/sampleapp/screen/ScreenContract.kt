package sg.com.temasys.skylink.sdk.sampleapp.screen

import androidx.fragment.app.Fragment
import sg.com.temasys.skylink.sdk.sampleapp.BaseService
import sg.com.temasys.skylink.sdk.sampleapp.BaseView
import sg.com.temasys.skylink.sdk.sampleapp.service.model.SkylinkPeer

/**
 * Created by muoi.pham on 20/07/18.
 * This interface is responsible for specify behaviors of View, Presenter, Service
 */
interface ScreenContract {
    interface View : BaseView<Presenter?> {
        /**
         * Get instance of the fragment for processing runtime audio permission
         */
        val instance: Fragment?

        /**
         * Update UI into connected state
         */
        fun updateUIConnected(roomId: String?)

        /**
         * Update UI into disconnected state
         */
        fun updateUIDisconnected()

        /**
         * Update UI when remote peer join the room
         */
        fun updateUIRemotePeerConnected(newPeer: SkylinkPeer?, index: Int)

        /**
         * Update UI details when remote peer left the room
         */
        fun updateUIRemotePeerDisconnected(peersList: List<SkylinkPeer?>?)

        /**
         * Update audio output button UI
         */
        fun updateUIAudioOutputChanged(isSpeakerOn: Boolean)
    }

    interface Presenter {
        /**
         * process data to display on view at initiative connected
         */
        fun processConnectedLayout()

        /**
         * process permission result (grant/deny)
         */
        fun processPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray?)

        /**
         * process change audio output between headset and speaker
         */
        fun processChangeAudioOutput()

        /**
         * process change state when view exit/closed
         */
        fun processExit()

        /**
         * process get peer info at specific index
         */
        fun processGetPeerByIndex(index: Int): SkylinkPeer?
    }

    interface Service : BaseService<Presenter?>
}