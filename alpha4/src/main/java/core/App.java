package core;

import core.io.ApiServer;
import core.io.DiscoveryClient;
import core.io.DiscoveryServer;
import core.io.MessagingClient;
import core.io.MessagingServer;
import core.manager.MessagesManager;
import core.manager.PeerManager;
import core.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

/**
 * Peer-to-peer messaging application.
 */
public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        Properties prop = new Properties();
        try (InputStream is = App.class.getClassLoader().getResourceAsStream(Config.DEFAULT_PROPERTIES_FILE)) {
            prop.load(is);
        } catch (IOException e) {
            log.error("Unable to load default properties", e);
        }
        if (args.length > 0) {
            log.debug("Starting application with arguments {}", String.join(", ", args));
            String propertyFilePath = args[0];
            File file = new File(propertyFilePath);
            if (file.exists() && file.isFile() && file.canRead()) {
                log.debug("Loading properties from the property file {}", file.getAbsolutePath());
                try {
                    prop.load(new FileInputStream(file));
                } catch (FileNotFoundException e) {
                    log.warn("Unable to fine property file {}", file.getAbsolutePath(), e);
                } catch (IOException e) {
                    log.warn("Unable to load properties from property file {}", file.getAbsolutePath(), e);
                }
            }
        }
        // make sure we have user defined or unique peer id
        String peerId = prop.getProperty(Config.PROP_PEER_ID);
        if (peerId == null || peerId.isEmpty()) {
            prop.setProperty(Config.PROP_PEER_ID, UUID.randomUUID().toString());
            peerId = prop.getProperty(Config.PROP_PEER_ID);
        }

        Integer peerTimeoutMs = Config.getProperty(prop, Config.PROP_PEER_TIMEOUT_MS, Integer.class);
        Integer historyLimit = Config.getProperty(prop, Config.PROP_HISTORY_LIMIT, Integer.class);
        Integer defaultPacketBufferLength = Config.getProperty(prop, Config.PROP_DEFAULT_PACKET_BUFFER_LENGTH, Integer.class);
        String broadcastAddress = Config.getProperty(prop, Config.PROP_BROADCAST_ADDRESS, String.class);
        Integer broadcastTimeoutMs = Config.getProperty(prop, Config.PROP_BROADCAST_TIMEOUT_MS, Integer.class);
        Integer msgPort = Config.getProperty(prop, Config.PROP_MSG_PORT, Integer.class);
        Integer apiPort = Config.getProperty(prop, Config.PROP_API_PORT, Integer.class);

        PeerManager peerManager = new PeerManager(peerTimeoutMs);

        MessagesManager messagesManager = new MessagesManager(historyLimit);

        DiscoveryClient discoverySender = new DiscoveryClient(defaultPacketBufferLength, broadcastAddress,
                broadcastTimeoutMs, msgPort, peerId, peerManager);
        new Thread(discoverySender).start();

        MessagingClient messageSender = new MessagingClient(broadcastTimeoutMs, peerId, messagesManager, peerManager);
        new Thread(messageSender).start();

        DiscoveryServer discoveryServer = new DiscoveryServer(defaultPacketBufferLength, msgPort, peerId);
        new Thread(discoveryServer).start();

        MessagingServer messageServer = new MessagingServer(msgPort, messagesManager);
        new Thread(messageServer).start();

        ApiServer apiServer = new ApiServer(apiPort, peerId, messagesManager);
        new Thread(apiServer).start();

        // gracefully stop servers and senders when JVM shutdown requested
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            discoverySender.stop();
            messageSender.stop();
            discoveryServer.stop();
            messageServer.stop();
            apiServer.stop();
        }));
    }
}
