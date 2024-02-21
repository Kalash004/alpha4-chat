package main;

import a.Config;
import a.DiscoveryServer;
import a.MessageServer;
import manager.MessagesManager;
import manager.PeerManager;

public class ServerMain {
    public static void main(String[] args) {
        String peerId = "kopatych";
        PeerManager peerManager = new PeerManager();
        MessagesManager messagesManager = new MessagesManager(100);
        new DiscoveryServer(Config.PORT, peerId).start();
        new MessageServer(Config.PORT, mes.sagesManager).start();
    }
}
.