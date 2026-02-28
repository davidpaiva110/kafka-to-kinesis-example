import json
import time
from kafka import KafkaProducer

producer = KafkaProducer(
    bootstrap_servers="localhost:9092",
    value_serializer=lambda v: json.dumps(v).encode("utf-8"),
)

topic = "input-topic"
counter = 0

print(f"Starting producer on topic: {topic}. Press Ctrl+C to stop.")

try:
    while True:

        for j in range(1):
            counter += 1
            msg = {
                "id": str(counter),
                "name": "example",
                "value": 250.0,
                "timestamp": int(time.time() * 1000) 
            }
            producer.send(topic=topic, value=msg)

        print(f"[PRODUCER] Sent 100 messages")
        
        time.sleep(1)

except KeyboardInterrupt:
    print("\nStopping producer...")
finally:
    producer.flush()
    producer.close()