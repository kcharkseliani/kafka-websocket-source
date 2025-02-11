package com.kcharkseliani.kafka.connect.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.java_websocket.client.WebSocketClient;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultWebSocketClientFactoryTest {

    private DefaultWebSocketClientFactory clientFactory;

    @BeforeEach
    void setUp() {
        clientFactory = new DefaultWebSocketClientFactory();
    }

    @Test
    void testCreateClient_WithValidParameters_ShouldReturnWebSocketClientWithUri() throws Exception {
        // Arrange
        URI testUri = new URI("ws://localhost:8080");
        String subscriptionMessage = "test_subscription_message";
        MessageHandler messageHandler = message -> {
            // Handle the message
        };

        // Act
        WebSocketClient client = clientFactory.createClient(testUri, subscriptionMessage, messageHandler);

        // Assert
        assertNotNull(client);
        // Use reflection to access the private 'uri' field of the WebSocketClient
        assertEquals(testUri, client.getURI(), "WebSocketClient URI should match the provided URI");
    }   
}

