package core.io;

import core.manager.PeerManager;
import core.model.Peer;
import core.util.TestConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DiscoveryClientTest {

    private static final String SERVER_PEER_ID = "ServerPeerId";
    private static final String CLIENT_PEER_ID = "ClientPeerId";

    private static String LOCAL_IP_ADDRESS;
    private static int localPort;

    private DiscoveryServer server;

    @BeforeClass
    public static void beforeClass() throws IOException {

        LOCAL_IP_ADDRESS = InetAddress.getLoopbackAddress().getHostAddress();
    }

    @Before
    public void before() throws IOException {
        // Take an available port
        ServerSocket s = new ServerSocket(0);
        localPort = s.getLocalPort();
        s.close();

        server = new DiscoveryServer(TestConfig.DEFAULT_PACKET_BUFFER, localPort, SERVER_PEER_ID);
        new Thread(server).start();
    }

    @After
    public void after() {
        server.stop();
    }

    @Test
    public void testDiscoveryRequest() {
        int timeout = TestConfig.BROADCAST_TIMEOUT_MS;
        PeerManager peerMgr = new PeerManager(TestConfig.PEER_TIMEOUT_MS);

        DiscoveryClient client = new DiscoveryClient(TestConfig.DEFAULT_PACKET_BUFFER, LOCAL_IP_ADDRESS, localPort,
                timeout, CLIENT_PEER_ID, peerMgr);

        new Thread(client).start();
        // sleep a bit to allow client to reach out server and get response back
        sleep(timeout / 3);
        client.stop();
        assertTrue(!peerMgr.getPeers().isEmpty());
        Peer serverPeer = peerMgr.getPeers().stream().filter(peer -> peer.peerId().equals(SERVER_PEER_ID)).findAny()
                .orElse(null);
        assertNotNull(serverPeer);
    }

    @Test
    public void testPeerExpired() {
        int broadcastTimeout = TestConfig.BROADCAST_TIMEOUT_MS;
        int peerTimeout = TestConfig.PEER_TIMEOUT_MS;

        PeerManager peerMgr = new PeerManager(peerTimeout);

        DiscoveryClient client = new DiscoveryClient(TestConfig.DEFAULT_PACKET_BUFFER, LOCAL_IP_ADDRESS, localPort,
                broadcastTimeout, CLIENT_PEER_ID, peerMgr);

        new Thread(client).start();
        // sleep a bit to allow client to reach out server and get response back
        sleep(broadcastTimeout);
        // stop server to block responses from server to the client
        server.stop();
        // wait until server peer expired
        sleep(peerTimeout);
        client.stop();

        assertTrue(peerMgr.getPeers().isEmpty());
    }

    private void sleep(long sleepMs) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
        }
    }
}
