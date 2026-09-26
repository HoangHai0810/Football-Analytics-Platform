{{ config(
    materialized='table',
    engine='MergeTree()',
    order_by=['season_id', 'player_id']
) }}

WITH aggregated_stats AS (
    SELECT
        player_id,
        season_id,
        any(team_id) AS primary_team_id,
        count(DISTINCT match_id) AS total_matches,
        sum(minutes) AS total_minutes,
        sum(goals) AS total_goals,
        sum(assists) AS total_assists,
        sum(shots) AS total_shots,
        sum(shots_on_target) AS total_shots_on_target,
        sum(passes) AS total_passes,
        sum(key_passes) AS total_key_passes,
        round(sum(xg), 2) AS total_xg,
        round(sum(xa), 2) AS total_xa,
        sum(tackles) AS total_tackles,
        sum(interceptions) AS total_interceptions,
        sum(duels) AS total_duels,
        sum(pressures) AS total_pressures,
        round(sum(goals) / nullif(sum(minutes), 0) * 90, 2) AS goals_per_90,
        round(sum(assists) / nullif(sum(minutes), 0) * 90, 2) AS assists_per_90,
        round(sum(xg) / nullif(sum(minutes), 0) * 90, 2) AS xg_per_90,
        round(sum(xa) / nullif(sum(minutes), 0) * 90, 2) AS xa_per_90,
        round(sum(shots) / nullif(sum(minutes), 0) * 90, 2) AS shots_per_90,
        round(sum(key_passes) / nullif(sum(minutes), 0) * 90, 2) AS key_passes_per_90,
        round(sum(tackles) / nullif(sum(minutes), 0) * 90, 2) AS tackles_per_90,
        round(sum(shots_on_target) / nullif(sum(shots), 0) * 100, 2) AS shot_accuracy_pct
    FROM {{ ref('stg_player_matches') }}
    GROUP BY player_id, season_id
),

dim_players AS (
    SELECT
        player_id,
        any(name) AS player_name,
        any(position) AS position,
        any(nationality) AS nationality
    FROM {{ source('football_analytics', 'dim_player') }}
    GROUP BY player_id
),

dim_teams AS (
    SELECT
        team_id,
        any(name) AS team_name
    FROM {{ source('football_analytics', 'dim_team') }}
    GROUP BY team_id
)

SELECT
    concat(toString(s.player_id), '_', toString(s.season_id)) AS player_season_key,
    s.player_id,
    s.season_id,
    coalesce(dp.player_name, 'Unknown') AS player_name,
    coalesce(dp.position, 'Unknown') AS position,
    coalesce(dp.nationality, 'Unknown') AS nationality,
    s.primary_team_id AS team_id,
    coalesce(dt.team_name, 'Unknown') AS team_name,
    s.total_matches,
    s.total_minutes,
    s.total_goals,
    s.total_assists,
    s.total_shots,
    s.total_shots_on_target,
    s.total_passes,
    s.total_key_passes,
    s.total_xg,
    s.total_xa,
    s.total_tackles,
    s.total_interceptions,
    s.total_duels,
    s.total_pressures,
    s.goals_per_90,
    s.assists_per_90,
    s.xg_per_90,
    s.xa_per_90,
    s.shots_per_90,
    s.key_passes_per_90,
    s.tackles_per_90,
    s.shot_accuracy_pct
FROM aggregated_stats s
LEFT JOIN dim_players dp ON s.player_id = dp.player_id
LEFT JOIN dim_teams dt ON s.primary_team_id = dt.team_id
