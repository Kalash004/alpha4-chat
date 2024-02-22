package core.main;

import core.io.DiscoveryClient;
import core.io.MessagingClient;
import core.manager.MessagesManager;
import core.manager.PeerManager;
import core.util.Config;

/**
 * This class used for the local development only and needs to be removed.
 */
public class ClientMain {

    public static void main(String[] args) {
        String peerId = "barash";

        PeerManager peerManager = new PeerManager(Config.BROADCAST_TIMEOUT_MS * 3);
        MessagesManager messagesManager = new MessagesManager(Config.HISTORY_LIMIT);

        Runnable discoverySender = new DiscoveryClient(Config.DEFAULT_PACKET_BUFFER, Config.BROADCAST_ADDRESS,
                Config.BROADCAST_TIMEOUT_MS, Config.MSG_PORT, peerId, peerManager);
        new Thread(discoverySender).start();

        Runnable messageSender = new MessagingClient(Config.BROADCAST_TIMEOUT_MS, peerId, messagesManager, peerManager);
        new Thread(messageSender).start();
    }
}
