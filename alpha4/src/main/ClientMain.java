package main;

import a.Config;
import a.DiscoveryLocator;
import a.MessageLocator;
import a.manager.MessagesManager;
import a.manager.PeerManager;

public class ClientMain {
    public static void main(String[] args) {
        String peerId = "barash";
        PeerManager peerManager = new PeerManager();
        MessagesManager messagesManager = new MessagesManager(100);
        new DiscoveryLocator(Config.BROADCAST_ADDRESS, Config.PORT, peerId, peerManager).start();
        new MessageLocator(peerId, messagesManager, peerManager).start();
    }
}
