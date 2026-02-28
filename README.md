# Kafka to Kinesis POC

A Scala-based POC that reads messages from Kafka, transforms them, and publishes to Kinesis using Pekko Streams and functional programming patterns with Either-based error handling.

## Architecture

```
Kafka Topic → Pekko Stream → Transform → Kinesis Stream
```

- **Kafka Consumer**: Reads JSON messages from configured topic
- **Transformation**: Transforms messages using functional patterns
- **Error Handling**: Uses Either monad with Iron framework
- **Kinesis Producer**: Publishes transformed messages to Kinesis
- **Configuration**: HOCON-based configuration with environment variable overrides

## Prerequisites

- JDK 11 or higher
- Gradle 8.x
- Docker and Docker Compose

## Project Structure

```
.
├── build.gradle.kts             # Gradle build configuration
├── docker-compose.yml           # Local infrastructure setup
├── local-setup/
│   ├── create-topic.sh          # Script to create Kafka topic
│   └── localstack-init.sh       # Script to create Kinesis stream
├── local-setup/
│   ├── kafka_producer.py        # Script to produce test messages to Kafka
│   └── kinesis_consumer.py      # Script to consume messages from Kinesis
├── src/main/
│   ├── scala/com/example/kafka/kinesis/
│   │   ├── Main.scala           # Application entry point
│   │   ├── config/
│   │   │   └── AppConfig.scala  # Configuration loader
│   │   ├── domain/
│   │   │   └── Domain.scala     # Domain models and errors
│   │   ├── service/
│   │   │   └── MessageTransformer.scala  # Message transformation logic
│   │   └── stream/
│   │       └── StreamProcessor.scala     # Pekko Streams pipeline
│   └── resources/
│       ├── application.conf      # Application configuration
│       └── logback.xml           # Logging configuration
└── README.md
```

## Configuration

Edit `src/main/resources/application.conf` to configure:

```hocon
kafka {
  bootstrap-servers = "localhost:9092"
  consumer {
    topic = "input-topic"          # Change this to read from different topic
    group-id = "kafka-kinesis-poc"
  }
}

kinesis {
  endpoint = "http://localhost:4566"
  stream-name = "output-stream"     # Change this to write to different stream
  region = "us-east-1"
}
```

## Local Setup

### 1. Start Infrastructure

```bash
docker-compose up -d
```

This starts:
- **Zookeeper** (port 2181)
- **Kafka** (port 9092)
- **Kafka UI** (port 8080) - Access at http://localhost:8080
- **LocalStack** (port 4566) - Emulates AWS Kinesis
- **Kafka Initializer** - Creates `input-topic`

Note: Make sure that the scripts inside local-setup folder has execution permissions.

### 2. Verify Services

Check Kafka UI at http://localhost:8080

Verify Kinesis stream:
```bash
aws --endpoint-url=http://localhost:4566 kinesis list-streams --region us-east-1
```

## Build and Run

### Build the project

```bash
./gradlew build
```

### Run the application

```bash
./gradlew run
```

## Testing the Application

### 1. Install Python scripts dependencies.

```bash
pip install -r ./scripts/requirements.txt
```


### 1. Produce messages to Kafka
Execute the script to produce test messages:
```bash
python ./scripts/kafka_producer.py
```

### 2. Verify Kinesis output
Read from Kinesis stream using the script:
```bash
python ./scripts/kinesis_consumer.py
```

## Message Transformation

The current implementation:
- Converts `name` to uppercase
- Doubles the `value` field
- Adds `processedAt` timestamp
- Preserves original fields

Example:

**Input:**
```json
{"id":"1","name":"test","value":100.5,"timestamp":1704067200000}
```

**Output:**
```json
{
  "id":"1",
  "name":"TEST",
  "value":100.5,
  "transformedValue":201.0,
  "timestamp":1704067200000,
  "processedAt":1704153600000
}
```

## Error Handling

Errors are handled using Either monad:
- `JsonParseError`: Failed to parse JSON
- `ValidationError`: Validation failed
- `TransformationError`: Transformation logic failed
- `KinesisPublishError`: Failed to publish to Kinesis

All errors are logged with details. Failed messages do not stop the stream.

## Cleanup

```bash
docker-compose down -v
```

## Next Steps for Production Scenarios:

1. **Remove LocalStack credentials** from `application.conf`
2. **Use IAM roles** instead of access keys
3. **Update Kinesis endpoint** to real AWS endpoint
4. **Configure proper error handling** (DLQ, retry logic)
5. **Add monitoring** (metrics, health checks)
6. **Tune Pekko settings** for throughput
7. **Configure proper consumer group management**
8. Add Unit and Integration Tests
