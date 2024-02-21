package core;

import core.manager.MessagesManager;
import core.model.Request;
import core.model.Response;
import core.model.Status;
import core.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MessageServer extends Thread implements Config {
    private static final Logger log = LoggerFactory.getLogger(MessageServer.class);
    private int port;
    private MessagesManager msgMgr;

    public MessageServer(int port, MessagesManager msgMgr) {
        this.port = port;
        this.msgMgr = msgMgr;
    }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            server.setReuseAddress(true);
            while (true) {
                try (Socket socket = server.accept(); PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
                    String address = socket.getInetAddress().getHostAddress();
                    String requestMsg = readInput(in);
                    log.info("Received message {} from {}:{}", requestMsg, address, port);
                    Request request = JsonUtil.fromJson(requestMsg, Request.class);

                    Response response;
                    switch (request.command()) {
                        case HELLO:
                            String requestPeerId = request.peerId();
                            if (requestPeerId != null) {
                                response = new Response(Status.OK, msgMgr.getMessages(), null, null);
                            } else {
                                response = new Response(Status.ERROR, null,
                                        String.format("Missing peer_id: %s", escapeQuotes(requestMsg)), null);
                            }
                            break;
                        case NEW_MESSAGE:
                            msgMgr.addMessage(request.messageId(), request.peerId(), request.message());
                            response = new Response(Status.OK, null, null, null);
                        case UNKNOWN:
                            response = new Response(Status.ERROR, null,
                                    String.format("Invalid message: %s", escapeQuotes(requestMsg)), null);
                            break;
                        default:
                            response = new Response(Status.ERROR, null,
                                    String.format("Invalid command: %s", request.command().value()), null);
                    }
                    String responseJson = JsonUtil.toJson(response);
                    log.debug("Returning response {} to {}", responseJson, address);
                    out.println(responseJson);
                    out.println();
                    out.println();
                } catch (IOException e) {
                    log.error(e.getLocalizedMessage(), e);
                }
            }
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }
}
