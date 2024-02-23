package core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Application default values constants and utility methods to side-load.
 */
public interface Config {

    /**
     * Default property file.
     */
    String DEFAULT_PROPERTIES_FILE = "default.properties";

    /**
     * Server peer id property.
     */
    String PROP_PEER_ID = "peer-id";

    /**
     * Messages history limit property.
     */
    String PROP_HISTORY_LIMIT = "history-limit";

    /**
     * Socket buffer length property.
     */
    String PROP_DEFAULT_PACKET_BUFFER_LENGTH = "default-packet-buffer-length";

    /**
     * Message broadcast port (for debug purposes only) property.
     */
    String PROP_BROADCAST_PORT = "broadcast-port";

    /**
     * Message exchange port property.
     */
    String PROP_MSG_PORT = "messaging-port";

    /**
     * HTTP API port property.
     */
    String PROP_API_PORT = "http-api-port";

    /**
     * Broadcast sub-net property.
     */
    String PROP_BROADCAST_ADDRESS = "broadcast-subnet";

    /**
     * Broadcast request timeout in milliseconds property.
     */
    String PROP_BROADCAST_TIMEOUT_MS = "broadcast-timeout-ms";

    /**
     * Peer expiration timeout property.
     */
    String PROP_PEER_TIMEOUT_MS = "peer-timeout-ms";

    default String readInput(BufferedReader in) throws IOException {
        StringBuilder input = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            input.append(line).append("\n");
        }
        return input.toString().strip();
    }

    /**
     * Trim empty bytes from the end of the array.
     *
     * @param bytes
     *            array of bytes
     *
     * @return trimmed array of bytes
     */
    default byte[] trim(byte[] bytes) {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0) {
            --i;
        }
        i++;
        i = bytes.length < i ? bytes.length : i;
        byte[] dest = new byte[i];
        System.arraycopy(bytes, 0, dest, 0, i);
        return dest;
    }

    @SuppressWarnings("unchecked")
    static <T> T getProperty(Properties prop, String key, Class<T> type) {
        String value = prop.getProperty(key);
        if (value == null) {
            return null;
        }
        if (type == Integer.class) {
            return (T) Integer.valueOf(value);
        }
        if (type == Long.class) {
            return (T) Long.valueOf(value);
        }
        if (type == String.class) {
            return (T) value;
        }
        throw new IllegalArgumentException(String.format("Unexpected property type %s", type));
    }
}
