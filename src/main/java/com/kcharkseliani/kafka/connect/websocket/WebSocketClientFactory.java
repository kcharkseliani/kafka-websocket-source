// SPDX-License-Identifier: MIT
// Copyright (c) 2025 Konstantin Charkseliani

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
    WebSocketClient createClient(
        URI uri,
        String subscriptionMessage,
        String pingMessage,
        int pingIntervalMs,
        String pongPattern,
        MessageHandler messageHandler
    );
}
