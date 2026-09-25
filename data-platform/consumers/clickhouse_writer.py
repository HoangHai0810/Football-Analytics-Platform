# Consumer that reads raw Kafka topics and writes to ClickHouse tables (Sprint 2).

import json
import os
import sys
from typing import Any, Dict, List

import clickhouse_connect
from confluent_kafka import Consumer, KafkaException

# ---------------------------------------------------------------------------
# Configuration – load from environment (Docker compose provides .env)
# ---------------------------------------------------------------------------
KAFKA_BROKERS = os.getenv("KAFKA_BROKERS", "localhost:9092")
KAFKA_GROUP_ID = "clickhouse_writer_group"
TOPICS = [
    "football.raw.competitions",
    "football.raw.matches",
    "football.raw.events",
    "football.raw.lineups",
]

CLICKHOUSE_HOST = os.getenv("CLICKHOUSE_HOST", "localhost")
CLICKHOUSE_HTTP_PORT = int(os.getenv("CLICKHOUSE_HTTP_PORT", "8123"))
CLICKHOUSE_DB = os.getenv("CLICKHOUSE_DB", "football_analytics")
CLICKHOUSE_USER = os.getenv("CLICKHOUSE_USER", "default")
CLICKHOUSE_PASSWORD = os.getenv("CLICKHOUSE_PASSWORD", "clickhouse_dev")

# ---------------------------------------------------------------------------
# Initialise ClickHouse client (single shared instance)
# ---------------------------------------------------------------------------
ch_client = clickhouse_connect.get_client(
    host=CLICKHOUSE_HOST,
    port=CLICKHOUSE_HTTP_PORT,
    database=CLICKHOUSE_DB,
    username=CLICKHOUSE_USER,
    password=CLICKHOUSE_PASSWORD,
)

# ---------------------------------------------------------------------------
# Helper insert functions for each dimension / fact table
# ---------------------------------------------------------------------------
def insert_dim_competition(comp: Dict[str, Any]):
    ch_client.insert(
        "dim_competition",
        [[
            int(comp.get("competition_id")),
            comp.get("competition_name", ""),
            comp.get("country_name", ""),
            comp.get("competition_type", "LEAGUE"),
        ]],
        column_names=["competition_id", "name", "country", "type"],
    )

def insert_dim_match(match: Dict[str, Any]):
    ch_client.insert(
        "dim_match",
        [[
            int(match.get("match_id")),
            int(match.get("competition_id")),
            int(match.get("season_id")),
            int(match.get("home_team_id")),
            int(match.get("away_team_id")),
            match.get("match_date"),
            match.get("status", "SCHEDULED"),
        ]],
        column_names=["match_id", "competition_id", "season_id", "home_team_id", "away_team_id", "match_date", "status"],
    )

def insert_fact_match(match: Dict[str, Any]):
    ch_client.insert(
        "fact_match",
        [[
            int(match.get("match_id")),
            int(match.get("home_team_id")),
            int(match.get("away_team_id")),
            int(match.get("home_score", 0)),
            int(match.get("away_score", 0)),
            float(match.get("home_team_xg", 0.0)),
            float(match.get("away_team_xg", 0.0)),
            int(match.get("attendance", 0)),
            int(match.get("duration", 90)),
        ]],
        column_names=["match_id", "home_team_id", "away_team_id", "home_score", "away_score", "home_xg", "away_xg", "attendance", "duration"],
    )

def insert_fact_event(event: Dict[str, Any]):
    ch_client.insert(
        "fact_event",
        [[
            event["event_id"],
            event["match_id"],
            event["player_id"],
            event["team_id"],
            event["event_type"],
            event["minute"],
            event["second"],
            event["x"],
            event["y"],
            event.get("end_x"),
            event.get("end_y"),
            event["outcome"],
        ]],
        column_names=["event_id", "match_id", "player_id", "team_id", "event_type", "minute", "second", "x", "y", "end_x", "end_y", "outcome"],
    )

def insert_dim_player(team_players: List[Dict[str, Any]]):
    for player in team_players:
        ch_client.insert(
            "dim_player",
            [[
                int(player.get("id")),
                player.get("name", ""),
                None,  # date_of_birth not provided by raw source
                player.get("nationality", ""),
                player.get("position", ""),
                player.get("preferred_foot", ""),
            ]],
            column_names=["player_id", "name", "date_of_birth", "nationality", "position", "preferred_foot"],
        )

# ---------------------------------------------------------------------------
# Main consumer loop
# ---------------------------------------------------------------------------
def run_consumer():
    consumer_cfg = {
        "bootstrap.servers": KAFKA_BROKERS,
        "group.id": KAFKA_GROUP_ID,
        "auto.offset.reset": "earliest",
        "enable.auto.commit": False,
    }
    consumer = Consumer(consumer_cfg)
    consumer.subscribe(TOPICS)
    print("🚀 ClickHouse writer consumer started – listening to:", ", ".join(TOPICS))

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
                # payload expected to be a list of competition dicts
                for comp in payload:
                    insert_dim_competition(comp)
            elif topic == "football.raw.matches":
                for match in payload:
                    insert_dim_match(match)
                    insert_fact_match(match)
            elif topic == "football.raw.events":
                from data_platform.transformers.event_normalizer import normalize_event
                for raw_evt in payload:
                    norm = normalize_event(raw_evt, raw_evt.get("match_id", 0))
                    if norm:
                        insert_fact_event(norm)
            elif topic == "football.raw.lineups":
                # payload format: [{"team_id": int, "players": [{...}]}, ...]
                for team in payload:
                    insert_dim_player(team.get("players", []))
            else:
                print(f"⚠️ Unexpected topic: {topic}")

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
