// SPDX-License-Identifier: MIT
// Copyright (c) 2025 Konstantin Charkseliani

package com.kcharkseliani.kafka.connect.websocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

/**
 * Implementation of {@link WebSocketClientFactory} using the Java-WebSocket library.
 * Used by the connector by default.
 * 
 * This creates a basic WebSocket client that can optionally send a subscription message on connect
 * and delegates message handling to the provided {@link MessageHandler}.
 */
public class DefaultWebSocketClientFactory implements WebSocketClientFactory {

    /**
     * Creates a {@link WebSocketClient} instance that connects to the specified URI and handles messages using the provided handler.
     *
     * @param uri the WebSocket server URI to connect to
     * @param subscriptionMessage optional message to send immediately after connection (e.g., subscription payload)
     * @param messageHandler callback to process incoming WebSocket messages
     * @return a configured {@link WebSocketClient} instance
     */
    @Override
    public WebSocketClient createClient(URI uri, String subscriptionMessage, MessageHandler messageHandler) {
        return new WebSocketClient(uri) {

            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("Connected to WebSocket");
                if (subscriptionMessage != null && !subscriptionMessage.isEmpty()) {
                    send(subscriptionMessage); // Send the subscription message
                    System.out.println("Sent the initial subscription message to the websocket");
                }
            }
    
            @Override
            public void onMessage(String message) {
                messageHandler.handle(message); // Use the lambda to handle the message
            }
    
            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("WebSocket closed: " + reason);
            }
    
            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
            }
        };
    }
}

