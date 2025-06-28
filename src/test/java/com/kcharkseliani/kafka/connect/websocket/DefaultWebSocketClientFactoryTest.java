// SPDX-License-Identifier: MIT
// Copyright (c) 2025 Konstantin Charkseliani

package com.kcharkseliani.kafka.connect.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.java_websocket.client.WebSocketClient;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link DefaultWebSocketClientFactory}, verifying WebSocket client creation behavior.
 */
class DefaultWebSocketClientFactoryTest {

    /** Instance of the client factory under test. */
    private DefaultWebSocketClientFactory clientFactory;

    /**
     * Initializes the {@code DefaultWebSocketClientFactory} instance before each test.
     */
    @BeforeEach
    void setUp() {
        clientFactory = new DefaultWebSocketClientFactory();
    }

    /**
     * Tests that {@link DefaultWebSocketClientFactory#createClient(URI, String, MessageHandler)}
     * returns a {@link WebSocketClient} initialized with the provided URI.
     *
     * @throws Exception if URI creation or WebSocket client setup fails
     */
    @Test
    void testCreateClient_WithValidParameters_ShouldReturnWebSocketClientWithUri() throws Exception {
        // Arrange
        URI testUri = new URI("ws://localhost:8080");
        String subscriptionMessage = "test_subscription_message";
        String pingMessage = "{\"message\":\"ping\"}";
        int pingIntervalMs = 20000;
        String pongPattern = "\"message\"\\s*:\\s*\"pong\"";
        MessageHandler messageHandler = message -> {
            // Handle the message
        };

        // Act
        WebSocketClient client = clientFactory.createClient(
            testUri, 
            subscriptionMessage, 
            pingMessage,
            pingIntervalMs,
            pongPattern,
            messageHandler);

        // Assert
        assertNotNull(client, "Expected WebSocketClient to be created, but it is null instead.");
        assertEquals(testUri, client.getURI(), "WebSocketClient URI should match the provided URI.");
    }   
}

