package com.kcharkseliani.kafka.connect.websocket;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import com.kcharkseliani.kafka.connect.websocket.util.MockWebSocketServer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.kafka.clients.admin.*;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.testcontainers.containers.Network;

public class WebSocketConnectorIT {

    private KafkaContainer kafka;
    private GenericContainer<?> connect;
    private Network network;

    private MockWebSocketServer websocketServer;
    private static final int WEBSOCKET_PORT = 9001;
    private static final String TOPIC = "trades";

    @BeforeEach
    void setup() throws Exception {
        System.out.println("Testcontainers Docker available: " + DockerClientFactory.instance().isDockerAvailable());

        network = Network.newNetwork();
        
        kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.0"))
        .withNetwork(network)
        .withNetworkAliases("kafka")
        .withEnv("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://kafka:9092") // important!
        .withEnv("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:9092")
        .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true"); // ensures the connect-* topics can be created

        kafka.start();

        String kafkaInternalHost = kafka.getNetworkAliases().get(0); // something like 'testcontainers-kafka-1'
        int kafkaPort = 9092; // internal PLAINTEXT listener for the container

        try {
            createKafkaConnectInternalTopics(kafka.getBootstrapServers());
        } 
        catch (Exception e) {
            throw new RuntimeException("Failed to create Kafka Connect internal topics", e);
        }

        connect = new GenericContainer<>(DockerImageName.parse("confluentinc/cp-kafka-connect:7.8.0"))
                .withNetwork(network)
                .withEnv("CONNECT_BOOTSTRAP_SERVERS", "kafka:9092")
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
                .withEnv("CONNECT_PLUGIN_PATH", "/plugins/")
                .withEnv("CONNECT_PLUGIN_DISCOVERY", "service_load")
                .withFileSystemBind("target/plugins", "/plugins", BindMode.READ_ONLY) // adjust to your connector JAR path
                .withExposedPorts(8083)
                .dependsOn(kafka);

        connect.start();   

        websocketServer = new MockWebSocketServer(new InetSocketAddress("localhost", WEBSOCKET_PORT));
        websocketServer.start();

        Thread.sleep(5_000);  
    }

    @Test
    void testConnectorWorks() throws Exception {
        deployWebSocketConnector();
    }   

    @AfterEach
    void teardown() throws InterruptedException {
        if (websocketServer != null) {
            websocketServer.stop();
        }
        if (connect != null && connect.isRunning()) {
            connect.stop();
        }
        if (kafka != null && kafka.isRunning()) {
            kafka.stop();
        }
        if (network != null) {
            network.close();
        }
    }

    private void deployWebSocketConnector() throws Exception {
        String connectUrl = "http://" + connect.getHost() + ":" + connect.getMappedPort(8083);

        String configJson = "{\n" +
            "  \"name\": \"websocket-source-connector\",\n" +
            "  \"config\": {\n" +
            "    \"connector.class\": \"com.kcharkseliani.kafka.connect.websocket.WebSocketSourceConnector\",\n" +
            "    \"tasks.max\": \"1\",\n" +
            "    \"websocket.url\": \"ws://localhost:" + WEBSOCKET_PORT + "\",\n" +
            "    \"topic\": \"" + TOPIC + "\",\n" +
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
    }

    private void createKafkaConnectInternalTopics(String bootstrapServers) throws Exception {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient admin = AdminClient.create(props)) {
            List<NewTopic> topics = List.of(
                new NewTopic("connect-offsets", 1, (short) 1).configs(Map.of("cleanup.policy", "compact")),
                new NewTopic("connect-configs", 1, (short) 1).configs(Map.of("cleanup.policy", "compact")),
                new NewTopic("connect-status", 1, (short) 1).configs(Map.of("cleanup.policy", "compact"))
            );

            admin.createTopics(topics).all().get(); // blocks until topics are created
        }
    }
}