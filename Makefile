# Makefile for Football Analytics Platform
# ──────────────────────────────────────────────────────────────
# Environment variables (can be overridden)
PYTHON ?= python
PIP    ?= pip
DC     := docker-compose

# ══════════════════════════════════════════
# FULL STACK — Quick Start (Local)
# ══════════════════════════════════════════

## Start full local stack (DE infra + BE + FE)
up:
	$(DC) up -d --build

## Start DE infrastructure only (ClickHouse, Redpanda, MinIO, Kestra, Postgres, Redis)
up-infra:
	$(DC) up -d clickhouse redpanda minio postgres redis kestra

## Start BE + FE only (assumes infra is already running)
up-app:
	$(DC) up -d --build backend frontend

## Stop all containers
down:
	$(DC) down

## Stop and remove all data volumes (DANGER: destroys all local data)
down-clean:
	$(DC) down -v

## Show live logs from all containers
logs:
	$(DC) logs -f

## Show logs for a specific service, e.g.: make logs-svc SVC=backend
logs-svc:
	$(DC) logs -f $(SVC)

## Check container statuses
status:
	$(DC) ps

## Wait for ClickHouse to be healthy then run seed data (StatsBomb real data)
seed:
	@echo "⏳ Waiting for ClickHouse to be ready..."
	@timeout 60 sh -c 'until $(DC) exec -T clickhouse clickhouse-client --query="SELECT 1" 2>/dev/null; do sleep 3; done' || echo "ClickHouse not ready, proceeding anyway..."
	$(PYTHON) data-platform/ingestion/seed_data.py

## Seed data on Windows (no sh/timeout)
seed-win:
	$(PYTHON) data-platform/ingestion/seed_data.py

## Run dbt transformations after seed
seed-and-dbt: seed dbt-run dbt-test

## One-shot: bring up infra + seed real data + run dbt
bootstrap: up-infra seed dbt-run
	@echo "✅ Bootstrap complete! Run 'make up-app' to start BE+FE."

## Run FE in local dev mode (Vite hot reload)
fe-dev:
	cd apps/frontend && npm install && npm run dev

## Check which ports are already in use on local machine (Windows)
check-ports:
	netstat -ano | findstr ":8123 :9092 :9000 :8080 :8081 :5432 :6379 :3000 :8000"

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

kestra-upload-flow:
	@echo "Upload flow to Kestra:"
	curl.exe -X POST http://localhost:8081/api/v1/flows/import \
	  -H "Content-Type: multipart/form-data" \
	  -F "fileUpload=@infrastructure/kestra/flows/ingestion_flow.yaml"

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
