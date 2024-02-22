package core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Application default values constants and utility methods to side-load.
 */
public interface Config {
    String DEFAULT_PROPERTIES_FILE = "default.properties";

    String PROP_PEER_ID = "peer-id";
    String PROP_HISTORY_LIMIT = "history-limit";
    String PROP_DEFAULT_PACKET_BUFFER_LENGTH = "default-packet-buffer-length";
    String PROP_MSG_PORT = "messaging-port";
    String PROP_API_PORT = "http-api-port";
    String PROP_BROADCAST_ADDRESS = "broadcast-address-subnet";
    String PROP_BROADCAST_TIMEOUT_MS = "broadcast-timeout-ms";
    String PROP_PEER_TIMEOUT_MS = "peer-timeout-ms";

    @Deprecated
    int HISTORY_LIMIT = 100;

    @Deprecated
    int DEFAULT_PACKET_BUFFER = 4096;

    @Deprecated
    int MSG_PORT = 9876;
    @Deprecated
    int API_PORT = 8000;
    @Deprecated
    String BROADCAST_ADDRESS = "192.168.50.255";

    @Deprecated
    int BROADCAST_TIMEOUT_MS = 5_000;
    @Deprecated
    int PEER_TIMEOUT_MS = BROADCAST_TIMEOUT_MS * 3;

    Pattern QUOTES_PATTERN = Pattern.compile("\"");

    @SuppressWarnings("unchecked")
    static <T> T getProperty(Properties prop, String key, Class<T> type) {
        String value = prop.getProperty(key);
        if (type == Integer.class) {
            return (T) Integer.valueOf(value);
        }
        if (type == Long.class) {
            return (T) Long.valueOf(value);
        }
        if (type == String.class) {
            return (T) value;
        }
        throw new IllegalArgumentException(String.format("Unexpected type %s", type));
    }

    default String readInput(BufferedReader in) throws IOException {
        StringBuilder input = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            input.append(line).append("\n");
        }
        return input.toString().strip();
    }

    default String escapeQuotes(String msg) {
        if (msg == null) {
            return null;
        }
        return QUOTES_PATTERN.matcher(msg).replaceAll("\\\\\"");
    }
}
