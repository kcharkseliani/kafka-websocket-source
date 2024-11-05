package com.kcharkseliani.kafka.connect.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketSourceConnectorTest {

    private WebSocketSourceConnector connector;
    private final String websocketUrl = "ws://example.com/socket";
    private final String kafkaTopic = "test-topic";
    private final String subscriptionMessage = "{\"type\":\"subscribe\"}";

    @BeforeEach
    void setUp() {
        connector = new WebSocketSourceConnector();
    }

    @Test
    void testStart_WithValidProperties_ShouldSetConfig() {
        // Arrange
        Map<String, String> props = new HashMap<>();
        props.put("websocket.url", websocketUrl);
        props.put("topic", kafkaTopic);
        props.put("websocket.subscription.message", subscriptionMessage);

        // Act
        connector.start(props);

        // Assert
        // Use reflection to check value of private configProperties
        try {
            Field configPropertiesField = WebSocketSourceConnector.class.getDeclaredField("configProperties");
            configPropertiesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Map<String, String> configProperties = (Map<String, String>) configPropertiesField.get(connector);
            
            assertEquals(props, configProperties, "The configProperties field should match the passed props.");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Reflection access to configProperties field failed: " + e.getMessage());
        }
    }

    @Test
    void testStart_MissingWebSocketUrl_ShouldThrowException() {
        // Arrange
        Map<String, String> props = new HashMap<>();
        props.put("topic", kafkaTopic);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> connector.start(props));
        assertEquals("Missing required configuration: websocket.url", exception.getMessage());
    }

    @Test
    void testStart_MissingTopic_ShouldThrowException() {
        // Arrange
        Map<String, String> props = new HashMap<>();
        props.put("websocket.url", websocketUrl);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> connector.start(props));
        assertEquals("Missing required configuration: topic", exception.getMessage());
    }

    @Test
    void testTaskConfigs_SingleTaskConfiguration() {
        // Arrange
        Map<String, String> props = new HashMap<>();
        props.put("websocket.url", websocketUrl);
        props.put("topic", kafkaTopic);
        props.put("websocket.subscription.message", subscriptionMessage);
        
        connector.start(props);
        
        // Act
        List<Map<String, String>> taskConfigs = connector.taskConfigs(1);

        // Assert
        assertEquals(1, taskConfigs.size());
        Map<String, String> config = taskConfigs.get(0);
        assertEquals(websocketUrl, config.get("websocket.url"));
        assertEquals(kafkaTopic, config.get("topic"));
        assertEquals(subscriptionMessage, config.get("websocket.subscription.message"));
    }
}

