import boto3
import time

kinesis = boto3.client(
    "kinesis",
    endpoint_url="http://localhost:4566",
    region_name="us-east-1",
    aws_access_key_id="test",
    aws_secret_access_key="test",
)

stream_name = "output-stream"

shard_id = kinesis.describe_stream(
    StreamName=stream_name
)["StreamDescription"]["Shards"][0]["ShardId"]

shard_iterator = kinesis.get_shard_iterator(
    StreamName=stream_name,
    ShardId=shard_id,
    ShardIteratorType="TRIM_HORIZON",
)["ShardIterator"]

print("[KINESIS] Listening for records...")

while True:
    response = kinesis.get_records(
        ShardIterator=shard_iterator,
        Limit=10,
    )

    for record in response["Records"]:
        print("[KINESIS] Received:", record["Data"].decode())

    shard_iterator = response["NextShardIterator"]
    time.sleep(1)
