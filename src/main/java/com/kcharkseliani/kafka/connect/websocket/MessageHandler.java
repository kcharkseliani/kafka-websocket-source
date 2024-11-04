package com.kcharkseliani.kafka.connect.websocket;

@FunctionalInterface
public interface MessageHandler {
    void handle(String message);
}