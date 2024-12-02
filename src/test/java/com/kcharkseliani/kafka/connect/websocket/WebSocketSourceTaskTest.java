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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class WebSocketSourceTaskTest {

    private WebSocketSourceTask task;
    
    @Mock
    private WebSocketClientFactory clientFactory;
    
    @Mock
    private WebSocketClient mockClient;
    
    private final String kafkaTopic = "test-topic";
    private final String websocketUrl = "ws://example.com";
    private final String subscriptionMessage = "{\"action\": \"subscribe\", \"channel\": \"test-stream\"}";

    private Properties properties;

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

    @Test
    public void testStart_SendsSubscriptionMessageOnOpen() throws Exception {
        // Prepare props with topic, websocket URL, and subscription message
        Map<String, String> props = Map.of(
            "topic", kafkaTopic,
            "websocket.url", websocketUrl,
            "websocket.subscription.message", subscriptionMessage
        );

        // Set up the factory to return the mock client
        when(clientFactory.createClient(any(URI.class), any(String.class), any(MessageHandler.class)))
            .thenReturn(mockClient);
        
        // Call start with the prepared props
        task.start(props);

        // Assert
        // Verify that the clientFactory.createClient method was called with the expected arguments
        verify(clientFactory).createClient(eq(URI.create(websocketUrl)), eq(subscriptionMessage), any(MessageHandler.class));
        
        // Verify that client.connect() was called
        verify(mockClient).connect();
    }

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
        when(clientFactory.createClient(any(URI.class), any(String.class), messageHandlerCaptor.capture()))
            .thenReturn(mockClient);

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
        assertEquals(1, records.size());
        SourceRecord record = records.get(0);
        assertEquals(kafkaTopic, record.topic());
        assertEquals(incomingMessage, record.value());
    }

    @Test
    public void testStop_ClosesWebSocketClient() {
        // Set up props and initialize task
        Map<String, String> props = Map.of(
            "topic", kafkaTopic,
            "websocket.url", websocketUrl,
            "websocket.subscription.message", subscriptionMessage
        );
        // Prepare the task
        when(clientFactory.createClient(any(URI.class), any(String.class), any(MessageHandler.class)))
            .thenReturn(mockClient);

        task.start(props);

        // Call stop
        task.stop();

        // Verify that client.close() was called
        verify(mockClient).close();
    }

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
        when(clientFactory.createClient(any(URI.class), any(String.class), any(MessageHandler.class)))
            .thenReturn(mockClient);

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

