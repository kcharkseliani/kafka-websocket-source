package com.kcharkseliani.kafka.connect.websocket;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import com.kcharkseliani.kafka.connect.websocket.util.MockWebSocketServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

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
                .withExtraHost("host.testcontainers.internal", "host-gateway")
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

    @Test
    void testWebSocketConnection() throws Exception {
        deployWebSocketConnector();

        // Wait up to 5 seconds for connector to establish WebSocket connection
        boolean connected = waitForWebSocketConnection(5_000);

        assertTrue(connected, "Connector should establish a WebSocket connection to the server");
    }

    @Test
    void testWebSocketSubscriptionMessageSent() throws Exception {
        // Step 1: Deploy the WebSocket Kafka Connector
        deployWebSocketConnector();

        // Step 2: Wait for connector to establish WebSocket connection
        boolean connected = waitForWebSocketConnection(5_000);
        assertTrue(connected, "Connector should establish a WebSocket connection to the mock server");

        // Step 3: Wait a short time to allow the connector to send the subscription message
        Thread.sleep(1_000);

        // Step 4: Retrieve the messages received by the mock server
        List<String> receivedMessages = websocketServer.getReceivedMessages();

        // Step 5: Verify that a subscription message was received
        boolean subscriptionReceived = receivedMessages.stream()
            .anyMatch(msg -> msg.contains("\"method\": \"subscribe\""));

        assertTrue(subscriptionReceived, "Expected subscription message was not received by the WebSocket server.");
    }

    @Test
    void testWebSocketMessageEndToEnd() throws Exception {
        // Step 1: Deploy the WebSocket Kafka Connector
        deployWebSocketConnector();

        // Step 2: Wait for the connector to establish a WebSocket connection
        boolean connected = waitForWebSocketConnection(5_000);
        assertTrue(connected, "Connector should establish a WebSocket connection to the mock server");

        // Step 3: Send a test WebSocket message through the mock WebSocket server
        String testMessage = "{\"type\": \"trade\", \"price\": \"50000\"}";
        websocketServer.broadcast(testMessage);

        // Step 4: Configure Kafka consumer properties
        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", kafka.getBootstrapServers()); // Connect to the Kafka container
        consumerProps.put("group.id", "test-consumer-group"); // Group ID for isolation
        consumerProps.put("key.deserializer", StringDeserializer.class.getName()); // Key deserializer
        consumerProps.put("value.deserializer", StringDeserializer.class.getName()); // Value deserializer
        consumerProps.put("auto.offset.reset", "earliest"); // Make sure we read messages from the beginning

        // Step 5: Create a Kafka consumer to consume from the 'trades' topic
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(List.of(TOPIC)); // Subscribe to the topic produced by the connector

            boolean messageReceived = false;
            long timeoutMillis = 5000; // How long to wait for the message
            long start = System.currentTimeMillis();

            // Step 6: Poll Kafka for new messages until timeout expires
            while (System.currentTimeMillis() - start < timeoutMillis) {
                ConsumerRecords<String, String> records = consumer.poll(java.time.Duration.ofMillis(500)); // poll every 500ms

                for (ConsumerRecord<String, String> record : records) {
                    System.out.println("Received message from Kafka: " + record.value());

                    // Step 7: Check if the message matches what we broadcast
                    if (record.value().contains("\"price\": \"50000\"")) {
                        messageReceived = true;
                        break;
                    }
                }

                if (messageReceived) {
                    break; // Stop polling once we found the expected message
                }
            }

            // Step 8: Assert that we successfully received the WebSocket message in Kafka
            assertTrue(messageReceived, "Expected WebSocket message was not found in Kafka topic 'trades'.");
        }
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

    private void deployWebSocketConnector() throws Exception {
        String connectUrl = "http://" + connect.getHost() + ":" + connect.getMappedPort(8083);

        String configJson = "{\n" +
            "  \"name\": \"websocket-source-connector\",\n" +
            "  \"config\": {\n" +
            "    \"connector.class\": \"com.kcharkseliani.kafka.connect.websocket.WebSocketSourceConnector\",\n" +
            "    \"tasks.max\": \"1\",\n" +
            "    \"websocket.url\": \"ws://host.testcontainers.internal:" + WEBSOCKET_PORT + "\",\n" +
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

    private boolean waitForWebSocketConnection(long timeoutMillis) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMillis) {
            if (websocketServer.getConnections().size() >= 1) {
                return true;
            }
            Thread.sleep(100); // sleep 100ms between retries
        }
        return false;
    }
}