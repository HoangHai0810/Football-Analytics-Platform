"""
Seed Data Script — Sprint 1
Loads StatsBomb open data directly into ClickHouse.
Default: La Liga 2015/2016 (competition_id=11, season_id=27)
"""
import clickhouse_connect
from statsbombpy import sb
from data_platform.transformers.event_normalizer import normalize_event

CLICKHOUSE_HOST = "localhost"
CLICKHOUSE_PORT = 8123
CLICKHOUSE_DB = "football_analytics"
CLICKHOUSE_USER = "default"
CLICKHOUSE_PASSWORD = "clickhouse_dev"

COMPETITION_ID = 11   # La Liga
SEASON_ID = 27        # 2015/2016
MAX_MATCHES = 10      # Seed 10 matches for quick start

def seed():
    client = clickhouse_connect.get_client(
        host=CLICKHOUSE_HOST,
        port=CLICKHOUSE_PORT,
        database=CLICKHOUSE_DB,
        username=CLICKHOUSE_USER,
        password=CLICKHOUSE_PASSWORD,
    )
    print("✅ Connected to ClickHouse")

    # 1. Competitions
    competitions = sb.competitions()
    la_liga = competitions[competitions["competition_id"] == COMPETITION_ID].iloc[0]
    client.insert("dim_competition", [[
        int(la_liga["competition_id"]),
        la_liga["competition_name"],
        la_liga["country_name"],
        "LEAGUE",
    ]], column_names=["competition_id", "name", "country", "type"])
    print(f"✅ Inserted competition: {la_liga['competition_name']}")

    # 2. Matches
    matches_df = sb.matches(competition_id=COMPETITION_ID, season_id=SEASON_ID)
    matches_df = matches_df.head(MAX_MATCHES)

    for _, m in matches_df.iterrows():
        # Insert teams
        for team_col in [("home_team_id", "home_team"), ("away_team_id", "away_team")]:
            t_id = int(m[team_col[0]])
            t_name = m[team_col[1]]
            client.insert("dim_team",
                [[t_id, t_name, la_liga["country_name"], "", ""]],
                column_names=["team_id", "name", "country", "stadium", "logo_url"])

        # Insert match dim
        client.insert("dim_match", [[
            int(m["match_id"]), COMPETITION_ID, SEASON_ID,
            int(m["home_team_id"]), int(m["away_team_id"]),
            m["match_date"], "FINISHED",
        ]], column_names=["match_id","competition_id","season_id","home_team_id","away_team_id","match_date","status"])

        # Insert fact_match (basic scores, xG if available)
        client.insert("fact_match", [[
            int(m["match_id"]), int(m["home_team_id"]), int(m["away_team_id"]),
            int(m["home_score"]), int(m["away_score"]),
            float(m.get("home_team_xg", 0)), float(m.get("away_team_xg", 0)),
            0, 90,
        ]], column_names=["match_id","home_team_id","away_team_id","home_score","away_score","home_xg","away_xg","attendance","duration"])

        print(f"  ✅ Match {m['match_id']}: {m['home_team']} vs {m['away_team']}")

        # 3. Events + Players
        events = sb.events(match_id=int(m["match_id"]), split=False, flatten_attrs=False)
        event_rows = []
        player_seen = set()

        for _, ev in events.iterrows():
            raw = ev.to_dict()

            # Insert player dims if not already
            player = raw.get("player", {})
            if isinstance(player, dict):
                pid = player.get("id")
                if pid and pid not in player_seen:
                    player_seen.add(pid)
                    client.insert("dim_player",
                        [[int(pid), player.get("name", ""), None, "", "FW", "RIGHT"]],
                        column_names=["player_id","name","date_of_birth","nationality","position","preferred_foot"])

            normalized = normalize_event(raw, int(m["match_id"]))
            if normalized:
                event_rows.append([
                    normalized["event_id"], normalized["match_id"],
                    normalized["player_id"], normalized["team_id"],
                    normalized["event_type"], normalized["minute"],
                    normalized["second"], normalized["x"], normalized["y"],
                    normalized["end_x"], normalized["end_y"], normalized["outcome"],
                ])

        if event_rows:
            client.insert("fact_event", event_rows,
                column_names=["event_id","match_id","player_id","team_id",
                               "event_type","minute","second","x","y","end_x","end_y","outcome"])
            print(f"    ↳ {len(event_rows)} events inserted")

    print("\n🎉 Seed Data complete! ClickHouse is ready for BE.")

if __name__ == "__main__":
    seed()
