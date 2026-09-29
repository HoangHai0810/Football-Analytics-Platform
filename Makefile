# Makefile for Football Analytics Platform
# ──────────────────────────────────────────────────────────────
# Environment variables (can be overridden)
PYTHON ?= python
PIP    ?= pip
DC     := docker-compose

# ══════════════════════════════════════════
# [DE] DATA ENGINEERING — do NOT modify
# ══════════════════════════════════════════

install-deps:
	$(PIP) install -r requirements-de.txt

build-worker:
	docker build -t football/worker:dev -f infrastructure/docker/Dockerfile.worker .

run-producer:
	$(PYTHON) data-platform/ingestion/producers.py

run-live-producer:
	$(PYTHON) data-platform/ingestion/live_matches_producer.py

run-live-poll:
	$(PYTHON) data-platform/ingestion/live_matches_producer.py --poll --interval 60

# ---------- Consumers ----------
run-clickhouse-writer:
	$(PYTHON) data-platform/consumers/clickhouse_writer.py

run-lakehouse-writer:
	$(PYTHON) data-platform/consumers/lakehouse_writer.py

lakehouse:
	$(PYTHON) data-platform/consumers/lakehouse_writer.py

kestra-flow:
	@echo "Run Kestra flow with: kestra flow run ingestion_flow.yaml"

test:
	$(PYTHON) -m pytest data-platform/tests

clean:
	rm -rf __pycache__ .pytest_cache

kestra-flow:
	@echo "Run Kestra flow with: kestra flow run ingestion_flow.yaml"

# ---------- dbt (Sprint 4) ----------
DBT ?= dbt

dbt-debug:
	$(DBT) debug --project-dir dbt --profiles-dir dbt

dbt-run:
	$(DBT) run --project-dir dbt --profiles-dir dbt

dbt-test:
	$(DBT) test --project-dir dbt --profiles-dir dbt

dbt-docs:
	$(DBT) docs generate --project-dir dbt --profiles-dir dbt
