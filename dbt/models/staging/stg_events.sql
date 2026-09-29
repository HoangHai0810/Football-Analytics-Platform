{{ config(
    materialized='view'
) }}

WITH raw_events AS (
    SELECT
        event_id,
        match_id,
        player_id,
        team_id,
        event_type,
        minute,
        second,
        x,
        y,
        end_x,
        end_y,
        outcome,
        ingested_at,
        ROW_NUMBER() OVER (
            PARTITION BY event_id 
            ORDER BY ingested_at DESC
        ) AS rn
    FROM {{ source('football_analytics', 'fact_event') }}
)

SELECT
    event_id,
    match_id,
    player_id,
    team_id,
    upper(trim(event_type)) AS event_type,
    minute,
    second,
    x,
    y,
    CASE 
        WHEN end_x >= 0 AND end_x <= 120 THEN end_x 
        ELSE NULL 
    END AS end_x,
    CASE 
        WHEN end_y >= 0 AND end_y <= 80 THEN end_y 
        ELSE NULL 
    END AS end_y,
    upper(trim(outcome)) AS outcome,
    ingested_at
FROM raw_events
WHERE rn = 1
  AND x >= 0 AND x <= 120
  AND y >= 0 AND y <= 80
