package sg.com.temasys.skylink.sdk.rtc;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by janidu on 12/5/15.
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class UpdateUserEventMessageProcessorTest {

    private static final String peerId = "1234";
    private static final String userName = "testUserName";

    private UpdateUserEventMessageProcessor userEventMessgeProcesor;
    private SkylinkConnection skylinkConnection;
    private SkylinkConnectionService skylinkConnectionService;
    private SkylinkPeerService mockSkylinkPeerService;
    private Peer peer;

    @Before
    public void setUp() throws Exception {
        skylinkConnection = spy(SkylinkConnection.getInstance());
        peer = new Peer(peerId, skylinkConnection);
        skylinkConnectionService = mock(SkylinkConnectionService.class);
        userEventMessgeProcesor = new UpdateUserEventMessageProcessor();
        userEventMessgeProcesor.setSkylinkConnection(skylinkConnection);

        mockSkylinkPeerService = mock(SkylinkPeerService.class);
        when(mockSkylinkPeerService.getPeer(peerId)).thenReturn(peer);

        skylinkConnection.setSkylinkPeerService(mockSkylinkPeerService);
    }

    @Test
    public void testProcess() throws Exception {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mid", peerId);
        jsonObject.put("userData", userName);

        doReturn(skylinkConnectionService)
                .when(skylinkConnection).getSkylinkConnectionService();
        doReturn(SkylinkConnectionService.ConnectionState.CONNECTING)
                .when(skylinkConnectionService).getConnectionState();


        RemotePeerListener remotePeerListener = new RemotePeerListener() {
            @Override
            public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
                fail();
            }

            @Override
            public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
                assertTrue(peerId.equals(remotePeerId));
                assertNotNull(userData);
                assertTrue(userName.equals(userName));
            }

            @Override
            public void onOpenDataConnection(String remotePeerId) {
                fail();
            }

            @Override
            public void onRemotePeerLeave(String remotePeerId, String message) {
                fail();
            }
        };

        doReturn(remotePeerListener).when(skylinkConnection).getRemotePeerListener();
        userEventMessgeProcesor.process(jsonObject);
        verify(mockSkylinkPeerService).setUserData(peerId, userName);
    }
}