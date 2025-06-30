# kafka-websocket-source

A Kafka Connect **source connector** that establishes a WebSocket connection to a remote server, optionally sends a subscription message, and streams incoming messages into a Kafka topic.

## Features

- Connects to a specified WebSocket URL.
- Sends an initial subscription message if configured.
- Periodically sends application-level ping messages
- Matches incoming pong messages using a specified regex pattern to log and exclude from the message stream
- Converts incoming WebSocket messages into Kafka `SourceRecord` entries.
- Fully compatible with Kafka Connect standalone and distributed modes.
- Supports integration testing using Testcontainers.

## Use Cases

- Stream real-time market data, events, or telemetry into Kafka.
- Bridge WebSocket-based APIs with Kafka consumers.
- Integrate with public or private WebSocket feeds.

## Quickstart

### 1. Build the Connector

```bash
mvn clean package
```

The resulting JAR will be located in `target/`.

### 2. Install the JAR in Kafka Connect
Place the connector JAR into your Kafka Connect `plugin.path`, e.g.:

```bash
mkdir -p /path/to/connect-plugins/websocket-connector
cp kafka-websocket-source-connector-*.jar /path/to/connect-plugins/websocket-connector/
```

Ensure your Kafka Connect worker config includes:

```properties
plugin.path=/path/to/connect-plugins
```

### 3. Kafka Connect Configuration

Example contents of the JSON config file for the connector:

```json
{
  "name": "websocket-source-connector",
  "config": {
    "connector.class": "com.kcharkseliani.kafka.connect.websocket.WebSocketSourceConnector",
    "tasks.max": "1",
    "websocket.url": "wss://example.com/feed",
    "topic": "websocket-topic",
    "websocket.subscription.message": "{\"type\": \"subscribe\"}",
    "websocket.ping.message": "{\"method\":\"ping\"}",
    "websocket.ping.interval.ms": 20000,
    "websocket.pong.pattern": "\\\"method\\\"\\s*:\\s*\\\"pong\\\""
  }
}
```

### 4. Running the Connector

Once Kafka Connect is running, you can deploy the WebSocket source connector using a configuration file with the contents from the section above:

```bash
curl -X POST http://<CONNECT_HOST>:8083/connectors \
  -H "Content-Type: application/json" \
  -d @connector-config.json
```
Alternatively, you can provide the connector configuration inline as a JSON string:
```bash
curl -X POST http://<CONNECT_HOST>:8083/connectors \
  -H "Content-Type: application/json" \
  -d '<JSON_STRING_HERE>'
```

## Configuration Options

| Property                          | Required | Description                                              |
|-----------------------------------|----------|----------------------------------------------------------|
| `websocket.url`                   | Yes      | WebSocket server URL to connect to.                     |
| `topic`                           | Yes      | Kafka topic to publish received messages.               |
| `websocket.subscription.message`  | No       | Message to send after connection (e.g., subscription).  |
| `websocket.ping.message`          | No       | Optional ping message to send periodically to keep the WebSocket connection alive.  |
| `websocket.ping.interval.ms`      | No       | Interval in milliseconds between each ping message. Defaults to 20000 (20 seconds) |
| `websocket.pong.pattern`          | No       | Regex pattern to detect pong responses in incoming WebSocket messages. If not provided, pong responses will be sent alongside other messages.  |

## Development

This project uses:

- Java 17
- Maven
- Kafka Connect API
- [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket)
- Testcontainers for integration testing

### Run Full Build and Tests

```bash
mvn clean verify
```

This runs a full build and executes all tests, including integration tests that use Testcontainers to spin up Kafka, Kafka Connect, and a local WebSocket server to validate full end-to-end behavior.

To run integration tests separately:
```bash
mvn failsafe:integration-test failsafe:verify
```
> **Note:** Docker must be available and running for integration tests to work.

### Project Structure

- `WebSocketSourceConnector`: Main entry point implementing `SourceConnector`.
- `WebSocketSourceTask`: Handles data streaming logic from WebSocket to Kafka.
- `WebSocketSourceConnectorConfig`: Handles `ConfigDef` initialisation and definition and validation of connector configuration
- `WebSocketClientFactory`: Interface for factories instantiating websocket clients.
- `DefaultWebSocketClientFactory`: Creates WebSocket client instances.
- `MessageHandler`: Functional interface for handling messages received from a WebSocket connection.

## Publishing

You can release the connector as:

- A GitHub Release (upload the `.jar`)

## License

MIT License. See [LICENSE](./LICENSE) for details.


