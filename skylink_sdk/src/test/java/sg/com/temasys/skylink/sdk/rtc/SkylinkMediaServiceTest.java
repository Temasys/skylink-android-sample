package sg.com.temasys.skylink.sdk.rtc;

/**
 * Created by xiangrong on 5/6/15.
 */

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.webrtc.VideoCapturerAndroid;

import java.util.concurrent.CountDownLatch;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Tests related to SkylinkConnection
 */

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)

public class SkylinkMediaServiceTest {

    private static final String TAG = SkylinkMediaServiceTest.class.getName();
    private SkylinkConnection skylinkConnection;
    private SkylinkConfig skylinkConfig;
    private SkylinkConnectionService skylinkConnectionService;
    private SkylinkMediaService skylinkMediaService;
    private PcShared mockPcShared;
    private LifeCycleListener lifeCycleListener;
    private VideoCapturerAndroid localVideoCapturer;
    private CountDownLatch counter;
    private boolean shouldMirror = true;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        skylinkConnection = mock(SkylinkConnection.class);
        skylinkConfig = mock(SkylinkConfig.class);
        skylinkConnectionService = mock(SkylinkConnectionService.class);
        mockPcShared = mock(PcShared.class);

        when(skylinkConfig.isMirrorLocalView()).thenReturn(shouldMirror);
        when(skylinkConnection.getSkylinkConfig()).thenReturn(skylinkConfig);

        localVideoCapturer = mock(VideoCapturerAndroid.class);
        // when(skylinkConnection.getLocalVideoCapturer()).thenReturn(localVideoCapturer);
        when(localVideoCapturer.switchCamera(null)).thenReturn(true);

        skylinkMediaService = spy(new SkylinkMediaService(
                skylinkConnection, mockPcShared));
        skylinkMediaService.setLocalVideoCapturer(localVideoCapturer);

        counter = new CountDownLatch(1);
    }

    @Test
    public void testCreateSkylinkMediaService() {
        assertNotNull(skylinkMediaService);
    }

    // TODO
    /*@Test
    public void testSwitchCameraWithNoMirroring() throws InterruptedException {
        // Prepare test objects
        //Set Camera number to 1.
        skylinkMediaService.setNumberOfCameras(1);

        lifeCycleListener = new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("onConnect should not be called.");
                counter.countDown();
            }

            @Override
            public void onWarning(int errorCode, String message) {
                assertEquals(ErrorCodes.VIDEO_SWITCH_CAMERA_ERROR, errorCode);
                counter.countDown();
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                fail("onDisconnect should not be called.");
                counter.countDown();
            }

            @Override
            public void onReceiveLog(String message) {
                fail("onReceiveLog should not be called.");
                counter.countDown();
            }

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail("onLockRoomStatusChange should not be called.");
                counter.countDown();
            }
        };

        // Test method
        boolean success = false;
        success = skylinkMediaService.switchCameraAndRender(lifeCycleListener);
        counter.await();
        // Ensure positive outcome method NOT called.
        assertFalse(success);
    }*/

    // TODO
    /*@Test
    public void testSwitchCameraWithMirroring() throws InterruptedException {
        // Prepare test objects
        //Set Camera number to 2.
        final int numCam = 2;
        skylinkMediaService.setNumberOfCameras(numCam);

        lifeCycleListener = new LifeCycleListener() {
            @Override
            public void onConnect(boolean isSuccessful, String message) {
                fail("onConnect should not be called.");
            }

            @Override
            public void onWarning(int errorCode, String message) {
                fail("onWarning should not be called as " + numCam + " cameras are set.");
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                fail("onDisconnect should not be called.");
            }

            @Override
            public void onReceiveLog(String message) {
                fail("onReceiveLog should not be called.");
            }

            @Override
            public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
                fail("onLockRoomStatusChange should not be called.");
            }
        };

        // Test method
        boolean success = false;
        success = skylinkMediaService.switchCamera(lifeCycleListener);
        // Ensure positive outcome method IS called.
        assertTrue(success);
    }*/

    @Test
    public void testSwitchCameraWith0Cameras() {
        // Prepare test objects
        //Set Camera number to 0.
        final int numCam = 0;
        skylinkMediaService.setNumberOfCameras(numCam);

        // Test method
        String strLog = null;
        strLog = skylinkMediaService.switchCamera();
        // Ensure positive outcome NOT true.
        assertFalse(strLog == null);
    }

    @Test
    public void testSwitchCameraWith1Cameras() {
        // Prepare test objects
        //Set Camera number to 1.
        final int numCam = 1;
        skylinkMediaService.setNumberOfCameras(numCam);

        // Test method
        String strLog = null;
        strLog = skylinkMediaService.switchCamera();
        // Ensure positive outcome NOT true.
        assertFalse(strLog == null);
    }

    @Test
    public void testSwitchCameraWith2Cameras() {
        // Prepare test objects
        //Set Camera number to 2.
        final int numCam = 2;
        skylinkMediaService.setNumberOfCameras(numCam);

        // Test method
        String strLog = null;
        strLog = skylinkMediaService.switchCamera();
        // Ensure positive outcome IS true.
        assertTrue(strLog == null);
    }

}
