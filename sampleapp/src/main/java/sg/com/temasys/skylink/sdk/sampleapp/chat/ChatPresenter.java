package sg.com.temasys.skylink.sdk.sampleapp.chat;

import android.content.Context;
import android.util.Log;

import sg.com.temasys.skylink.sdk.sampleapp.data.model.PermRequesterInfo;
import sg.com.temasys.skylink.sdk.sampleapp.data.model.SkylinkPeer;
import sg.com.temasys.skylink.sdk.sampleapp.data.service.ChatService;

/**
 * Created by muoi.pham on 20/07/18.
 */

public abstract class ChatPresenter implements ChatContract.Presenter {

    private final String TAG = ChatPresenter.class.getName();

    public ChatContract.View mChatView;

    private ChatService mChatService;

    public ChatPresenter(ChatContract.View chatView, Context context) {
        this.mChatView = chatView;
        mChatService = new ChatService(context);

        this.mChatView.setPresenter(this);
//        this.mChatService.setPresenter(this);
    }

    //---------------------------Listener to work with ChatFragment---------------------------------

    @Override
    public void setRoomDetailsPresenterHandler() {
        updateRoomDetailsPresenterHandler();
    }

    //---------------------------Listener to work with ChatService----------------------------------

    @Override
    public void sendServerMessagePresenterHandler(String remotePeerId, String message) {
//        mChatService.sendServerMessageServiceHandler(remotePeerId, message);
    }

    @Override
    public void sendP2PMessagePresenterHandler(String remotePeerId, String message){
//        mChatService.sendP2PMessageServiceHandler(remotePeerId, message);
    }

    @Override
    public void disconnectFromRoomPresenterHandler(){
//        mChatService.disconnectFromRoomServiceHandler();
    }

    @Override
    public void connectToRoomPresenterHandler(){
//        mChatService.connectToRoomServiceHandler();
    }


    @Override
    public boolean isConnectingOrConnectedPresenterHandler() {
//        return mChatService.isConnectingOrConnectedServiceHandler();
        return false;
    }

    @Override
    public void onMessageReceivePresenterHandler(String msg) {
        mChatView.addToChatMessageCollectionViewHandler(msg);
        mChatView.listViewRefreshViewHandler();
    }


    @Override
    public void onViewLayoutRequestedPresenterHandler() {

    }

    @Override
    public void onViewExitPresenterHandler() {

    }

    public void onConnectPresenterHandler() {

        updateUIPresenterHandler();

    }

    @Override
    public void onDisconnectPresenterHandler() {
        // Reset peerList

        mChatView.clearPeerListViewHandler();

        mChatView.clearChatMessageCollectionViewHandler();

        updateRoomDetailsPresenterHandler();

    }

    @Override
    public void onRemotePeerJoinPresenterHandler(SkylinkPeer skylinkPeer) {

        mChatView.addPeerRadioBtnViewHandler(skylinkPeer);

        //Set room status if it's the only peer in the room.
        if (mChatView.getPeerNumViewHandler() == 1) {
            updateRoomDetailsPresenterHandler();
        }
    }

    @Override
    public void onRemotePeerLeavePresenterHandler(String remotePeerId) {

        mChatView.removePeerRadioBtnViewHandler(remotePeerId);

        //Set room status if there are no more peers.
        if (mChatView.getPeerListSizeViewHandler() == 0) {
            updateRoomDetailsPresenterHandler();
        }
    }

//    @Override
    public void onPermissionRequiredPresenterHandler(PermRequesterInfo info) {

    }

    //    @Override
    public void onViewLayoutRequestedPresenterHandler(boolean tryToConnect) {
        Log.d(TAG, "onViewLayoutRequestedPresenterHandler");

        if (tryToConnect) {
            //start to connect to room when entering room
            //if not being connected, then connect
//            if (!mChatService.isConnectingOrConnectedServiceHandler()) {
//
//                //connect to room on Skylink connection
//                mChatService.connectToRoomServiceHandler();
//
//                //after connected to skylink SDK, UI will be updated latter on AudioService.onConnect
//
//                Log.d(TAG, "Try to connect when entering room");
//
//            } else {
//
//                //update UI into connected
//                updateUIPresenterHandler();
//
//                Log.d(TAG, "Try to update UI when changing configuration");
//            }

        } else {
            //process disconnect from room
//            mChatService.disconnectFromRoomServiceHandler();

            //after disconnected from skylink SDK, UI will be updated latter on AudioService.onDisconnect

            Log.d(TAG, "Try to disconnect from room");
        }
    }

    private void updateUIPresenterHandler() {
        mChatView.listViewRefreshViewHandler();

        mChatView.fillPeerRadioBtnViewHandler();

        updateRoomDetailsPresenterHandler();
    }

    private void updateRoomDetailsPresenterHandler(){
//        String strRoomDetails = mChatService.getRoomDetailsServiceHandler();
//        mChatView.onUpdateUIViewHandler(strRoomDetails);
    }

}
