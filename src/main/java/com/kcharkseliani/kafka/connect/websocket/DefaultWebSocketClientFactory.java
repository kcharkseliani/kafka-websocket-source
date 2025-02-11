package com.kcharkseliani.kafka.connect.websocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class DefaultWebSocketClientFactory implements WebSocketClientFactory {

    @Override
    public WebSocketClient createClient(URI uri, String subscriptionMessage, MessageHandler messageHandler) {
        return new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("Connected to WebSocket");
                if (subscriptionMessage != null && !subscriptionMessage.isEmpty()) {
                    send(subscriptionMessage); // Send the subscription message
                    System.out.println("Sent the initial subscription message to the websocket");
                }
            }
    
            @Override
            public void onMessage(String message) {
                messageHandler.handle(message); // Use the lambda to handle the message
            }
    
            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("WebSocket closed: " + reason);
            }
    
            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
            }
        };
    }
}

