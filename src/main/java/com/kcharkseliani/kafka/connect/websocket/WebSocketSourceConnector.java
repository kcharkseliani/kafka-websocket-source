package com.kcharkseliani.kafka.connect.websocket;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.InputStream;
import java.util.Properties;

public class WebSocketSourceConnector extends SourceConnector {

    private Map<String, String> configProperties;
    private static final Properties properties = new Properties();
    
    static {
        // Load config.properties at class initialization
        try (InputStream input = WebSocketSourceConnector.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            } else {
                System.err.println("config.properties file not found in resources.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String version() {
        return properties.getProperty("app.version", "unknown-version");
    }

    @Override
    public void start(Map<String, String> props) {
        // Retrieve essential configurations
        String websocketUrl = props.get("websocket.url");
        String topic = props.get("topic");
        
        // Validate required configurations
        if (websocketUrl == null || websocketUrl.isEmpty()) {
            throw new IllegalArgumentException("Missing required configuration: websocket.url");
        }
        if (topic == null || topic.isEmpty()) {
            throw new IllegalArgumentException("Missing required configuration: topic");
        }

        // Save the connector's configuration properties
        this.configProperties = props;
    }

    @Override
    public Class<? extends Task> taskClass() {
        return WebSocketSourceTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        // Create a single task configuration that reuses the connector's properties
        List<Map<String, String>> configs = new ArrayList<>();
        
        // Pass the connector's configuration as is
        configs.add(new HashMap<>(this.configProperties));
        
        return configs;
    }

    @Override
    public void stop() {
        // Clean up resources if needed
    }

    private static final ConfigDef CONFIG_DEF = new ConfigDef()
        .define(
            "websocket.url", 
            ConfigDef.Type.STRING, 
            ConfigDef.Importance.HIGH, 
            "The WebSocket URL to connect to."
        )
        .define(
            "topic", 
            ConfigDef.Type.STRING, 
            ConfigDef.Importance.HIGH, 
            "The Kafka topic where WebSocket messages will be published."
        )
        .define(
            "websocket.subscription.message", 
            ConfigDef.Type.STRING, 
            "", // Default to an empty string if not provided
            ConfigDef.Importance.LOW, 
            "Optional subscription message to send after connecting to the WebSocket."
        );

    public ConfigDef config() {
        return CONFIG_DEF;
    }
}
