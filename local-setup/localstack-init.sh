#!/bin/bash
set -e

echo "Creating Kinesis stream..."

aws --endpoint-url=http://localhost:4566 \
  kinesis create-stream \
  --stream-name output-stream \
  --region us-east-1 \
  --shard-count 1 || true


echo "Kinesis stream 'output-stream' ready."

