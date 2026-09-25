"""Create required Kafka topics for raw ingestion.
All topics are created with 3 partitions and replication factor 1 (single-node Redpanda).
"""
from confluent_kafka.admin import AdminClient, NewTopic
import sys

BROKERS = "localhost:9092"
TOPICS = [
    "football.raw.competitions",
    "football.raw.matches",
    "football.raw.events",
    "football.raw.lineups",
]

def create_topics():
    admin = AdminClient({"bootstrap.servers": BROKERS})
    existing = set(admin.list_topics(timeout=5).topics.keys())
    new_topics = []
    for topic in TOPICS:
        if topic not in existing:
            new_topics.append(NewTopic(topic, num_partitions=3, replication_factor=1))
    if not new_topics:
        print("✅ All required topics already exist.")
        return
    fs = admin.create_topics(new_topics)
    for topic, f in fs.items():
        try:
            f.result()
            print(f"✅ Created topic: {topic}")
        except Exception as e:
            print(f"❌ Failed to create topic {topic}: {e}", file=sys.stderr)

if __name__ == "__main__":
    create_topics()
