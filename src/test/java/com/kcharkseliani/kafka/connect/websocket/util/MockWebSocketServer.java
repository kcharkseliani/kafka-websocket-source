package com.kcharkseliani.kafka.connect.websocket.util;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A mock WebSocket server for testing WebSocket client and connector behavior.
 * This server captures all messages received from clients
 * and stores them for inspection during integration tests.
 */
public class MockWebSocketServer extends WebSocketServer {

    /** Thread-safe list to store messages received from WebSocket clients. */
    private final List<String> receivedMessages = new CopyOnWriteArrayList<>();

    /**
     * Constructs a MockWebSocketServer bound to the given address.
     *
     * @param address the address (host and port) the server should bind to
     */
    public MockWebSocketServer(InetSocketAddress address) {
        super(address);
    }

    /**
     * Callback when a new WebSocket connection is established.
     *
     * @param conn the WebSocket connection
     * @param handshake the handshake data from the client
     */
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("WebSocket connection opened: " + conn.getRemoteSocketAddress());
    }

    /**
     * Callback when a WebSocket connection is closed.
     *
     * @param conn the WebSocket connection
     * @param code the status code representing the close reason
     * @param reason the human-readable reason for closing
     * @param remote whether the closing was initiated remotely
     */
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("WebSocket connection closed: " + reason);
    }

    /**
     * Callback when a message is received from a WebSocket client.
     *
     * @param conn the WebSocket connection
     * @param message the message received
     */
    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Received message: " + message);
        receivedMessages.add(message);
    }

    /**
     * Callback when an error occurs on a WebSocket connection.
     *
     * @param conn the WebSocket connection (can be null if the error is server-wide)
     * @param ex the exception that was thrown
     */
    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    /**
     * Callback when the WebSocket server has successfully started.
     */
    @Override
    public void onStart() {
        System.out.println("WebSocket server started successfully!");
    }

    /**
     * Returns a list of all messages received by the server so far.
     *
     * @return the list of received WebSocket messages
     */
    public List<String> getReceivedMessages() {
        return receivedMessages;
    }
}