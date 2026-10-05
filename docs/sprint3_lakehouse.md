# Sprint 3 – Lakehouse Writer

## Overview

The **Lakehouse Writer** is a consumer that reads raw football data from Kafka topics, validates the payload using the shared validator utilities, converts the records to a Pandas `DataFrame`, writes the data to a Parquet file in‑memory and uploads the file to MinIO. This provides an immutable, query‑able lakehouse layer alongside the PostgreSQL / ClickHouse analytical store.

## Architecture

```
Kafka (raw topics) → Lakehouse Writer → MinIO (Parquet objects)
```

- **Kafka topics**: `football.raw.competitions`, `football.raw.matches`, `football.raw.events`, `football.raw.lineups`.
- **Validation**: `data_platform.validators.kafka_payload_validator` ensures schema correctness before persisting.
- **Parquet conversion**: `pandas.json_normalize` ⇒ `DataFrame` ⇒ `to_parquet` (in‑memory).
- **Object storage**: MinIO bucket `football-lake` (configurable via `MINIO_BUCKET`).

## Deployment

The consumer runs as a Kestra task (`write_lakehouse`) defined in `infrastructure/kestra/flows/ingestion_flow.yaml`. The Docker image for the worker is built with `make build-worker` (see `infrastructure/docker/Dockerfile.worker`).

## Running locally

```bash
# Install DE dependencies
make install-deps

# Start a local MinIO instance (docker-compose provides it)
docker compose up -d minio

# Run the consumer (will block and consume messages)
python data-platform/consumers/lakehouse_writer.py
```

## Testing

Unit tests live under `data-platform/tests/`. They verify:
- Payload validation functions return the expected dictionaries.
- The lakehouse writer’s processing helpers (`process_competitions`, `process_matches`, etc.) correctly produce DataFrames.

Run tests with:
```bash
make test
```

## Notes

- The writer is **lightweight** – it does not implement DLQ handling or advanced schema evolution; those are planned for future sprints.
- All environment variables (`KAFKA_BROKERS`, `MINIO_*`) are read at runtime and have sensible defaults for local development.
