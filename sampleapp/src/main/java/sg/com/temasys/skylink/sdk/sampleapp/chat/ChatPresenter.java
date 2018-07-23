package sg.com.temasys.skylink.sdk.sampleapp.chat;

import android.content.Context;

import sg.com.temasys.skylink.sdk.sampleapp.data.service.ChatService;

/**
 * Created by muoi.pham on 20/07/18.
 */

public class ChatPresenter implements ChatContract.Presenter {

    public ChatContract.View mChatView;

    private ChatService mChatService;

    public ChatPresenter(ChatContract.View chatView, Context context) {
        this.mChatView = chatView;
        mChatService = new ChatService(context);

        this.mChatView.setPresenter(this);
        this.mChatService.setPresenter(this);
    }

    //---------------------------Listener to work with ChatFragment---------------------------------
    @Override
    public void fillPeerRadioBtnPresenterHandler() {
        mChatView.fillPeerRadioBtnViewHandler();
    }

    @Override
    public void clearPeerListPresenterHandler(){
        mChatView.clearPeerListViewHandler();
    }

    @Override
    public void addPeerRadioBtnPresenterHandler(String remotePeerId, String nick){
        mChatView.addPeerRadioBtnViewHandler(remotePeerId, nick);
    }

    @Override
    public int getPeerNumPresenterHandler(){
        return mChatView.getPeerNumViewHandler();
    }

    @Override
    public void removePeerRadioBtnPresenterHandler(String remotePeerId){
        mChatView.removePeerRadioBtnViewHandler(remotePeerId);
    }

    @Override
    public int getPeerListSizePresenterHandler(){
        return mChatView.getPeerListSizeViewHandler();
    }

    @Override
    public void listViewRefreshPresenterHandler(){
        mChatView.listViewRefreshViewHandler();
    }

    @Override
    public void setRoomDetailsPresenterHandler(boolean isPeerInRoom) {
        String roomDetails = mChatService.getRoomDetailsServiceHandler(isPeerInRoom);
        mChatView.setRoomDetailsViewHandler(roomDetails);
    }

    @Override
    public void setMultiChatPeersInfoPresenterHandler(boolean isPeerJoined) {
        mChatView.setMultiChatPeersInfoViewHandler(isPeerJoined);
    }

    @Override
    public void clearChatMessageCollectionPresenterHandler() {
        mChatView.clearChatMessageCollectionViewHandler();
    }

    @Override
    public void addToChatMessageCollectionPresenterHandler(String s) {
        mChatView.addToChatMessageCollectionViewHandler(s);
    }


    //---------------------------Listener to work with ChatService----------------------------------

    @Override
    public void sendServerMessagePresenterHandler(String remotePeerId, String message) {
        mChatService.sendServerMessageServiceHandler(remotePeerId, message);
    }

    @Override
    public void sendP2PMessagePresenterHandler(String remotePeerId, String message){
        mChatService.sendP2PMessageServiceHandler(remotePeerId, message);
    }

    @Override
    public void disconnectFromRoomPresenterHandler(){
        mChatService.disconnectFromRoomServiceHandler();
    }

    @Override
    public void connectToRoomPresenterHandler(){
        mChatService.connectToRoomServiceHandler();
    }

    @Override
    public void saveIsPeerJoinPresenterHandler(boolean isPeerJoined) {
        mChatService.saveIsPeerJoinServiceHandler(isPeerJoined);
    }

    @Override
    public boolean isConnectingOrConnectedPresenterHandler() {
        return mChatService.isConnectingOrConnectedServiceHandler();
    }

}
