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

public class WebSocketSourceTask extends SourceTask {

    private WebSocketClient client;
    private String kafkaTopic;
    private LinkedBlockingQueue<SourceRecord> recordsQueue = new LinkedBlockingQueue<>();
    private WebSocketClientFactory clientFactory = new DefaultWebSocketClientFactory();

    @Override
    public String version() {
        return "0.0";
    }

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

    @Override
    public List<SourceRecord> poll() {
        List<SourceRecord> records = new ArrayList<>();
        recordsQueue.drainTo(records);
        return records;
    }

    @Override
    public void stop() {
        client.close();
    }

    // For testing: allows setting a mock factory
    void setWebSocketClientFactory(WebSocketClientFactory factory) {
        this.clientFactory = factory;
    }
}
