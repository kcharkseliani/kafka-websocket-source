// SPDX-License-Identifier: MIT
// Copyright (c) 2025 Konstantin Charkseliani

package com.kcharkseliani.kafka.connect.websocket;

import org.apache.kafka.connect.source.SourceRecord;
import org.java_websocket.client.WebSocketClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the {@link WebSocketSourceTask} class,
 * verifying WebSocket behavior and correct Kafka record production.
 */
public class WebSocketSourceTaskTest {

    /** Instance of the source task under test. */
    private WebSocketSourceTask task;
    
    /** Mock factory used to create WebSocket clients. */
    @Mock
    private WebSocketClientFactory clientFactory;
    
    /** Mock WebSocket client used in tests. */
    @Mock
    private WebSocketClient mockClient;
    
    /** Kafka topic that would be used for produced records. */
    private final String kafkaTopic = "test-topic";

    /** Mock WebSocket server URL. */
    private final String websocketUrl = "ws://example.com";

    /** Subscription message to send after WebSocket connection. */
    private final String subscriptionMessage = "{\"action\": \"subscribe\", \"channel\": \"test-stream\"}";

     /** Properties loaded from config.properties, mainly for version testing. */
    private Properties properties;

     /**
     * Initializes mocks and loads properties before each test.
     */
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        task = new WebSocketSourceTask();
        task.setWebSocketClientFactory(clientFactory);

        // Load properties to verify the version from config.properties
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Verifies that when the task starts, it sends a subscription message
     * and connects using the created WebSocket client.
     *
     * @throws Exception if WebSocket connection setup fails
     */
    @Test
    public void testStart_SendsSubscriptionMessageOnOpen() throws Exception {
        // Prepare props with topic, websocket URL, and subscription message
        Map<String, String> props = Map.of(
            "topic", kafkaTopic,
            "websocket.url", websocketUrl,
            "websocket.subscription.message", subscriptionMessage
        );

        // Set up the factory to return the mock client
        doReturn(mockClient)
            .when(clientFactory).createClient(any(URI.class), any(String.class), any(MessageHandler.class));
        
        // Call start with the prepared props
        task.start(props);

        // Assert
        // Verify that the clientFactory.createClient method was called with the expected arguments
        verify(clientFactory).createClient(eq(URI.create(websocketUrl)), eq(subscriptionMessage), any(MessageHandler.class));
        
        // Verify that client.connect() was called
        verify(mockClient).connect();
    }

    /**
     * Verifies that when a WebSocket message is received, the task correctly
     * converts it into a {@link SourceRecord} queued for Kafka.
     *
     * @throws Exception if starting the task or handling messages fails
     */
    @Test
    public void testOnMessage_AddsMessageToQueueAsSourceRecord() throws Exception {
        // Set up props and initialize task
        Map<String, String> props = Map.of(
            "topic", kafkaTopic,
            "websocket.url", websocketUrl,
            "websocket.subscription.message", subscriptionMessage
        );

        // Capture the MessageHandler when createClient is called
        ArgumentCaptor<MessageHandler> messageHandlerCaptor = ArgumentCaptor.forClass(MessageHandler.class);
        
        // Set up the factory to return a mock client and capture the handler
        doReturn(mockClient)
            .when(clientFactory).createClient(any(URI.class), any(String.class), messageHandlerCaptor.capture());

        // Start the task
        task.start(props);

        // Retrieve the captured MessageHandler instance
        MessageHandler capturedMessageHandler = messageHandlerCaptor.getValue();

        // Simulate receiving a message from WebSocket by invoking the handler directly
        String incomingMessage = "{\"event\": \"test-data\"}";
        capturedMessageHandler.handle(incomingMessage);

        // Poll from the task to retrieve SourceRecord
        List<SourceRecord> records = task.poll();

        // Verify that the record was correctly added to the queue
        assertEquals(1, records.size(), 
            "Expected one SourceRecord to be returned from the poll after handling a message, but got " + records.size());
        SourceRecord record = records.get(0);
        assertEquals(kafkaTopic, record.topic(), "The SourceRecord topic should match the configured Kafka topic.");
        assertEquals(incomingMessage, record.value(), "The SourceRecord value should match the WebSocket message.");
    }

    /**
     * Verifies that calling {@link WebSocketSourceTask#stop()} closes
     * the underlying WebSocket connection.
     */
    @Test
    public void testStop_ClosesWebSocketClient() {
        // Set up props and initialize task
        Map<String, String> props = Map.of(
            "topic", kafkaTopic,
            "websocket.url", websocketUrl,
            "websocket.subscription.message", subscriptionMessage
        );
        // Prepare the task
        doReturn(mockClient)
            .when(clientFactory).createClient(any(URI.class), any(String.class), any(MessageHandler.class));

        task.start(props);

        // Call stop
        task.stop();

        // Verify that client.close() was called
        verify(mockClient).close();
    }

    /**
     * Verifies that the {@link WebSocketSourceTask#version()} method
     * returns the correct version string from the filtered config.properties file.
     */
    @Test
    public void testVersion_ShouldReturnCorrectVersion() {
        // Arrange      
        // Set up props and initialize task
        Map<String, String> props = Map.of(
            "topic", kafkaTopic,
            "websocket.url", websocketUrl,
            "websocket.subscription.message", subscriptionMessage
        );
        // Prepare the task
        doReturn(mockClient)
            .when(clientFactory).createClient(any(URI.class), any(String.class), any(MessageHandler.class));

        // Start the task to initialize properties from config.properties
        task.start(props);

        // Expected version from the filtered properties file
        String expectedVersion = properties.getProperty("app.version", "unknown-version");

        // Act
        String actualVersion = task.version();

        // Assert that the version from task matches expected after calling start
        assertEquals(expectedVersion, actualVersion, "The version method should return the version specified in config.properties after start.");
    }
}

