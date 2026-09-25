# Makefile for Football Analytics Platform

# Environment variables (can be overridden)
PYTHON ?= python
PIP ?= pip

# ---------- Dependencies ----------
install-deps:
	$(PIP) install -r requirements-de.txt

# ---------- Docker ----------
build-worker:
	docker build -t football/worker:dev -f infrastructure/docker/Dockerfile.worker .

# ---------- Kafka producers ----------
run-producer:
	$(PYTHON) data-platform/ingestion/producers.py

# ---------- Consumers ----------
run-clickhouse-writer:
	$(PYTHON) data-platform/consumers/clickhouse_writer.py

run-lakehouse-writer:
	$(PYTHON) data-platform/consumers/lakehouse_writer.py

# ---------- Test ----------
test:
	$(PYTHON) -m pytest data-platform/tests

# ---------- Utility ----------
clean:
	rm -rf __pycache__ .pytest_cache
# ---------- Sprint 3 targets ----------
lakehouse:
	$(PYTHON) data-platform/consumers/lakehouse_writer.py

kestra-flow:
	@echo "Run Kestra flow with: kestra flow run ingestion_flow.yaml"
