-- ============================================================
-- Football Analytics — PostgreSQL Analytics Schema
-- Replaces ClickHouse ReplacingMergeTree tables with standard
-- PostgreSQL tables using PRIMARY KEY + ON CONFLICT for upsert.
-- ============================================================

-- DIMENSIONS
CREATE TABLE IF NOT EXISTS dim_competition (
    competition_id  BIGINT PRIMARY KEY,
    name            TEXT NOT NULL,
    country         TEXT NOT NULL DEFAULT '',
    type            TEXT NOT NULL DEFAULT 'LEAGUE',
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS dim_season (
    season_id       BIGINT NOT NULL,
    competition_id  BIGINT NOT NULL,
    name            TEXT NOT NULL,
    start_date      DATE,
    end_date        DATE,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (competition_id, season_id)
);

CREATE TABLE IF NOT EXISTS dim_team (
    team_id     BIGINT PRIMARY KEY,
    name        TEXT NOT NULL,
    country     TEXT NOT NULL DEFAULT '',
    stadium     TEXT NOT NULL DEFAULT '',
    logo_url    TEXT NOT NULL DEFAULT '',
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS dim_player (
    player_id       BIGINT PRIMARY KEY,
    name            TEXT NOT NULL,
    date_of_birth   DATE,
    nationality     TEXT NOT NULL DEFAULT '',
    position        TEXT NOT NULL DEFAULT 'MF',
    preferred_foot  TEXT NOT NULL DEFAULT '',
    jersey_number   INTEGER NOT NULL DEFAULT 0,
    avatar_url      TEXT NOT NULL DEFAULT '',
    team_id         BIGINT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Idempotent column additions for existing deployments
ALTER TABLE dim_player ADD COLUMN IF NOT EXISTS jersey_number INTEGER NOT NULL DEFAULT 0;
ALTER TABLE dim_player ADD COLUMN IF NOT EXISTS avatar_url TEXT NOT NULL DEFAULT '';
ALTER TABLE dim_player ADD COLUMN IF NOT EXISTS team_id BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS dim_match (
    match_id        BIGINT PRIMARY KEY,
    competition_id  BIGINT NOT NULL,
    season_id       BIGINT NOT NULL,
    home_team_id    BIGINT NOT NULL,
    away_team_id    BIGINT NOT NULL,
    match_date      TIMESTAMPTZ NOT NULL,
    status          TEXT NOT NULL DEFAULT 'FINISHED',
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dim_match_season ON dim_match (season_id);
CREATE INDEX IF NOT EXISTS idx_dim_match_comp   ON dim_match (competition_id);
CREATE INDEX IF NOT EXISTS idx_dim_match_date   ON dim_match (match_date DESC);

-- FACTS
CREATE TABLE IF NOT EXISTS fact_match (
    match_id        BIGINT PRIMARY KEY,
    home_team_id    BIGINT NOT NULL,
    away_team_id    BIGINT NOT NULL,
    home_score      SMALLINT NOT NULL DEFAULT 0,
    away_score      SMALLINT NOT NULL DEFAULT 0,
    home_xg         REAL NOT NULL DEFAULT 0,
    away_xg         REAL NOT NULL DEFAULT 0,
    attendance      INTEGER NOT NULL DEFAULT 0,
    duration        SMALLINT NOT NULL DEFAULT 90,
    ingested_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fact_player_match (
    match_id            BIGINT NOT NULL,
    player_id           BIGINT NOT NULL,
    team_id             BIGINT NOT NULL DEFAULT 0,
    minutes             SMALLINT NOT NULL DEFAULT 90,
    goals               SMALLINT NOT NULL DEFAULT 0,
    assists             SMALLINT NOT NULL DEFAULT 0,
    shots               SMALLINT NOT NULL DEFAULT 0,
    shots_on_target     SMALLINT NOT NULL DEFAULT 0,
    passes              INTEGER NOT NULL DEFAULT 0,
    key_passes          SMALLINT NOT NULL DEFAULT 0,
    xg                  REAL NOT NULL DEFAULT 0,
    xa                  REAL NOT NULL DEFAULT 0,
    tackles             SMALLINT NOT NULL DEFAULT 0,
    interceptions       SMALLINT NOT NULL DEFAULT 0,
    duels               SMALLINT NOT NULL DEFAULT 0,
    pressures           INTEGER NOT NULL DEFAULT 0,
    ingested_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (player_id, match_id)
);

CREATE INDEX IF NOT EXISTS idx_fpm_match  ON fact_player_match (match_id);
CREATE INDEX IF NOT EXISTS idx_fpm_player ON fact_player_match (player_id);

CREATE TABLE IF NOT EXISTS fact_event (
    event_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id    BIGINT NOT NULL,
    player_id   BIGINT NOT NULL DEFAULT 0,
    team_id     BIGINT NOT NULL DEFAULT 0,
    event_type  TEXT NOT NULL DEFAULT '',
    minute      SMALLINT NOT NULL DEFAULT 0,
    second      SMALLINT NOT NULL DEFAULT 0,
    x           REAL NOT NULL DEFAULT 0,
    y           REAL NOT NULL DEFAULT 0,
    end_x       REAL,
    end_y       REAL,
    outcome     TEXT NOT NULL DEFAULT '',
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fact_event_match  ON fact_event (match_id);
CREATE INDEX IF NOT EXISTS idx_fact_event_player ON fact_event (player_id);
CREATE INDEX IF NOT EXISTS idx_fact_event_type   ON fact_event (event_type);

-- ANALYTICS MART
CREATE TABLE IF NOT EXISTS mart_player_season_stats (
    player_season_key       TEXT PRIMARY KEY,
    player_id               BIGINT NOT NULL,
    season_id               BIGINT NOT NULL,
    player_name             TEXT NOT NULL DEFAULT '',
    position                TEXT NOT NULL DEFAULT '',
    nationality             TEXT NOT NULL DEFAULT '',
    team_id                 BIGINT NOT NULL DEFAULT 0,
    team_name               TEXT NOT NULL DEFAULT '',
    total_matches           INTEGER NOT NULL DEFAULT 0,
    total_minutes           INTEGER NOT NULL DEFAULT 0,
    total_goals             INTEGER NOT NULL DEFAULT 0,
    total_assists           INTEGER NOT NULL DEFAULT 0,
    total_shots             INTEGER NOT NULL DEFAULT 0,
    total_shots_on_target   INTEGER NOT NULL DEFAULT 0,
    total_passes            INTEGER NOT NULL DEFAULT 0,
    total_key_passes        INTEGER NOT NULL DEFAULT 0,
    total_xg                REAL NOT NULL DEFAULT 0,
    total_xa                REAL NOT NULL DEFAULT 0,
    total_tackles           INTEGER NOT NULL DEFAULT 0,
    total_interceptions     INTEGER NOT NULL DEFAULT 0,
    total_duels             INTEGER NOT NULL DEFAULT 0,
    total_pressures         INTEGER NOT NULL DEFAULT 0,
    goals_per_90            REAL NOT NULL DEFAULT 0,
    assists_per_90          REAL NOT NULL DEFAULT 0,
    xg_per_90               REAL NOT NULL DEFAULT 0,
    xa_per_90               REAL NOT NULL DEFAULT 0,
    shots_per_90            REAL NOT NULL DEFAULT 0,
    key_passes_per_90       REAL NOT NULL DEFAULT 0,
    tackles_per_90          REAL NOT NULL DEFAULT 0,
    shot_accuracy_pct       REAL NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_mpss_player ON mart_player_season_stats (player_id);
CREATE INDEX IF NOT EXISTS idx_mpss_season ON mart_player_season_stats (season_id);
