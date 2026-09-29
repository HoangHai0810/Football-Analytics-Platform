{{ config(
    materialized='table',
    engine='MergeTree()',
    order_by=['season_id', 'team_id']
) }}

WITH aggregated_stats AS (
    SELECT
        team_id,
        season_id,
        count(DISTINCT match_id) AS matches_played,
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
        round(sum(goals) / nullif(count(DISTINCT match_id), 0), 2) AS goals_per_match,
        round(sum(xg) / nullif(count(DISTINCT match_id), 0), 2) AS xg_per_match,
        round(sum(shots) / nullif(count(DISTINCT match_id), 0), 2) AS shots_per_match,
        round(sum(shots_on_target) / nullif(sum(shots), 0) * 100, 2) AS shot_accuracy_pct
    FROM {{ ref('stg_player_matches') }}
    GROUP BY team_id, season_id
),

dim_teams AS (
    SELECT
        team_id,
        any(name) AS team_name,
        any(logo_url) AS logo_url
    FROM {{ source('football_analytics', 'dim_team') }}
    GROUP BY team_id
)

SELECT
    concat(toString(s.team_id), '_', toString(s.season_id)) AS team_season_key,
    s.team_id,
    s.season_id,
    coalesce(dt.team_name, 'Unknown') AS team_name,
    coalesce(dt.logo_url, '') AS logo_url,
    s.matches_played,
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
    s.goals_per_match,
    s.xg_per_match,
    s.shots_per_match,
    s.shot_accuracy_pct
FROM aggregated_stats s
LEFT JOIN dim_teams dt ON s.team_id = dt.team_id
