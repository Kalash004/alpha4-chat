package core.main;

import core.Config;
import core.DiscoveryLocator;
import core.MessageLocator;
import core.manager.MessagesManager;
import core.manager.PeerManager;

public class ClientMain {
    public static void main(String[] args) {
        String peerId = "barash";
        PeerManager peerManager = new PeerManager();
        MessagesManager messagesManager = new MessagesManager(100);
        new DiscoveryLocator(Config.BROADCAST_ADDRESS, Config.PORT, peerId, peerManager).start();
        new MessageLocator(peerId, messagesManager, peerManager).start();
    }
}
