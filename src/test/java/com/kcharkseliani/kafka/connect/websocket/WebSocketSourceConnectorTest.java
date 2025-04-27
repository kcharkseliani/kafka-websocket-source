package com.kcharkseliani.kafka.connect.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link WebSocketSourceConnector},
 * validating connector configuration handling, task creation, and version retrieval logic.
 */
class WebSocketSourceConnectorTest {

    /** Instance of the source connector under test. */
    private WebSocketSourceConnector connector;
    
    /** Example webSocket URL used for connecting during tests. */
    private final String websocketUrl = "ws://example.com/socket";

    /** Kafka topic name that would be used for publishing WebSocket messages. */
    private final String kafkaTopic = "test-topic";

    /** Subscription message that would be sent when connecting to the WebSocket server. */
    private final String subscriptionMessage = "{\"type\":\"subscribe\"}";

    /** Properties loaded from {@code config.properties} for version verification. */
    private Properties properties;

    /**
     * Initializes a new instance of {@link WebSocketSourceConnector} and loads application properties.
     */
    @BeforeEach
    void setUp() {
        connector = new WebSocketSourceConnector();

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
     * Verifies that {@link WebSocketSourceConnector#start(Map)} correctly sets internal configuration
     * when provided with valid properties.
     */
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

    /**
     * Verifies that {@link WebSocketSourceConnector#start(Map)} throws an exception
     * when the required 'websocket.url' property is missing.
     */
    @Test
    void testStart_MissingWebSocketUrl_ShouldThrowException() {
        // Arrange
        Map<String, String> props = new HashMap<>();
        props.put("topic", kafkaTopic);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> connector.start(props));
        assertEquals("Missing required configuration: websocket.url", exception.getMessage());
    }

    /**
     * Verifies that {@link WebSocketSourceConnector#start(Map)} throws an exception
     * when the required 'topic' property is missing.
     */
    @Test
    void testStart_MissingTopic_ShouldThrowException() {
        // Arrange
        Map<String, String> props = new HashMap<>();
        props.put("websocket.url", websocketUrl);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> connector.start(props));
        assertEquals("Missing required configuration: topic", exception.getMessage());
    }

    /**
     * Verifies that {@link WebSocketSourceConnector#taskConfigs(int)} correctly generates
     * a single task configuration when one task is requested.
     */
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

    /**
     * Verifies that {@link WebSocketSourceConnector#config()} does not throw any exceptions.
     */
    @Test
    void testConfigDoesNotThrowException() {
        // Act & Assert: Ensure config() does not throw any exceptions when called
        assertDoesNotThrow(connector::config);
    }

    /**
     * Verifies that {@link WebSocketSourceConnector#version()} returns the expected version
     * from the {@code config.properties} file or "unknown-version" as fallback.
     */
    @Test
    void testVersion_ShouldReturnCorrectVersion() {
        // Arrange
        Map<String, String> props = new HashMap<>();
        props.put("websocket.url", websocketUrl);
        props.put("topic", kafkaTopic);

        // Expected version from the properties file or "unknown-version" if not set
        String expectedVersion = properties.getProperty("app.version", "unknown-version");

        // Act
        String actualVersion = connector.version();

        // Assert
        assertEquals(expectedVersion, actualVersion, "The version method should return the correct app version.");
    }
}

