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

class WebSocketSourceConnectorTest {

    private WebSocketSourceConnector connector;
    private final String websocketUrl = "ws://example.com/socket";
    private final String kafkaTopic = "test-topic";
    private final String subscriptionMessage = "{\"type\":\"subscribe\"}";
    private Properties properties;

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

    @Test
    void testConfigDoesNotThrowException() {
        // Act & Assert: Ensure config() does not throw any exceptions when called
        assertDoesNotThrow(connector::config);
    }

    @Test
    void testVersion_ShouldReturnCorrectVersion() {
        // Arrange
        Map<String, String> props = new HashMap<>();
        props.put("websocket.url", websocketUrl);
        props.put("topic", kafkaTopic);
        
        connector.start(props);

        // Expected version from the properties file or "unknown-version" if not set
        String expectedVersion = properties.getProperty("app.version", "unknown-version");

        // Act
        String actualVersion = connector.version();

        // Assert
        assertEquals(expectedVersion, actualVersion, "The version method should return the correct app version.");
    }
}

