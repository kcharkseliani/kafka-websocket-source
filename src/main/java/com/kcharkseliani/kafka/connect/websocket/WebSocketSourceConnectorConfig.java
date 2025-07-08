// SPDX-License-Identifier: MIT
// Copyright (c) 2025 Konstantin Charkseliani

package com.kcharkseliani.kafka.connect.websocket;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.Map;

/**
 * Configuration class for the {@link WebSocketSourceConnector}.
 *
 * <p>This class defines and validates all configuration options for the connector.
 * It extends {@link AbstractConfig} to provide access to typed values with defaults
 * and validation checks.</p>
 *
 * <p>Supported configuration parameters:</p>
 * <ul>
 *   <li><b>websocket.url</b> (String, required): The WebSocket endpoint to connect to</li>
 *   <li><b>topic</b> (String, required): The Kafka topic to publish incoming WebSocket messages</li>
 *   <li><b>websocket.subscription.message</b> (String, optional): Optional subscription message sent immediately after connection</li>
 *   <li><b>websocket.ping.message</b> (String, optional): Optional ping message sent periodically to keep the connection alive</li>
 *   <li><b>websocket.ping.interval.ms</b> (int, optional, default: 20000): Interval between pings in milliseconds</li>
 *   <li><b>websocket.pong.pattern</b> (String, optional): Regex pattern used to filter out pong responses from the WebSocket stream</li>
 * </ul>
 */
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

    /**
     * Constructs a new {@link WebSocketSourceConnectorConfig} using the given properties.
     *
     * @param props the raw configuration properties provided by the Kafka Connect framework
     */
    public WebSocketSourceConnectorConfig(Map<String, String> props) {
        super(CONFIG_DEF, props);
    }
}