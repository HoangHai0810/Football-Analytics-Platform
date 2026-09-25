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

# ══════════════════════════════════════════
# [INF] INFRASTRUCTURE — shared services
# ══════════════════════════════════════════

infra-up:
	$(DC) up -d clickhouse postgres redis redpanda minio

infra-down:
	$(DC) stop clickhouse postgres redis redpanda minio

infra-logs:
	$(DC) logs -f clickhouse postgres redis

# ══════════════════════════════════════════
# [BE] BACKEND — Spring Boot 3.3.4
# ══════════════════════════════════════════

## Build backend Docker image
be-build:
	$(DC) build backend

## Start backend container (also starts required infrastructure)
be-up:
	$(DC) up -d backend

## Stop backend container
be-down:
	$(DC) stop backend

## Tail backend logs
be-logs:
	$(DC) logs -f backend

## Run backend locally (no Docker, needs JDK 17+)
be-dev:
	cd apps/backend && ./mvnw spring-boot:run

## Build fat JAR locally
be-package:
	cd apps/backend && ./mvnw clean package -DskipTests

# ══════════════════════════════════════════
# [FE] FRONTEND — ReactJS + Nginx
# ══════════════════════════════════════════

## Build frontend Docker image
fe-build:
	$(DC) build frontend

## Start frontend container
fe-up:
	$(DC) up -d frontend

## Stop frontend container
fe-down:
	$(DC) stop frontend

## Tail frontend (Nginx) logs
fe-logs:
	$(DC) logs -f frontend

## Run frontend locally with Vite dev server
fe-dev:
	cd apps/frontend && npm run dev

## Build frontend production bundle locally
fe-build-local:
	cd apps/frontend && npm run build

# ══════════════════════════════════════════
# [FULL] FULL STACK — all services
# ══════════════════════════════════════════

## Build all images (BE + FE)
build-all:
	$(DC) build backend frontend

## Start the full platform (infra + BE + FE)
up:
	$(DC) up -d

## Start full platform, rebuild images first
up-build:
	$(DC) up -d --build

## Stop all services
down:
	$(DC) down

## Stop all and remove volumes (⚠ destroys data)
down-clean:
	$(DC) down -v

## View all running services
ps:
	$(DC) ps

## Tail all logs
logs:
	$(DC) logs -f backend frontend

.PHONY: install-deps build-worker run-producer run-clickhouse-writer \
        run-lakehouse-writer lakehouse kestra-flow test clean \
        infra-up infra-down infra-logs \
        be-build be-up be-down be-logs be-dev be-package \
        fe-build fe-up fe-down fe-logs fe-dev fe-build-local \
        build-all up up-build down down-clean ps logs
