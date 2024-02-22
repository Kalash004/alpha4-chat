package core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Application default values constants and utility methods to side-load.
 */
public interface Config {
    int HISTORY_LIMIT = 100;

    int DEFAULT_PACKET_BUFFER = 4096;

    int MSG_PORT = 9876;
    int API_PORT = 8000;
    String BROADCAST_ADDRESS = "192.168.50.255";

    int BROADCAST_TIMEOUT_MS = 5_000;

    Pattern QUOTES_PATTERN = Pattern.compile("\"");

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
