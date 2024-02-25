package core.io;

import core.manager.MessagesManager;
import core.model.Response;
import core.model.Status;
import core.util.Config;
import core.util.JsonUtil;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API server to process requests from HTTP client. Listening port {@link ApiServer#port}.
 */
public class ApiServer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ApiServer.class);

    private static final Pattern HTTP_REQUEST_LINE = Pattern.compile("^([A-Z]+)\s*(.*)(\s+HTTP/\\d\\.\\d)$");

    private final int port;
    private final String peerId;
    private final MessagesManager msgMgr;

    private boolean stop;

    /**
     * @param port
     *            API port
     * @param peerId
     *            peer Id
     * @param msgMgr
     *            messaging manager {@see MessagesManager}
     */
    public ApiServer(int port, String peerId, MessagesManager msgMgr) {
        this.port = port;
        this.peerId = peerId;
        this.msgMgr = msgMgr;
    }

    @Override
    public void run() {
        // open socket only once and then reuse it
        try (ServerSocket server = new ServerSocket(port)) {
            server.setReuseAddress(true);
            while (!stop) {
                // start new thread for each incoming request to process multiple requests in parallel
                // TODO: implement thread limits to avoid system DDOS
                new Thread(new ApiRunnable(server.accept(), peerId, msgMgr)).start();
            }
            if (stop) {
                log.info("Execution stopped");
            }
        } catch (IOException e) {
            String msg = String.format("Failed to open socket on port %s", port);
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Stop API server execution.
     */
    public void stop() {
        this.stop = true;
    }

    /**
     * Processing incoming request.
     */
    class ApiRunnable implements Runnable, Config {
        private static final Logger log = LoggerFactory.getLogger(ApiRunnable.class);

        private static final String LAST_MODIFIED_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

        private static final String MESSAGES_END_POINT = "/messages";
        private static final String SEND_MESSAGE_END_POINT = "/send?message=";

        private static final String MEDIA_APPLICATION_JSON = "application/json; charset=utf-8";
        private static final String MEDIA_TEXT_PLAIN = "text/plain; charset=utf-8";

        private static final String STATUS_OK = "200 OK";
        private static final String STATUS_BAD_REQUEST = "400 Bad Request";
        private static final String STATUS_NOT_FOUND = "404 Not Found";

        private static final String METHOD_GET = "GET";

        private final Socket socket;
        private final String peerId;
        private final MessagesManager msgMgr;

        /**
         * @param socket
         *            connected socket
         * @param peerId
         *            peer Id
         * @param msgMgr
         *            messaging manager {@see MessagesManager}
         */
        public ApiRunnable(Socket socket, String peerId, MessagesManager msgMgr) {
            this.socket = socket;
            this.peerId = peerId;
            this.msgMgr = msgMgr;
        }

        @Override
        public void run() {
            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
                String address = socket.getInetAddress().getHostAddress();
                int port = socket.getPort();
                String requestString = in.readLine();
                // just ignore rest of the stream as we don't care
                while (!in.readLine().equals("")) {
                }
                log.debug("Received request\n{}\nfrom {}:{}", requestString, address, port);
                Matcher m = HTTP_REQUEST_LINE.matcher(requestString);
                String response;
                // validate it is HTTP request, not a garbage sent to the port
                if (!m.matches()) {
                    String msg = "Invalid HTTP request " + requestString;
                    log.warn(msg);
                    response = generateResponse(STATUS_BAD_REQUEST, MEDIA_TEXT_PLAIN, new Date(), msg);
                    // validate HTTP GET request received
                } else if (!METHOD_GET.equals(m.group(1))) {
                    String method = m.group(1);
                    String msg = "Invalid HTTP method " + method;
                    log.warn("{} in the request {}", msg, requestString);
                    response = generateResponse(STATUS_BAD_REQUEST, MEDIA_TEXT_PLAIN, new Date(), msg);
                } else {
                    String queryString = m.group(2);
                    // validate called /messages
                    if (queryString.startsWith(MESSAGES_END_POINT)) {
                        String body = JsonUtil.toJson(new Response(null, msgMgr.getMessages(), null, null));
                        response = generateResponse(STATUS_OK, MEDIA_APPLICATION_JSON, new Date(), body);
                        // validate called /send message
                    } else if (queryString.startsWith(SEND_MESSAGE_END_POINT)) {
                        String message = queryString.substring(SEND_MESSAGE_END_POINT.length());
                        // validate message provided in the request
                        if (message.length() < 1) {
                            String body = "Message is empty";
                            response = generateResponse(STATUS_BAD_REQUEST, MEDIA_TEXT_PLAIN, new Date(), body);
                        } else {
                            // decode URL encoded message (see https://en.wikipedia.org/wiki/Percent-encoding)
                            String decodedMessage = URLDecoder.decode(message, StandardCharsets.UTF_8.name());
                            // escape HTML entities (to prevent <script></script> and other tags)
                            String escapedMessage = StringEscapeUtils.escapeHtml4(decodedMessage);
                            msgMgr.addMessage(System.currentTimeMillis(), peerId, escapedMessage);
                            String body = JsonUtil.toJson(new Response(Status.OK, null, null, null));
                            response = generateResponse(STATUS_OK, MEDIA_APPLICATION_JSON, new Date(), body);
                        }
                        // send not found for any other requests
                    } else {
                        String body = "The requested resource not found " + queryString;
                        response = generateResponse(STATUS_NOT_FOUND, MEDIA_TEXT_PLAIN, new Date(), body);
                    }
                    log.error(queryString);
                }

                out.println(response);
            } catch (IOException e) {
                log.error("Failed to process request", e);
            } finally {
                try {
                    socket.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }

        /**
         * Generate HTTP message response
         *
         * @param status
         *            HTTP status
         * @param contentType
         *            HTTP content type
         * @param date
         *            Last modification date
         * @param responseBody
         *            response body
         *
         * @return HTTP message
         */
        private String generateResponse(String status, String contentType, Date date, String responseBody) {
            return """
                    HTTP/1.1 %s
                    Content-Type: %s
                    Last-Modified: %s

                    %s
                    """.formatted(status, contentType, new SimpleDateFormat(LAST_MODIFIED_DATE_FORMAT).format(date),
                    responseBody);
        }
    }
}
