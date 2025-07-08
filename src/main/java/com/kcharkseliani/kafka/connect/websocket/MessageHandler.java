// SPDX-License-Identifier: MIT
// Copyright (c) 2025 Konstantin Charkseliani

package com.kcharkseliani.kafka.connect.websocket;

import org.java_websocket.client.WebSocketClient;

/**
 * Functional interface for handling messages received from a WebSocket connection.
 * Used in the {@link WebSocketClient#onMessage(String)} method to process incoming WebSocket messages.
 * Passed to {@link WebSocketClientFactory#createClient(java.net.URI, String, MessageHandler)} factory method.
 */
@FunctionalInterface
public interface MessageHandler {
    /**
     * Handles an incoming message from the WebSocket. Passed as a callback.
     *
     * @param message the raw message payload received from the WebSocket
     */
    void handle(String message);
}