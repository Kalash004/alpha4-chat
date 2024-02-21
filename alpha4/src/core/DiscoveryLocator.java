package core;


import core.model.Command;
import core.model.Request;
import core.model.Response;
import core.manager.PeerManager;
import core.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class DiscoveryLocator extends Thread implements Config {
    private static final Logger log = LoggerFactory.getLogger(DiscoveryLocator.class);

    private InetAddress ipAddress;
    private int port;
    private int timeout;

    private String peerId;
    private PeerManager peerMgr;

    public DiscoveryLocator(String ipAddress, int port, String peerId, PeerManager peerMgr) {
        try {
            this.ipAddress = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            String msg = String.format("Invalid ip address %s", ipAddress);
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
        this.port = port;
        this.timeout = Config.BROADCAST_TIMEOUT_MS;
        this.peerId = peerId;
        this.peerMgr = peerMgr;
    }

    @Override
    public void run() {
        String cmd = JsonUtil.toJson(new Request(Command.HELLO, peerId, null, null));

        // open socket
        try (DatagramSocket socket = new DatagramSocket()) {
            // set broadcast
            socket.setBroadcast(true);
            socket.setSoTimeout(timeout);
            while (true) {
                log.debug("Sending broadcard request {} to {}:{}", cmd, ipAddress.getHostAddress(), port);
                // send request
                byte[] outBuf = cmd.getBytes();
                DatagramPacket request = new DatagramPacket(outBuf, outBuf.length, ipAddress, port);
                socket.send(request);
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < Config.BROADCAST_TIMEOUT_MS) {
                    try {
                        log.debug("Waiting for the messages");
                        byte[] inBuf = new byte[Config.DEFAULT_PACKET_BUFFER];
                        DatagramPacket response = new DatagramPacket(inBuf, inBuf.length);
                        socket.receive(response);

                        // peer IP address
                        String peerAddress = response.getAddress().getHostAddress();
                        int peerPort = response.getPort();
                        String responseString = new String(response.getData()).trim();
                        log.debug("Received message {} from {}:{}", responseString, peerAddress, peerPort);

                        Response responseObj = JsonUtil.fromJson(responseString, Response.class);
                        String peerId = responseObj.peerId();
                        if (peerId == null) {
                            String errorMsg = responseObj.message();
                            if (errorMsg != null) {
                                log.warn("Received error: {} from {}:{}", errorMsg, peerAddress, peerPort);
                            } else {
                                log.warn("Unable to get peer id from: {} from {}:{}", responseString, peerAddress,
                                        peerPort);
                            }
                            // ignore ourself
                        } else if (!this.peerId.equals(peerId)) {
                            peerMgr.addPeer(peerId, peerAddress, peerPort);
                        } else {
                            log.debug("Ignoring out peer %s from {}:{}", peerId, peerAddress, peerPort);
                        }
                    } catch (SocketTimeoutException e) {
                        // do nothing
                        log.warn("Got socket timeout exception, no responses received during timeout {}ms", timeout);
                    } catch (Exception e) {
                        log.debug("Got exception while waiting for the response", e);
                    }
                }
            }
        } catch (IOException e) {
            String msg = String.format("Unable to send command \"%s\" to the peer %s:%s", cmd,
                    ipAddress.getHostAddress(), port);
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }
}
