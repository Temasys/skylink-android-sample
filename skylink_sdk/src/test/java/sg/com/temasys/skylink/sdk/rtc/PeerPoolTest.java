package sg.com.temasys.skylink.sdk.rtc;

import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by janidu on 14/7/15.
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class PeerPoolTest implements PeerPoolClient {

    private static final String TAG = PeerPoolTest.class.getName();
    private static final int MAX_PEER_COUNT = 10;
    private static final String PEER_ID = "PeerId_";
    private PeerPool peerPool;

    @Before
    public void setup() {
        peerPool = new PeerPool(this);
    }

    @Test
    public void testAddPeer() {
        addPeers();

        assertEquals(MAX_PEER_COUNT, peerPool.getPeerNumber());

        Peer peer = mock(Peer.class);
        when(peer.getPeerId()).thenReturn(PEER_ID + (MAX_PEER_COUNT + 1));

        assertFalse("Should not be able to add pass " + MAX_PEER_COUNT, peerPool.addPeer(peer));
        assertFalse("Should not be able to add pass " + MAX_PEER_COUNT, peerPool.canAddPeer());
    }

    @Test
    public void testRemovePeer() {
        addPeers();
        peerPool.removePeer(PEER_ID + 0);
        assertEquals(MAX_PEER_COUNT - 1, peerPool.getPeerNumber());
    }

    @Test
    public void testGetPeer() {
        Peer peer = mock(Peer.class);
        when(peer.getPeerId()).thenReturn(PEER_ID);
        peerPool.addPeer(peer);
        assertEquals(peer, peerPool.getPeer(PEER_ID));
    }

    @Test
    public void testGetPeerIdSet() {
        Peer peer = mock(Peer.class);

        when(peer.getPeerId()).thenReturn(PEER_ID);
        peerPool.addPeer(peer);

        assertEquals(peer, peerPool.getPeer(PEER_ID));
        assertNotNull(peerPool.getPeerIdSet());
        assertTrue(peerPool.getPeerIdSet().contains(PEER_ID));
    }

    @Test
    public void testGetPeerCollection() {
        Peer peer = mock(Peer.class);

        when(peer.getPeerId()).thenReturn(PEER_ID);
        peerPool.addPeer(peer);

        assertEquals(peer, peerPool.getPeer(PEER_ID));
        assertNotNull(peerPool.getPeerCollection());
        assertTrue(peerPool.getPeerCollection().contains(peer));
    }

    @Test
    /**
     * Create multiple Peers and test that they can be concurrently added.
     * Remove these same Peers and test that they can be concurrently removed.
     */
    public void testPeerPoolConcurrent() throws InterruptedException, BrokenBarrierException {

        final CyclicBarrier barrierReady = new CyclicBarrier(MAX_PEER_COUNT);
        final CyclicBarrier barrierDone = new CyclicBarrier(MAX_PEER_COUNT + 1);

        // Start multiple threads to add Peers.
        for (int i = 0; i < MAX_PEER_COUNT; ++i) {
            Thread thread = getThread(barrierReady, barrierDone, i, true);
            thread.start();
        }

        barrierDone.await();
        // Test that the required number of Peers have been added.
        assertEquals(MAX_PEER_COUNT, peerPool.getPeerNumber());

        // Start multiple threads to remove Peers.
        for (int i = 0; i < MAX_PEER_COUNT; ++i) {
            Thread thread = getThread(barrierReady, barrierDone, i, false);
            thread.start();
        }

        barrierDone.await();
        // Test that the required Peers have been removed.
        assertEquals(0, peerPool.getPeerNumber());
    }

    /**
     * Create thread to add or remove Peer when all Peers are ready to add.
     * Assert that add or remove Peer is successful.
     *
     * @param barrierReady
     * @param barrierDone
     * @param id
     * @param addPeers
     * @return
     */
    private Thread getThread(final CyclicBarrier barrierReady, final CyclicBarrier
            barrierDone, final int id, final boolean addPeers) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                String peerId = PEER_ID + id;
                Peer peer = mock(Peer.class);
                when(peer.getPeerId()).thenReturn(peerId);
                try {
                    barrierReady.await();
                } catch (BrokenBarrierException e) {
                    Log.d(TAG, e.getMessage());
                } catch (InterruptedException e) {
                    Log.d(TAG, e.getMessage());
                }

                if (addPeers) {
                    assertTrue(peerPool.addPeer(peer));
                } else {
                    assertNotNull(peerPool.removePeer(peerId));
                }

                try {
                    barrierDone.await();
                } catch (InterruptedException e) {
                    Log.d(TAG, e.getMessage());
                } catch (BrokenBarrierException e) {
                    Log.d(TAG, e.getMessage());
                }
            }
        });
    }

    @Override
    public int getMaxPeers() {
        return MAX_PEER_COUNT;
    }

    private void addPeers() {
        for (int i = 0; i < MAX_PEER_COUNT; i++) {
            Peer peer = mock(Peer.class);
            when(peer.getPeerId()).thenReturn(PEER_ID + i);
            assertTrue("Should be able to add until " + MAX_PEER_COUNT, peerPool.addPeer(peer));
        }
    }
}
