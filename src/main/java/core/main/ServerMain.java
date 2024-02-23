package core.main;

import core.io.ApiServer;
import core.io.DiscoveryServer;
import core.io.MessagingServer;
import core.manager.MessagesManager;
import core.util.Config;

/**
 * This class used for the local development only and needs to be removed.
 */
@Deprecated
public class ServerMain {
    public static void main(String[] args) {
        String peerId = "kopatych";

        MessagesManager messagesManager = new MessagesManager(Config.HISTORY_LIMIT);

        Runnable discoveryServer = new DiscoveryServer(Config.DEFAULT_PACKET_BUFFER, Config.MSG_PORT, peerId);
        new Thread(discoveryServer).start();
        Runnable messageServer = new MessagingServer(Config.MSG_PORT, messagesManager);
        new Thread(messageServer).start();

        Runnable apiServer = new ApiServer(Config.API_PORT, peerId, messagesManager);
        new Thread(apiServer).start();
    }
}
