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
import static org.mockito.Mockito.spy;

/**
 * Created by janidu on 12/5/15.
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class UpdateUserEventMessageProcessorTest {

    private static final String peerId = "1234";
    private static final String userName = "testUserName";

    private SkylinkConnection skylinkConnection;
    private UpdateUserEventMessageProcessor userEventMessgeProcesor;

    @Before
    public void setUp() throws Exception {
        skylinkConnection = spy(SkylinkConnection.getInstance());
        userEventMessgeProcesor = new UpdateUserEventMessageProcessor();
        userEventMessgeProcesor.setSkylinkConnection(skylinkConnection);
    }

    @Test
    public void testProcess() throws Exception {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mid", peerId);
        jsonObject.put("userData", userName);

        doReturn(SkylinkConnection.ConnectionState.CONNECT).when
                (skylinkConnection).getConnectionState();

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
    }
}