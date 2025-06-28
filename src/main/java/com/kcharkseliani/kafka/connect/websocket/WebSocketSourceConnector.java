// SPDX-License-Identifier: MIT
// Copyright (c) 2025 Konstantin Charkseliani

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

/**
 * A Kafka Connect {@link SourceConnector} implementation that streams messages from a WebSocket server into a Kafka topic.
 * 
 * This connector supports:
 * <ul>
 *   <li>An optional subscription message sent immediately after establishing the WebSocket connection</li>
 *   <li>Periodic sending of configurable application-level ping messages to keep the connection alive</li>
 *   <li>Filtering out of application-level pong messages using a configurable regex pattern</li>
 * </ul>
 * 
 * Incoming WebSocket messages (excluding matched pong responses) are forwarded to Kafka as
 * {@link org.apache.kafka.connect.source.SourceRecord} entries.
 */
public class WebSocketSourceConnector extends SourceConnector {

    /** Configuration properties provided to the connector. */
    private Map<String, String> configProperties;

     /** Properties loaded from the internal config.properties file (e.g., for version information). */
    private static final Properties properties = new Properties();

    /** Defines the configuration options supported by this connector. */
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
            "",
            ConfigDef.Importance.LOW, 
            "Optional subscription message to send after connecting to the WebSocket."
        )
        .define(
            "websocket.ping.message",
            ConfigDef.Type.STRING,
            "",
            ConfigDef.Importance.LOW,
            "Optional ping message to send periodically to keep the WebSocket connection alive."
        )
        .define(
            "websocket.ping.interval.ms",
            ConfigDef.Type.INT,
            20000,
            ConfigDef.Importance.LOW,
            "Interval in milliseconds between each ping message."
        )
        .define(
            "websocket.pong.pattern",
            ConfigDef.Type.STRING,
            "",
            ConfigDef.Importance.LOW,
            "Regex pattern to detect pong responses in incoming WebSocket messages. " +
            "If not provided, pong responses will be sent alongside other messages."
        );
    
    // Static initializer to load the config.properties file at class loading time
    static {
        // Load config.properties at class initialization
        try (InputStream input = WebSocketSourceConnector.class
            .getClassLoader()
            .getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            } else {
                System.err.println("config.properties file not found in resources.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns the version of the connector, loaded from config.properties.
     * 
     * @return the connector version or "unknown-version" if not available
     */
    @Override
    public String version() {
        return properties.getProperty("app.version", "unknown-version");
    }

    /**
     * Starts the connector and validates the configuration properties.
     * 
     * @param props configuration key-value pairs provided when the connector is instantiated
     */
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

    /**
     * Returns the class that should be instantiated for running tasks.
     * 
     * @return the {@link WebSocketSourceTask} class
     */
    @Override
    public Class<? extends Task> taskClass() {
        return WebSocketSourceTask.class;
    }

    /**
     * Returns a list of configurations for each task based on the connector configuration.
     * 
     * @param maxTasks maximum number of tasks to generate configurations for
     * @return a list of task configurations
     */
    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        // Create a single task configuration that reuses the connector's properties
        List<Map<String, String>> configs = new ArrayList<>();
        
        // Pass the connector's configuration as is
        configs.add(new HashMap<>(this.configProperties));
        
        return configs;
    }

    /**
     * Stops the connector.
     * This implementation does not allocate any resources that need explicit cleanup.
     */
    @Override
    public void stop() {
        // Clean up resources if needed
    }

    /**
     * Returns the {@link ConfigDef} that defines the configuration for this connector.
     * 
     * @return the configuration definition
     */
    public ConfigDef config() {
        return CONFIG_DEF;
    }
}