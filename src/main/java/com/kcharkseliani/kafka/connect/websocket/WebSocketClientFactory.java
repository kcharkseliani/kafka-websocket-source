package com.kcharkseliani.kafka.connect.websocket;

import org.java_websocket.client.WebSocketClient;

import java.net.URI;

/**
 * Factory interface for creating {@link WebSocketClient} instances.
 * 
 * This allows injecting custom WebSocket client implementations, especially for testing purposes.
 */
public interface WebSocketClientFactory {
    /**
     * Creates a {@link WebSocketClient} instance that connects to the specified URI and handles messages using the provided handler.
     *
     * @param uri the WebSocket server URI to connect to
     * @param subscriptionMessage optional message to send immediately after connection (e.g., subscription payload)
     * @param messageHandler callback to process incoming WebSocket messages
     * @return a configured {@link WebSocketClient} instance
     */
    WebSocketClient createClient(URI uri, String subscriptionMessage, MessageHandler messageHandler);
}
