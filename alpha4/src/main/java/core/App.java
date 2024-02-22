package core;

import core.io.ApiServer;
import core.io.DiscoveryClient;
import core.io.DiscoveryServer;
import core.io.MessagingClient;
import core.io.MessagingServer;
import core.manager.MessagesManager;
import core.manager.PeerManager;
import core.util.Config;

/**
 * Peer-to-peer messaging application.
 */
public class App {

    public static void main(String[] args) {
        // TODO: this logic needs enhancement:
        // 1. move all constants to the property file and provide path to the file either via command line or use
        // default location.
        // 2. add JVM shutdown hook to be able to stop threads gracefully

        String peerId = "barash";

        PeerManager peerManager = new PeerManager(Config.BROADCAST_TIMEOUT_MS * 3);
        MessagesManager messagesManager = new MessagesManager(Config.HISTORY_LIMIT);

        Runnable discoverySender = new DiscoveryClient(Config.BROADCAST_ADDRESS, Config.BROADCAST_TIMEOUT_MS,
                Config.MSG_PORT, peerId, peerManager);
        new Thread(discoverySender).start();

        Runnable messageSender = new MessagingClient(Config.BROADCAST_TIMEOUT_MS, peerId, messagesManager, peerManager);
        new Thread(messageSender).start();

        Runnable discoveryServer = new DiscoveryServer(Config.MSG_PORT, peerId);
        new Thread(discoveryServer).start();

        Runnable messageServer = new MessagingServer(Config.MSG_PORT, messagesManager);
        new Thread(messageServer).start();

        Runnable apiServer = new ApiServer(Config.API_PORT, peerId, messagesManager);
        new Thread(apiServer).start();

    }
}
