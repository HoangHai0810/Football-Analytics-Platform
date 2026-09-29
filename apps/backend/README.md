# ⚽ Football Analytics Platform — Spring Boot Backend API

## Overview
High-performance REST API module built with **Spring Boot 3.3.4** and **Java 17+**, designed for the Football Analytics Platform. It implements a dual-mode data layer:
1. **ClickHouse OLAP Layer**: Connects to ClickHouse on port `8123` / `9000` via JDBC to run analytical queries over dimension tables (`dim_*`), fact tables (`fact_*`), and dbt analytical marts (`mart_player_season_stats`).
2. **High-Fidelity Resilient Seed Store**: Provides realistic, complete data matching `data_contract.md` even when the Data Engineering (DE) pipeline is in progress.

## Prerequisites
- **JDK 17+** (e.g. OpenJDK 17 or 21)
- **Maven 3.9+** (or use the provided `mvnw` wrapper)

## Running the Backend

```bash
# From apps/backend
./mvnw clean spring-boot:run

# Or with Maven directly:
mvn clean spring-boot:run
```

API will start on port `8000`:
- Base URL: `http://localhost:8000`
- Health check: `http://localhost:8000/health`
- System Status: `http://localhost:8000/api/v1/system/status`

## RESTful Endpoints Inventory

All responses conform to the standard API response envelope:
```json
{
  "data": { ... },
  "meta": {
    "execution_time_ms": 12.4,
    "cached": false,
    "version": "v1"
  }
}
```

### Competitions & Matches
- `GET /api/v1/competitions`: List all competitions (Premier League, Champions League, La Liga)
- `GET /api/v1/competitions/{id}`: Competition details
- `GET /api/v1/competitions/{id}/seasons`: Seasons list
- `GET /api/v1/matches`: Matches list with filters (`competition_id`, `season_id`, `status`)
- `GET /api/v1/matches/{id}`: Detailed match overview

### Players & Analytics
- `GET /api/v1/players`: List players with search query and position filters
- `GET /api/v1/players/{id}`: Player profile
- `GET /api/v1/players/{id}/stats?season_id=2024`: Player season stats (Goals, Assists, xG, xA, per-90 metrics, radar ratings)
- `GET /api/v1/players/{id}/shots?season_id=2024`: Pitch shot map events with (x,y) coordinates in standard [0..120]x[0..80] grid
- `GET /api/v1/players/compare?ids=1024,1088&season_id=2024`: Head-to-head comparison and delta calculations

### AI Football Analyst
- `POST /api/v1/ai/analyze`: Ask AI Analyst a natural language question with strict zero-hallucination data citations.
- `GET /api/v1/ai/prompts`: Recommended query templates.
