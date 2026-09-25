-- =============================
-- DIMENSIONS
-- =============================
CREATE TABLE IF NOT EXISTS dim_competition (
    competition_id UInt32,
    name           String,
    country        String,
    type           LowCardinality(String),
    updated_at     DateTime DEFAULT now()
) ENGINE = ReplacingMergeTree(updated_at)
ORDER BY (competition_id);

CREATE TABLE IF NOT EXISTS dim_season (
    season_id      UInt32,
    competition_id UInt32,
    name           String,
    start_date     Date,
    end_date       Date,
    updated_at     DateTime DEFAULT now()
) ENGINE = ReplacingMergeTree(updated_at)
ORDER BY (competition_id, season_id);

CREATE TABLE IF NOT EXISTS dim_team (
    team_id    UInt32,
    name       String,
    country    String,
    stadium    String,
    logo_url   String,
    updated_at DateTime DEFAULT now()
) ENGINE = ReplacingMergeTree(updated_at)
ORDER BY (team_id);

CREATE TABLE IF NOT EXISTS dim_player (
    player_id      UInt32,
    name           String,
    date_of_birth  Nullable(Date),
    nationality    String,
    position       LowCardinality(String),
    preferred_foot LowCardinality(String),
    updated_at     DateTime DEFAULT now()
) ENGINE = ReplacingMergeTree(updated_at)
ORDER BY (player_id);

CREATE TABLE IF NOT EXISTS dim_match (
    match_id       UInt64,
    competition_id UInt32,
    season_id      UInt32,
    home_team_id   UInt32,
    away_team_id   UInt32,
    match_date     DateTime,
    status         LowCardinality(String),
    updated_at     DateTime DEFAULT now()
) ENGINE = ReplacingMergeTree(updated_at)
ORDER BY (season_id, match_date, match_id);

-- =============================
-- FACTS
-- =============================
CREATE TABLE IF NOT EXISTS fact_match (
    match_id     UInt64,
    home_team_id UInt32,
    away_team_id UInt32,
    home_score   UInt8,
    away_score   UInt8,
    home_xg      Float32,
    away_xg      Float32,
    attendance   UInt32,
    duration     UInt16,
    ingested_at  DateTime DEFAULT now()
) ENGINE = ReplacingMergeTree(ingested_at)
ORDER BY (match_id);

CREATE TABLE IF NOT EXISTS fact_player_match (
    match_id         UInt64,
    player_id        UInt32,
    team_id          UInt32,
    minutes          UInt8,
    goals            UInt8,
    assists          UInt8,
    shots            UInt8,
    shots_on_target  UInt8,
    passes           UInt16,
    key_passes       UInt8,
    xg               Float32,
    xa               Float32,
    tackles          UInt8,
    interceptions    UInt8,
    duels            UInt8,
    pressures        UInt16,
    ingested_at      DateTime DEFAULT now()
) ENGINE = ReplacingMergeTree(ingested_at)
ORDER BY (player_id, match_id);

CREATE TABLE IF NOT EXISTS fact_event (
    event_id    UUID,
    match_id    UInt64,
    player_id   UInt32,
    team_id     UInt32,
    event_type  LowCardinality(String),
    minute      UInt8,
    second      UInt8,
    x           Float32,
    y           Float32,
    end_x       Nullable(Float32),
    end_y       Nullable(Float32),
    outcome     LowCardinality(String),
    ingested_at DateTime DEFAULT now()
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(ingested_at)
ORDER BY (match_id, event_type, minute, second);
