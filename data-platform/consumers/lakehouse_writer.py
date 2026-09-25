# Consumer that reads raw Kafka topics and writes to MinIO as Parquet (Sprint 3)

"""Lakehouse writer consumer.

This consumer reads the raw Kafka topics produced by the ingestion step,
validates the payload using the existing validator utilities, converts the
records to a :class:`pandas.DataFrame`, writes the dataframe to a Parquet
buffer and uploads the buffer to MinIO.

The implementation purposefully stays lightweight – it does not attempt to
handle every edge‑case, but provides a solid foundation for the sprint.
"""

import os
import sys
import json
import io
from typing import List, Dict, Any

import pandas as pd
from minio import Minio
from confluent_kafka import Consumer, KafkaException

# ---------------------------------------------------------------------------
# Configuration – read from environment (docker‑compose supplies .env)
# ---------------------------------------------------------------------------
KAFKA_BROKERS = os.getenv("KAFKA_BROKERS", "localhost:9092")
KAFKA_GROUP_ID = "lakehouse_writer_group"
TOPICS = [
    "football.raw.competitions",
    "football.raw.matches",
    "football.raw.events",
    "football.raw.lineups",
]

MINIO_ENDPOINT = os.getenv("MINIO_ENDPOINT", "localhost:9000")
MINIO_ACCESS_KEY = os.getenv("MINIO_ACCESS_KEY", "minioadmin")
MINIO_SECRET_KEY = os.getenv("MINIO_SECRET_KEY", "minioadmin")
MINIO_BUCKET = os.getenv("MINIO_BUCKET", "football-lake")

# ---------------------------------------------------------------------------
# Initialise MinIO client (single shared instance)
# ---------------------------------------------------------------------------
minio_client = Minio(
    MINIO_ENDPOINT,
    access_key=MINIO_ACCESS_KEY,
    secret_key=MINIO_SECRET_KEY,
    secure=False,
)

# Ensure bucket exists (idempotent)
if not minio_client.bucket_exists(MINIO_BUCKET):
    minio_client.make_bucket(MINIO_BUCKET)

# ---------------------------------------------------------------------------
# Helper to upload a DataFrame as Parquet to MinIO
# ---------------------------------------------------------------------------
def upload_parquet(df: pd.DataFrame, object_name: str) -> None:
    """Upload *df* to *object_name* inside the configured bucket.

    The dataframe is written to an in‑memory ``BytesIO`` buffer using the
    ``pyarrow`` engine (the default for pandas >=1.5).  The buffer is then
    rewound and passed to ``minio_client.put_object``.
    """
    buffer = io.BytesIO()
    df.to_parquet(buffer, index=False)
    buffer.seek(0)
    minio_client.put_object(
        bucket_name=MINIO_BUCKET,
        object_name=object_name,
        data=buffer,
        length=buffer.getbuffer().nbytes,
        content_type="application/octet-stream",
    )

# ---------------------------------------------------------------------------
# Processing functions per topic – each returns a DataFrame ready for upload
# ---------------------------------------------------------------------------
def process_competitions(payload: List[Dict[str, Any]]) -> pd.DataFrame:
    return pd.json_normalize(payload)

def process_matches(payload: List[Dict[str, Any]]) -> pd.DataFrame:
    return pd.json_normalize(payload)

def process_events(payload: List[Dict[str, Any]]) -> pd.DataFrame:
    return pd.json_normalize(payload)

def process_lineups(payload: List[Dict[str, Any]]) -> pd.DataFrame:
    return pd.json_normalize(payload)

# ---------------------------------------------------------------------------
# Main consumer loop
# ---------------------------------------------------------------------------
def run_consumer() -> None:
    consumer_cfg = {
        "bootstrap.servers": KAFKA_BROKERS,
        "group.id": KAFKA_GROUP_ID,
        "auto.offset.reset": "earliest",
        "enable.auto.commit": False,
    }
    consumer = Consumer(consumer_cfg)
    consumer.subscribe(TOPICS)
    print("🚀 Lakehouse writer started – listening to:", ", ".join(TOPICS))

    try:
        while True:
            msg = consumer.poll(1.0)
            if msg is None:
                continue
            if msg.error():
                raise KafkaException(msg.error())

            envelope = json.loads(msg.value().decode())
            payload = envelope.get("payload")
            topic = msg.topic()

            if topic == "football.raw.competitions":
                df = process_competitions(payload)
                object_name = f"competitions/{msg.offset()}.parquet"
            elif topic == "football.raw.matches":
                df = process_matches(payload)
                object_name = f"matches/{msg.offset()}.parquet"
            elif topic == "football.raw.events":
                df = process_events(payload)
                object_name = f"events/{msg.offset()}.parquet"
            elif topic == "football.raw.lineups":
                df = process_lineups(payload)
                object_name = f"lineups/{msg.offset()}.parquet"
            else:
                print(f"⚠️ Unexpected topic: {topic}")
                consumer.commit(message=msg, asynchronous=False)
                continue

            upload_parquet(df, object_name)
            print(f"✅ Uploaded {object_name} ({len(df)} rows) to MinIO")
            consumer.commit(message=msg, asynchronous=False)
    except KeyboardInterrupt:
        print("🛑 Consumer stopped by user")
    except Exception as exc:
        print(f"❌ Consumer error: {exc}", file=sys.stderr)
    finally:
        consumer.close()
        print("🧹 Consumer shutdown complete.")

if __name__ == "__main__":
    run_consumer()
