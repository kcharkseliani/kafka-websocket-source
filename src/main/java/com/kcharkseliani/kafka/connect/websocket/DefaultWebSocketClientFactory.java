// SPDX-License-Identifier: MIT
// Copyright (c) 2025 Konstantin Charkseliani

package com.kcharkseliani.kafka.connect.websocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

/**
 * Implementation of {@link WebSocketClientFactory} using the Java-WebSocket library.
 * Used by the connector by default.
 * 
 * This creates a basic WebSocket client that can optionally send a subscription message on connect
 * and delegates message handling to the provided {@link MessageHandler}.
 */
public class DefaultWebSocketClientFactory implements WebSocketClientFactory {

    /**
     * Creates a {@link WebSocketClient} instance that connects to the specified URI,
     * optionally sends a subscription message upon connection, periodically sends ping messages,
     * filters out pong responses based on a pattern and delegates message handling to the provided {@link MessageHandler}.
     *
     * @param uri the WebSocket server URI to connect to
     * @param subscriptionMessage optional message to send immediately after connection (e.g., subscription payload)
     * @param pingMessage optional application-level ping message to send periodically (e.g., {"method":"ping"}); empty string disables ping
     * @param pingIntervalMs interval in milliseconds between sending ping messages; ignored if pingMessage is empty
     * @param pongPattern optional regular expression pattern used to detect pong responses; matching messages are not forwarded to the handler
     * @param messageHandler callback to process incoming WebSocket messages (when they don't match pong pattern if any)
     * @return a configured {@link WebSocketClient} instance
     */
    @Override
    public WebSocketClient createClient(
        URI uri,
        String subscriptionMessage,
        String pingMessage,
        int pingIntervalMs,
        String pongPattern,
        MessageHandler messageHandler
    ) {
        return new WebSocketClient(uri) {

            /** Timer used to schedule periodic sending of application-level ping messages */
            private Timer pingTimer;

            /** Compiled regular expression used to identify application-level pong messages */
            private final Pattern pongRegex = (pongPattern != null && !pongPattern.isEmpty())
                ? Pattern.compile(pongPattern)
                : null;

            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("Connected to WebSocket");

                if (subscriptionMessage != null && !subscriptionMessage.isEmpty()) {
                    send(subscriptionMessage); // Send the subscription message
                    System.out.println("Sent the initial subscription message to the websocket");
                }

                // If ping message was provided, schedule a background timer task to
                // periodically send the application-level ping message at fixed intervals
                // to keep the WebSocket connection alive
                if (pingMessage != null && !pingMessage.isEmpty() && pingIntervalMs > 0) {
                    pingTimer = new Timer(true);
                    pingTimer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            // Only send the ping if the connection is still open
                            if (isOpen()) {
                                send(pingMessage);
                                System.out.println("Sent ping message: " + pingMessage);
                            }
                        }
                    }, 0, pingIntervalMs);
                }
            }
    
            @Override
            public void onMessage(String message) {

                // Skip pong messages if they match the configured pattern
                if (pingMessage != null && 
                    !pingMessage.isEmpty() && 
                    pongRegex != null && 
                    pongRegex.matcher(message).find()
                    ) {
                    System.out.println("Received pong message: " + message);
                    return;
                }

                // Use the provided lambda to handle the incoming message
                messageHandler.handle(message);
            }
    
            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("WebSocket closed: " + reason);
                if (pingTimer != null) pingTimer.cancel();
            }
    
            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
            }
        };
    }
}

