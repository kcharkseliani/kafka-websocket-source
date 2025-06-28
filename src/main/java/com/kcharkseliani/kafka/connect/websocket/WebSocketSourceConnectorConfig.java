package com.kcharkseliani.kafka.connect.websocket;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.Map;

public class WebSocketSourceConnectorConfig extends AbstractConfig {

    /** Defines the configuration options supported by this connector. */
    public static final ConfigDef CONFIG_DEF = new ConfigDef()
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

    public WebSocketSourceConnectorConfig(Map<String, String> props) {
        super(CONFIG_DEF, props);
    }
}
