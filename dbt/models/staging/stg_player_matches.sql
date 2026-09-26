{{ config(
    materialized='view'
) }}

WITH raw_player_matches AS (
    SELECT
        match_id,
        player_id,
        team_id,
        minutes,
        goals,
        assists,
        shots,
        shots_on_target,
        passes,
        key_passes,
        xg,
        xa,
        tackles,
        interceptions,
        duels,
        pressures,
        ingested_at,
        ROW_NUMBER() OVER (
            PARTITION BY player_id, match_id 
            ORDER BY ingested_at DESC
        ) AS rn
    FROM {{ source('football_analytics', 'fact_player_match') }}
),

deduped_player_matches AS (
    SELECT
        match_id,
        player_id,
        team_id,
        minutes,
        goals,
        assists,
        shots,
        shots_on_target,
        passes,
        key_passes,
        xg,
        xa,
        tackles,
        interceptions,
        duels,
        pressures,
        ingested_at
    FROM raw_player_matches
    WHERE rn = 1
      AND minutes >= 0
      AND minutes <= 140
)

SELECT
    concat(toString(dpm.player_id), '_', toString(dpm.match_id)) AS match_player_key,
    dpm.match_id,
    dpm.player_id,
    dpm.team_id,
    coalesce(dm.competition_id, 0) AS competition_id,
    coalesce(dm.season_id, 0) AS season_id,
    dm.match_date,
    dpm.minutes,
    dpm.goals,
    dpm.assists,
    dpm.shots,
    dpm.shots_on_target,
    dpm.passes,
    dpm.key_passes,
    dpm.xg,
    dpm.xa,
    dpm.tackles,
    dpm.interceptions,
    dpm.duels,
    dpm.pressures,
    dpm.ingested_at
FROM deduped_player_matches dpm
LEFT JOIN {{ source('football_analytics', 'dim_match') }} dm
    ON dpm.match_id = dm.match_id
