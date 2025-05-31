// SPDX-License-Identifier: MIT
// Copyright (c) 2025 Konstantin Charkseliani

package com.kcharkseliani.kafka.connect.websocket;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.java_websocket.client.WebSocketClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.InputStream;
import java.util.Properties;

/**
 * A Kafka Connect {@link SourceTask} implementation that receives messages from a WebSocket server
 * and produces them to a Kafka topic as {@link SourceRecord} entries.
 */
public class WebSocketSourceTask extends SourceTask {

    /** WebSocket client used to connect to the WebSocket server. */
    private WebSocketClient client;

    /** Kafka topic where incoming WebSocket messages are published. */
    private String kafkaTopic;

    /** Queue holding SourceRecords waiting to be sent to Kafka. */
    private LinkedBlockingQueue<SourceRecord> recordsQueue = new LinkedBlockingQueue<>();

    /** Factory for creating WebSocket clients. */
    private WebSocketClientFactory clientFactory = new DefaultWebSocketClientFactory();

    /** Static configuration properties loaded from config.properties. */
    private static final Properties properties = new Properties();

    // Static initializer to load config.properties at class loading time
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
     * @return the version string, or "unknown-version" if not found
     */
    @Override
    public String version() {
        return properties.getProperty("app.version", "unknown-version");
    }

    /**
     * Initializes the task by connecting to the configured WebSocket URL
     * and setting up a message handler to capture incoming WebSocket messages.
     *
     * @param props task-specific configuration properties
     */
    @Override
    public void start(Map<String, String> props) {
        kafkaTopic = props.get("topic");
        String subscriptionMessage = props.get("websocket.subscription.message"); // Retrieve subscription message

        // Pass the subscription message to the client
        client = clientFactory.createClient(URI.create(props.get("websocket.url")), subscriptionMessage, message -> {
            SourceRecord record = new SourceRecord(
                null, null, kafkaTopic, Schema.STRING_SCHEMA, message
            );
            recordsQueue.add(record);
        });       

        client.connect();
    }

    /**
     * Polls the queue of received WebSocket messages and returns them as a batch of SourceRecords.
     *
     * @return a list of SourceRecords to be sent to Kafka
     */
    @Override
    public List<SourceRecord> poll() {
        List<SourceRecord> records = new ArrayList<>();
        recordsQueue.drainTo(records);
        return records;
    }

    /**
     * Stops the task by closing the WebSocket connection.
     */
    @Override
    public void stop() {
        client.close();
    }

    /**
     * Allows setting a custom WebSocketClientFactory, primarily for testing purposes.
     *
     * @param factory the WebSocketClientFactory to use
     */
    void setWebSocketClientFactory(WebSocketClientFactory factory) {
        this.clientFactory = factory;
    }
}
