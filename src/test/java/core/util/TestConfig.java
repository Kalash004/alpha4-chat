package core.util;

/**
 * Application test values constants and utility methods to side-load.
 */
public interface TestConfig {
    String DEFAULT_PROPERTIES_FILE = "default.properties";

    String PROP_PEER_ID = "peer-id";
    String PROP_HISTORY_LIMIT = "history-limit";
    String PROP_DEFAULT_PACKET_BUFFER_LENGTH = "default-packet-buffer-length";
    String PROP_BROADCAST_PORT = "broadcast-port";
    String PROP_MSG_PORT = "messaging-port";
    String PROP_API_PORT = "http-api-port";
    String PROP_BROADCAST_ADDRESS = "broadcast-address-subnet";
    String PROP_BROADCAST_TIMEOUT_MS = "broadcast-timeout-ms";
    String PROP_PEER_TIMEOUT_MS = "peer-timeout-ms";

    int HISTORY_LIMIT = 100;
    int DEFAULT_PACKET_BUFFER = 4096;
    int MSG_PORT = 9876;
    int API_PORT = 8000;
    String BROADCAST_ADDRESS = "192.168.50.255";
    int BROADCAST_TIMEOUT_MS = 5_000;
    int PEER_TIMEOUT_MS = BROADCAST_TIMEOUT_MS * 3;
}
