#!/bin/bash
set -e

echo "Waiting for Kafka to be ready..."
sleep 10

kafka-topics \
  --bootstrap-server kafka:29092 \
  --create \
  --if-not-exists \
  --topic input-topic \
  --partitions 1 \
  --replication-factor 1

echo "Kafka topic 'input-topic' created."
