package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import sg.com.temasys.skylink.sdk.config.SkylinkConfig;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests related to SkylinkPeerService
 */
@Config(emulateSdk = 18)
@RunWith(JmockitRunner.class)
public class SkylinkPeerServiceTest {

    private static final String TAG = SkylinkPeerServiceTest.class.getSimpleName();

    private static final String peerId = "1234";
    private static final String EXPECTED_URI = "testUri";
    private static final String EXPECTED_PWD = "testPassword";
    private static final String EXPECTED_USERNAME = "testUsername";
    private String TEST_USERDATA = "UserData";

    @Mocked
    private PeerConnection peerConnection;

    @Mocked({"sendWelcome"})
    ProtocolHelper protocolHelper;

    private SkylinkPeerService skylinkPeerService;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testReceivedEnterWithAudioSend(@Mocked final SkylinkConnection
                                                       skylinkConnection) throws JSONException {

        final UserInfo userInfo = new UserInfo();
        final PeerInfo peerInfo = new PeerInfo();

        final SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_ONLY);

        final Map testMap = new HashMap<String, PeerInfo>();

        new Expectations() {{
            skylinkConnection
                    .getPeerConnection(peerId, HealthChecker.ICE_ROLE_ANSWERER);
            result = peerConnection;

            skylinkConnection.setUserInfo(peerId, userInfo);
            result = null;

            skylinkConnection.getPeerInfoMap();
            result = testMap;

            testMap.put(peerId, peerInfo);
            result = null;

            skylinkConnection.getMyConfig();
            result = skylinkConfig;

            skylinkConnection.getLocalMediaStream();
        }};

        skylinkPeerService = new SkylinkPeerService(skylinkConnection);
        skylinkPeerService.receivedEnter(peerId, peerInfo, userInfo);

        new Verifications() {{
            peerConnection.addStream((MediaStream) any);
            protocolHelper.sendWelcome(peerId, skylinkConnection, false);
        }};
    }

    @Test
    public void testReceivedEnterWithVideoSend(@Mocked final SkylinkConnection
                                                       skylinkConnection) throws JSONException {

        final UserInfo userInfo = new UserInfo();
        final PeerInfo peerInfo = new PeerInfo();

        final SkylinkConfig skylinkConfig = new SkylinkConfig();
        skylinkConfig.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.VIDEO_ONLY);

        final Map testMap = new HashMap<String, PeerInfo>();

        new Expectations() {{
            skylinkConnection
                    .getPeerConnection(peerId, HealthChecker.ICE_ROLE_ANSWERER);
            result = peerConnection;

            skylinkConnection.setUserInfo(peerId, userInfo);
            result = null;

            skylinkConnection.getPeerInfoMap();
            result = testMap;

            testMap.put(peerId, peerInfo);
            result = null;

            skylinkConnection.getMyConfig();
            result = skylinkConfig;

            skylinkConnection.getMyConfig();
            result = skylinkConfig;

            skylinkConnection.getLocalMediaStream();
        }};

        skylinkPeerService = new SkylinkPeerService(skylinkConnection);
        skylinkPeerService.receivedEnter(peerId, peerInfo, userInfo);

        new Verifications() {{
            peerConnection.addStream((MediaStream) any);
            protocolHelper.sendWelcome(peerId, skylinkConnection, false);
        }};
    }

    @Test
    public void testReceivedBye() {

        SkylinkConnection skylinkConnection = spy(SkylinkConnection.getInstance());

        DataChannelManager dataChannelManager = mock(DataChannelManager.class);

        when(skylinkConnection.isPeerIdMCU(peerId)).thenReturn(false);
        when(skylinkConnection.getConnectionState()).thenReturn(SkylinkConnection
                .ConnectionState.CONNECT);

        when(skylinkConnection.getDataChannelManager()).thenReturn(dataChannelManager);
        when(skylinkConnection.getPeerConnectionPool()).thenReturn(new HashMap<String, PeerConnection>());

        skylinkConnection.setRemotePeerListener(new RemotePeerListener() {
            @Override
            public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {

            }

            @Override
            public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {

            }

            @Override
            public void onOpenDataConnection(String remotePeerId) {

            }

            @Override
            public void onRemotePeerLeave(String remotePeerId, String message) {
                assertEquals(peerId, remotePeerId);
                assertTrue(message.equals("The peer has left the room"));
            }
        });

        SkylinkPeerService skylinkPeerService = new SkylinkPeerService(skylinkConnection);
        skylinkPeerService.receivedBye(peerId);
        verify(dataChannelManager).disposeDC(peerId);
    }

    @Test
    public void testAddIceCandidate(@Mocked final SkylinkConnection skylinkConnection) {

        new Expectations() {{
            skylinkConnection.getPeerConnection(peerId);
            result = peerConnection;
        }};

        skylinkPeerService = new SkylinkPeerService(skylinkConnection);
        skylinkPeerService.addIceCandidate(peerId, new IceCandidate("", 1, ""));

        new Verifications() {{
            peerConnection.addIceCandidate((IceCandidate) any);
        }};
    }

    @Test
    public void testReceivedInRoom() {

        WebServerClient.IceServersObserver iceServersObserver = new WebServerClient.IceServersObserver() {
            @Override
            public void onIceServers(List<PeerConnection.IceServer> iceServers) {
                assertTrue(iceServers.size() == 1);
                assertTrue(iceServers.get(0).uri.equals(EXPECTED_URI));
                assertTrue(iceServers.get(0).password.equals(EXPECTED_PWD));
                assertTrue(iceServers.get(0).username.equals(EXPECTED_USERNAME));
            }

            @Override
            public void onError(String message) {
                fail(message);
            }

            @Override
            public void onShouldConnectToRoom() {
                fail();
            }
        };

        SkylinkConnection skylinkConnection = mock(SkylinkConnection.class);

        SkylinkConnectionService skylinkConnectionService = mock(SkylinkConnectionService.class);
        when(skylinkConnection.getSkylinkConnectionService()).thenReturn(skylinkConnectionService);

        when(skylinkConnection.getIceServersObserver()).thenReturn(iceServersObserver);

        when(skylinkConnection.getMyUserData()).thenReturn(TEST_USERDATA);
        when(skylinkConnection.getPcObserverPool()).thenReturn(new
                HashMap<String, SkylinkConnection.PCObserver>());

        DataChannelManager dataChannelManager = mock(DataChannelManager.class);
        when(skylinkConnection.getDataChannelManager()).thenReturn(dataChannelManager);

        List iceServers = new ArrayList<PeerConnection.IceServer>();
        iceServers.add(new PeerConnection.IceServer(EXPECTED_URI,
                EXPECTED_USERNAME, EXPECTED_PWD));

        SkylinkPeerService skylinkPeerService = new SkylinkPeerService(skylinkConnection);
        skylinkPeerService.receivedInRoom(peerId, iceServers);

        verify(skylinkConnectionService).setSid(peerId);
        verify(dataChannelManager).setMid(peerId);
        verify(dataChannelManager).setDisplayName(TEST_USERDATA);

        // Should call rejoinRestart since pCObserverPool is not null
        verify(skylinkConnection).rejoinRestart();

        when(skylinkConnection.getPcObserverPool()).thenReturn(null);
        when(skylinkConnection.getMyConfig()).thenReturn(new SkylinkConfig());
        when(skylinkConnection.getUserInfoMap()).thenReturn(new HashMap<String, UserInfo>());

        skylinkPeerService.receivedInRoom(peerId, iceServers);

        // Should call initializePcRelatedMaps
        verify(skylinkConnection).initializePcRelatedMaps();
    }

    @Test
    public void testReceivedOfferAnswer(@Mocked final SkylinkConnection skylinkConnection) {

        new Expectations() {{
            skylinkConnection.getPeerConnection(peerId);
            result = peerConnection;

            skylinkConnection.getMyConfig();
            result = new SkylinkConfig();

            skylinkConnection.getSdpObserver(peerId);
        }};

        skylinkPeerService = new SkylinkPeerService(skylinkConnection);
        skylinkPeerService.receivedOfferAnswer(peerId, "testSDP", "offer");

        new Verifications() {{
            peerConnection.setRemoteDescription(
                    (org.webrtc.SdpObserver) any, (SessionDescription) any);
        }};
    }

}
