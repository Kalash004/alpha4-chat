package core;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

public interface Config {
    int DEFAULT_PACKET_BUFFER = 4096;

    int PORT = 9876;
    String BROADCAST_ADDRESS = "192.168.50.255";

    int BROADCAST_TIMEOUT_MS = 5_000;

    Pattern QUOTES_PATTERN = Pattern.compile("\"");

    default String readLocatorInput(BufferedReader in) throws IOException {
        StringBuilder input = new StringBuilder();
        String line;
        boolean emptyLine = false;
        while ((line = in.readLine()) != null) {
            if (line.isEmpty() && emptyLine) {
                break;
            }
            emptyLine = line.isEmpty();
            input.append(line).append("\n");
        }
        return input.toString();
    }

    default String readInput(BufferedReader in) throws IOException {
        StringBuilder input = new StringBuilder();
        String line;
        boolean emptyLine = false;
        while ((line = in.readLine()) != null) {// && (input.length() == 0 ||
            if (line.isEmpty() && emptyLine) {
                break;
            }
            emptyLine = line.isEmpty();
            input.append(line).append("\n");
        }
        // String inputLine;
        // while (in.ready() && (inputLine = in.readLine()) != null) {
        // }
        return input.toString();
    }

    default String escapeQuotes(String msg) {
        if (msg == null) {
            return null;
        }
        return QUOTES_PATTERN.matcher(msg).replaceAll("\\\\\"");
    }
}
