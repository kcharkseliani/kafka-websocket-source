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

        // Validate essential configurations
        new WebSocketSourceConnectorConfig(props);

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
        return WebSocketSourceConnectorConfig.CONFIG_DEF;
    }
}