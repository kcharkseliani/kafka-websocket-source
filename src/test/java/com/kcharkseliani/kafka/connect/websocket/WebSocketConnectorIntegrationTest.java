package com.kcharkseliani.kafka.connect.websocket;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WebSocketConnectorIntegrationTest {

    private KafkaContainer kafka;
    private GenericContainer<?> connect;

    @BeforeEach
    void setup() {
        System.out.println("Testcontainers Docker available: " + DockerClientFactory.instance().isDockerAvailable());
        
        kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));
        kafka.start();

        connect = new GenericContainer<>(DockerImageName.parse("confluentinc/cp-kafka-connect:7.6.0"))
                .withEnv("CONNECT_BOOTSTRAP_SERVERS", kafka.getBootstrapServers())
                .withEnv("CONNECT_REST_PORT", "8083")
                .withEnv("CONNECT_REST_ADVERTISED_HOST_NAME", "localhost")
                .withEnv("CONNECT_GROUP_ID", "connect-cluster")
                .withEnv("CONNECT_CONFIG_STORAGE_TOPIC", "connect-configs")
                .withEnv("CONNECT_OFFSET_STORAGE_TOPIC", "connect-offsets")
                .withEnv("CONNECT_STATUS_STORAGE_TOPIC", "connect-status")
                .withEnv("CONNECT_KEY_CONVERTER", "org.apache.kafka.connect.storage.StringConverter")
                .withEnv("CONNECT_VALUE_CONVERTER", "org.apache.kafka.connect.storage.StringConverter")
                .withEnv("CONNECT_KEY_CONVERTER_SCHEMAS_ENABLE", "false")
                .withEnv("CONNECT_VALUE_CONVERTER_SCHEMAS_ENABLE", "false")
                .withEnv("CONNECT_INTERNAL_KEY_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
                .withEnv("CONNECT_INTERNAL_VALUE_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
                .withEnv("CONNECT_PLUGIN_PATH", "/plugins")
                .withFileSystemBind("target/plugins", "/plugins", BindMode.READ_ONLY) // adjust to your connector JAR path
                .withExposedPorts(8083)
                .dependsOn(kafka);

        connect.start();
    }

    @Test
    void testConnectorWorks() throws Exception {
        String connectUrl = "http://" + connect.getHost() + ":" + connect.getMappedPort(8083);

        String configJson = "{\n" +
            "  \"name\": \"websocket-source-connector\",\n" +
            "  \"config\": {\n" +
            "    \"connector.class\": \"com.kcharkseliani.kafka.connect.websocket.WebSocketSourceConnector\",\n" +
            "    \"tasks.max\": \"1\",\n" +
            "    \"websocket.url\": \"wss://ws.kraken.com/v2\",\n" +
            "    \"topic\": \"trades\",\n" +
            "    \"websocket.subscription.message\": \"{ \\\"method\\\": \\\"subscribe\\\", \\\"params\\\": { \\\"channel\\\": \\\"trade\\\", \\\"symbol\\\": [\\\"BTC/USD\\\"], \\\"snapshot\\\": false } }\"\n" +
            "  }\n" +
            "}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(connectUrl + "/connectors"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(configJson))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode(), "Connector creation failed: " + response.body());

        // Optional wait if you want to inspect topic data later
        Thread.sleep(5_000);
    }   

    @AfterEach
    void teardown() {
        if (connect != null && connect.isRunning()) {
            connect.stop();
        }
        if (kafka != null && kafka.isRunning()) {
            kafka.stop();
        }
    }
}
