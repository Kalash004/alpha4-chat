package core;

import core.model.Request;
import core.model.Response;
import core.model.Status;
import core.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DiscoveryServer extends Thread implements Config {
    private static final Logger log = LoggerFactory.getLogger(DiscoveryServer.class);

    private final int port;
    private final String peerId;

    public DiscoveryServer(int port, String peerId) {
        this.port = port;
        this.peerId = peerId;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            while (true) {
                byte[] buf = new byte[DEFAULT_PACKET_BUFFER];
                DatagramPacket request = new DatagramPacket(buf, buf.length);
                socket.receive(request);
                InetAddress address = request.getAddress();
                String addressString = address.getHostAddress();
                int port = request.getPort();

                String requestMsg = new String(request.getData());
                log.info("Received broadcast request {} from {}:{}", requestMsg, addressString, port);

                Request requestObj = JsonUtil.fromJson(requestMsg, Request.class);
                Response response;
                switch (requestObj.command()) {
                case HELLO:
                    String requestPeerId = requestObj.peerId();
                    if (requestPeerId != null) {
                        response = new Response(Status.OK, null, null, peerId);
                    } else {
                        response = new Response(Status.ERROR, null,
                                String.format("Missing peer_id: %s", escapeQuotes(requestMsg)), peerId);
                    }
                    break;
                case UNKNOWN:
                    response = new Response(Status.ERROR, null,
                            String.format("Invalid message: %s", escapeQuotes(requestMsg)), peerId);
                    break;
                default:
                    response = new Response(Status.ERROR, null,
                            String.format("Invalid command: %s", requestObj.command().value()), peerId);
                }
                String responseString = JsonUtil.toJson(response);
                log.debug("Sending response {} to {}:{}", responseString, addressString, port);
                byte[] outBuf = responseString.getBytes();
                socket.send(new DatagramPacket(outBuf, outBuf.length, address, port));
            }
        } catch (IOException e) {
            log.error("Unable to receive messages on port", port, e);
        }
    }
}
