# Sprint 4 – dbt Transformations & Analytics Marts

## Overview

Sprint 4 introduces the data transformation and analytical modeling layer using **dbt-core** and **dbt-clickhouse**.
This layer transforms raw ingested event and match data in ClickHouse into optimized, pre-aggregated **Analytics Marts** designed for sub-50ms query latencies by Backend APIs and the AI Analyst layer.

---

## Architecture & Data Lineage

```
ClickHouse Raw / Facts               dbt Staging (Views)                 dbt Marts (Tables)
┌───────────────────────┐           ┌───────────────────────┐           ┌────────────────────────────┐
│ fact_event            │ ────────► │ stg_events            │           │                            │
└───────────────────────┘           └───────────────────────┘           │                            │
                                                                        │                            │
┌───────────────────────┐           ┌───────────────────────┐           │ mart_player_season_stats   │
│ fact_player_match     │ ────┬───► │ stg_player_matches    │ ────┬───► │ (Pre-aggregated Player KPI)│
└───────────────────────┘     │     └───────────────────────┘     │     └────────────────────────────┘
                              │                                   │     
┌───────────────────────┐     │                                   │     ┌────────────────────────────┐
│ dim_match             │ ────┘                                   └───► │ mart_team_season_stats     │
└───────────────────────┘                                               │ (Pre-aggregated Team KPI)  │
                                                                        └────────────────────────────┘
```

---

## Project Structure

```
dbt/
├── dbt_project.yml          # Project configuration & materialization strategies
├── profiles.yml             # ClickHouse connection profile (supports .env)
├── models/
│   ├── staging/
│   │   ├── sources.yml           # Raw ClickHouse source declarations
│   │   ├── staging_schema.yml    # Staging data quality tests (unique, not_null)
│   │   ├── stg_events.sql        # Event deduplication & pitch coordinate cleaning
│   │   └── stg_player_matches.sql# Player match deduplication & dimension enrichment
│   └── marts/
│       ├── marts_schema.yml      # Mart tests on surrogate keys and metrics
│       ├── mart_player_season_stats.sql # Player seasonal aggregations & p90 metrics
│       └── mart_team_season_stats.sql   # Team seasonal performance KPIs
```

---

## Transformations & Logic

### 1. Staging Layer (`stg_*`)
- **`stg_events`** (`view`):
  - Deduplicates events using window partitioning (`ROW_NUMBER() OVER (PARTITION BY event_id ORDER BY ingested_at DESC) = 1`).
  - Cleans and bounds pitch coordinates: `0 <= x <= 120` and `0 <= y <= 80`.
  - Normalizes nulls for `end_x` and `end_y` for passes and carries.
  - Standardizes text casing for `event_type` and `outcome`.

- **`stg_player_matches`** (`view`):
  - Deduplicates player-match records by `(player_id, match_id)`.
  - Filters out anomalous records (`minutes` between 0 and 140).
  - Enriches records with `season_id`, `competition_id`, and `match_date` by joining `dim_match`.
  - Generates surrogate key `match_player_key`.

### 2. Marts Layer (`mart_*`)
- **`mart_player_season_stats`** (`table`, `MergeTree() ORDER BY (season_id, player_id)`):
  - Aggregates seasonal totals: minutes, goals, assists, shots, shots on target, passes, key passes, xG, xA, tackles, interceptions, duels, pressures.
  - Pre-computes per-90 metrics: `goals_per_90`, `assists_per_90`, `xg_per_90`, `xa_per_90`, `shots_per_90`, `key_passes_per_90`, `tackles_per_90`.
  - Calculates accuracy rates: `shot_accuracy_pct`.
  - Joins dimensional metadata (`player_name`, `position`, `nationality`, `team_name`).

- **`mart_team_season_stats`** (`table`, `MergeTree() ORDER BY (season_id, team_id)`):
  - Aggregates team totals across matches: total goals, assists, shots, xG, xA, passes, defensive actions.
  - Pre-computes per-match metrics: `goals_per_match`, `xg_per_match`, `shots_per_match`, `shot_accuracy_pct`.
  - Joins `dim_team` to provide `team_name` and `logo_url`.

---

## Running Locally

To run the dbt pipeline against your ClickHouse instance:

```bash
# 1. Test database connection
make dbt-debug
# Or: dbt debug --project-dir dbt --profiles-dir dbt

# 2. Run transformations (materialize views and tables)
make dbt-run
# Or: dbt run --project-dir dbt --profiles-dir dbt

# 3. Run data quality tests
make dbt-test
# Or: dbt test --project-dir dbt --profiles-dir dbt

# 4. Generate & view documentation
make dbt-docs
```

---

## SLA & Query Latency

All mart tables are materialized with ClickHouse `MergeTree()` indexed on `(season_id, player_id)` and `(season_id, team_id)` respectively. This guarantees sub-50ms execution times for Backend API calls:
- `GET /api/v1/players/{id}/stats?season_id={season_id}`
- `GET /api/v1/teams/{id}/stats?season_id={season_id}`
