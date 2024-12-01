package com.kcharkseliani.kafka.connect.websocket;

import org.java_websocket.client.WebSocketClient;

import java.net.URI;

public interface WebSocketClientFactory {
    WebSocketClient createClient(URI uri, String subscriptionMessage, MessageHandler messageHandler);
}

