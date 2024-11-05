package com.kcharkseliani.kafka.connect.websocket;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebSocketSourceConnector extends SourceConnector {

    private Map<String, String> configProperties;

    @Override
    public String version() {
        return "0.0";
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

    @Override
    public ConfigDef config() {
        return new ConfigDef()
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
            )
            .define(
                "tasks.max", 
                ConfigDef.Type.INT, 
                1, 
                ConfigDef.Range.between(1, 1), 
                ConfigDef.Importance.HIGH, 
                "Maximum number of tasks (this connector version only supports 1 task)");
    }
}
