import json
import uuid
from datetime import datetime, timezone

from confluent_kafka import Producer

class FootballKafkaProducer:
    """Simple wrapper around confluent_kafka Producer that emits a standard envelope.

    All DE raw events are published to topics like ``football.raw.<entity>``.
    The envelope contains meta‑information required by downstream consumers and
    the AI analyst layer.
    """

    def __init__(self, brokers: str = "localhost:9092"):
        self._producer = Producer({"bootstrap.servers": brokers})

    def publish(self, topic: str, source: str, payload: dict, key: str | None = None) -> None:
        """Publish a payload to *topic*.

        Parameters
        ----------
        topic: str
            The Kafka topic name (e.g. ``football.raw.matches``).
        source: str
            Identifier of the originating provider (``statsbomb`` or ``football_data_org``).
        payload: dict
            The raw JSON‑serialisable data.
        key: str | None
            Optional key for partitioning; defaults to empty string.
        """
        envelope = {
            "event_id": str(uuid.uuid4()),
            "event_type": topic,
            "source": source,
            "schema_version": 1,
            "occurred_at": datetime.now(timezone.utc).isoformat(),
            "ingested_at": datetime.now(timezone.utc).isoformat(),
            "payload": payload,
        }
        self._producer.produce(
            topic=topic,
            key=(key or "").encode(),
            value=json.dumps(envelope).encode(),
            callback=self._delivery_report,
        )
        # Immediately serve delivery callbacks
        self._producer.poll(0)

    def flush(self) -> None:
        """Block until all queued messages are delivered."""
        self._producer.flush()

    @staticmethod
    def _delivery_report(err, msg):  # pragma: no cover (callback)
        if err:
            print(f"❌ Delivery failed for {msg.topic()}: {err}")
        else:
            print(f"✅ Delivered to {msg.topic()} [{msg.partition()}] @ offset {msg.offset()}")
