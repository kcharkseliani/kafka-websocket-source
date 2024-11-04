package com.kcharkseliani.kafka.connect.websocket;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebSocketSourceConnector extends SourceConnector {

    @Override
    public String version() {
        return "0.0";
    }

    @Override
    public void start(Map<String, String> props) {
        // Initialize resources or configurations here if needed
    }

    @Override
    public Class<? extends Task> taskClass() {
        return WebSocketSourceTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        Map<String, String> config = new HashMap<>();
        // Configure task properties here
        return List.of(config);
    }

    @Override
    public void stop() {
        // Clean up resources if needed
    }

    @Override
    public ConfigDef config() {
        return new ConfigDef()
            // Define configuration options here if needed
            ;
    }
}
