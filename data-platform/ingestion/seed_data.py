"""
Seed Data Script — Real Data Ingestion via PostgreSQL (psycopg2)
Loads StatsBomb open data (La Liga 2015/2016) into PostgreSQL.
Populates:
  - dim_competition, dim_season, dim_team, dim_player, dim_match
  - fact_match, fact_event, fact_player_match
  - mart_player_season_stats
"""
import os
import sys
from pathlib import Path
import uuid

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")

root_dir = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(root_dir))
sys.path.insert(0, str(root_dir / "data-platform"))

import psycopg2
import psycopg2.extras
from statsbombpy import sb

try:
    from data_platform.transformers.event_normalizer import normalize_event
except ModuleNotFoundError:
    from transformers.event_normalizer import normalize_event

DATABASE_URL = os.getenv("DATABASE_URL") or os.getenv("POSTGRES_URL") or ""
PG_HOST      = os.getenv("PG_HOST", "").strip()
PG_PORT      = int(os.getenv("PG_PORT", "5432"))
PG_DB        = os.getenv("PG_DB", "football_analytics").strip()
PG_USER      = os.getenv("PG_USER", "postgres").strip()
PG_PASSWORD  = os.getenv("PG_PASSWORD", "").strip()
PG_SSL       = os.getenv("PG_SSL", "").strip().lower()

COMPETITION_ID = 11   # La Liga
SEASON_ID = 27        # 2015/2016
MAX_MATCHES = 10      # Seed 10 matches for quick start

TEAM_LOGOS = {
    217: "https://crests.football-data.org/81.png",
    220: "https://crests.football-data.org/86.png",
    212: "https://crests.football-data.org/78.png",
    206: "https://crests.football-data.org/559.png",
    213: "https://crests.football-data.org/558.png",
}


def get_conn():
    if DATABASE_URL:
        return psycopg2.connect(DATABASE_URL, connect_timeout=15)

    # Determine SSL mode:
    # If host is remote (e.g. Render, Neon, Supabase) or PG_SSL explicitly requested, require SSL
    is_local = PG_HOST in ("localhost", "127.0.0.1", "postgres")
    sslmode = "require" if (PG_SSL in ("true", "1", "yes", "require") or not is_local) else "prefer"

    return psycopg2.connect(
        host=PG_HOST,
        port=PG_PORT,
        dbname=PG_DB,
        user=PG_USER,
        password=PG_PASSWORD,
        sslmode=sslmode,
        connect_timeout=15,
    )


def init_schema(cur):
    """Run init_analytics.sql to create all tables if they don't exist."""
    sql_path = root_dir / "infrastructure" / "postgres" / "init_analytics.sql"
    if sql_path.exists():
        print(f"📦 Verifying schema from {sql_path.name}...")
        cur.execute(sql_path.read_text(encoding="utf-8"))
        print("✅ Schema verified / initialized.")
    else:
        print(f"⚠️  Schema file not found: {sql_path}")


def seed():
    if not DATABASE_URL and not PG_HOST:
        print("=" * 60)
        print("❌ LỖI: Chưa cấu hình kết nối PostgreSQL!")
        print("   Vui lòng vào GitHub: Settings -> Secrets and variables -> Actions")
        print("   Thêm một trong 2 cách sau vào 'Repository secrets':")
        print("   👉 Cách 1 (Khuyên dùng Render/Neon/Supabase):")
        print("      - DATABASE_URL: postgresql://user:pass@host:5432/dbname")
        print("   👉 Cách 2 (Từng trường riêng lẻ):")
        print("      - PG_HOST: (vd: dpg-xxxx.singapore-postgres.render.com)")
        print("      - PG_PASSWORD: ...")
        print("      - PG_USER: postgres")
        print("      - PG_DB: football_analytics")
        print("=" * 60)
        sys.exit(1)

    if DATABASE_URL:
        print(f"📡 Connecting to PostgreSQL via DATABASE_URL...")
    else:
        print(f"📡 Connecting to PostgreSQL at {PG_HOST}:{PG_PORT} db={PG_DB}...")

    conn = get_conn()
    conn.autocommit = False
    cur = conn.cursor()
    print("✅ Connected to PostgreSQL successfully.")

    init_schema(cur)
    conn.commit()

    from datetime import date, datetime

    # 1. Competition & Season
    competitions = sb.competitions()
    la_liga = competitions[competitions["competition_id"] == COMPETITION_ID].iloc[0]

    cur.execute("""
        INSERT INTO dim_competition (competition_id, name, country, type)
        VALUES (%s, %s, %s, %s)
        ON CONFLICT (competition_id) DO UPDATE
          SET name=EXCLUDED.name, country=EXCLUDED.country, type=EXCLUDED.type, updated_at=NOW()
    """, (int(la_liga["competition_id"]), str(la_liga["competition_name"]),
          str(la_liga["country_name"]), "LEAGUE"))
    print(f"✅ Upserted competition: {la_liga['competition_name']}")

    cur.execute("""
        INSERT INTO dim_season (season_id, competition_id, name, start_date, end_date)
        VALUES (%s, %s, %s, %s, %s)
        ON CONFLICT (competition_id, season_id) DO UPDATE
          SET name=EXCLUDED.name, updated_at=NOW()
    """, (int(SEASON_ID), int(COMPETITION_ID), "2015/2016",
          date(2015, 8, 21), date(2016, 5, 15)))
    print("✅ Upserted season: 2015/2016")
    conn.commit()

    # 2. Matches
    matches_df = sb.matches(competition_id=COMPETITION_ID, season_id=SEASON_ID)
    matches_df = matches_df.head(MAX_MATCHES)

    all_player_matches = {}

    for _, m in matches_df.iterrows():
        match_id = int(m["match_id"])

        # Teams
        for team_col, name_col in [("home_team_id", "home_team"), ("away_team_id", "away_team")]:
            t_id = int(m[team_col])
            t_name = str(m[name_col])
            logo = TEAM_LOGOS.get(t_id, "https://crests.football-data.org/81.png")
            cur.execute("""
                INSERT INTO dim_team (team_id, name, country, stadium, logo_url)
                VALUES (%s, %s, %s, %s, %s)
                ON CONFLICT (team_id) DO UPDATE
                  SET name=EXCLUDED.name, updated_at=NOW()
            """, (t_id, t_name, str(la_liga["country_name"]), "Estadio " + t_name, logo))

        # dim_match
        m_date_str = str(m["match_date"])
        try:
            m_dt = datetime.strptime(m_date_str, "%Y-%m-%d")
        except Exception:
            m_dt = datetime.now()

        cur.execute("""
            INSERT INTO dim_match (match_id, competition_id, season_id, home_team_id, away_team_id, match_date, status)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (match_id) DO UPDATE
              SET status=EXCLUDED.status, updated_at=NOW()
        """, (match_id, COMPETITION_ID, SEASON_ID,
               int(m["home_team_id"]), int(m["away_team_id"]), m_dt, "FINISHED"))

        # fact_match
        home_score = int(m.get("home_score", 0))
        away_score = int(m.get("away_score", 0))
        home_xg = float(m.get("home_team_xg") or round(home_score * 0.8 + 0.4, 2))
        away_xg = float(m.get("away_team_xg") or round(away_score * 0.8 + 0.3, 2))
        cur.execute("""
            INSERT INTO fact_match (match_id, home_team_id, away_team_id, home_score, away_score, home_xg, away_xg, attendance, duration)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (match_id) DO UPDATE
              SET home_score=EXCLUDED.home_score, away_score=EXCLUDED.away_score,
                  home_xg=EXCLUDED.home_xg, away_xg=EXCLUDED.away_xg
        """, (match_id, int(m["home_team_id"]), int(m["away_team_id"]),
               home_score, away_score, home_xg, away_xg, 65400, 90))

        print(f"  ⚽ Match {match_id}: {m['home_team']} vs {m['away_team']}")

        # 3. Events + Players
        events = sb.events(match_id=match_id, split=False, flatten_attrs=False)
        event_rows = []
        player_seen = set()

        for _, ev in events.iterrows():
            raw = ev.to_dict()

            raw_pid = raw.get("player_id")
            pid = None
            pname = ""
            if raw_pid is not None and str(raw_pid) != "nan":
                try:
                    pid = int(float(raw_pid))
                    pname = str(raw.get("player", ""))
                except (ValueError, TypeError):
                    pid = None
            elif isinstance(raw.get("player"), dict):
                pid = int(raw["player"].get("id", 0))
                pname = str(raw["player"].get("name", ""))

            raw_tid = raw.get("team_id")
            if raw_tid is not None and str(raw_tid) != "nan":
                try:
                    tid = int(float(raw_tid))
                except (ValueError, TypeError):
                    tid = 0
            elif isinstance(raw.get("team"), dict):
                tid = int(raw["team"].get("id", 0))
            else:
                tid = 0

            if pid:
                if pid not in player_seen:
                    player_seen.add(pid)
                    pos = raw.get("position", "FW")
                    pos_name = pos.get("name", "FW") if isinstance(pos, dict) else str(pos or "FW")
                    pos_short = ("FW" if any(k in pos_name for k in ["Forward", "Striker", "Wing"])
                                 else ("MF" if "Midfield" in pos_name
                                       else ("DF" if "Back" in pos_name else "GK")))
                    cur.execute("""
                        INSERT INTO dim_player (player_id, name, date_of_birth, nationality, position, preferred_foot)
                        VALUES (%s, %s, %s, %s, %s, %s)
                        ON CONFLICT (player_id) DO UPDATE
                          SET name=EXCLUDED.name, position=EXCLUDED.position, updated_at=NOW()
                    """, (pid, pname, None, "Spain", pos_short, "RIGHT"))

                pm_key = (match_id, pid)
                if pm_key not in all_player_matches:
                    all_player_matches[pm_key] = {
                        "match_id": match_id, "player_id": pid, "team_id": tid,
                        "minutes": 90, "goals": 0, "assists": 0, "shots": 0, "shots_on_target": 0,
                        "passes": 0, "key_passes": 0, "xg": 0.0, "xa": 0.0,
                        "tackles": 0, "interceptions": 0, "duels": 0, "pressures": 0,
                    }

                pm = all_player_matches[pm_key]
                etype = str(raw.get("type", {})).lower() if isinstance(raw.get("type"), dict) else str(raw.get("type", "")).lower()

                if "shot" in etype:
                    pm["shots"] += 1
                    shot_dict = raw.get("shot") or {}
                    xg = float(shot_dict.get("statsbomb_xg", 0.15)) if isinstance(shot_dict, dict) else 0.15
                    pm["xg"] = round(pm["xg"] + xg, 2)
                    outcome = (shot_dict.get("outcome") or {}).get("name", "") if isinstance(shot_dict, dict) else ""
                    if outcome == "Goal":
                        pm["goals"] += 1
                        pm["shots_on_target"] += 1
                    elif outcome in ["Saved", "Post"]:
                        pm["shots_on_target"] += 1
                elif "pass" in etype:
                    pm["passes"] += 1
                    pass_dict = raw.get("pass") or {}
                    if isinstance(pass_dict, dict) and pass_dict.get("goal_assist"):
                        pm["assists"] += 1
                    if isinstance(pass_dict, dict) and pass_dict.get("shot_assist"):
                        pm["key_passes"] += 1
                        pm["xa"] = round(pm["xa"] + 0.25, 2)
                elif "duel" in etype:
                    pm["duels"] += 1
                elif "tackle" in etype:
                    pm["tackles"] += 1
                elif "interception" in etype:
                    pm["interceptions"] += 1
                elif "pressure" in etype:
                    pm["pressures"] += 1

            normalized = normalize_event(raw, match_id)
            if normalized:
                event_rows.append((
                    str(normalized["event_id"]), int(normalized["match_id"]),
                    int(normalized["player_id"]), int(normalized["team_id"]),
                    str(normalized["event_type"]), int(normalized["minute"]),
                    int(normalized["second"]),
                    float(normalized["x"]), float(normalized["y"]),
                    float(normalized["end_x"]) if normalized["end_x"] is not None else None,
                    float(normalized["end_y"]) if normalized["end_y"] is not None else None,
                    str(normalized["outcome"]),
                ))

        if event_rows:
            psycopg2.extras.execute_values(cur, """
                INSERT INTO fact_event
                  (event_id, match_id, player_id, team_id, event_type, minute, second, x, y, end_x, end_y, outcome)
                VALUES %s
                ON CONFLICT (event_id) DO NOTHING
            """, event_rows)
            print(f"    ↳ {len(event_rows)} events inserted")

        conn.commit()

    # 4. fact_player_match
    if all_player_matches:
        fpm_rows = [(
            pm["match_id"], pm["player_id"], pm["team_id"],
            pm["minutes"], pm["goals"], pm["assists"],
            pm["shots"], pm["shots_on_target"], pm["passes"],
            pm["key_passes"], pm["xg"], pm["xa"],
            pm["tackles"], pm["interceptions"], pm["duels"], pm["pressures"]
        ) for pm in all_player_matches.values()]

        psycopg2.extras.execute_values(cur, """
            INSERT INTO fact_player_match
              (match_id, player_id, team_id, minutes, goals, assists, shots, shots_on_target,
               passes, key_passes, xg, xa, tackles, interceptions, duels, pressures)
            VALUES %s
            ON CONFLICT (player_id, match_id) DO UPDATE
              SET goals=EXCLUDED.goals, assists=EXCLUDED.assists, shots=EXCLUDED.shots,
                  passes=EXCLUDED.passes, xg=EXCLUDED.xg, xa=EXCLUDED.xa
        """, fpm_rows)
        conn.commit()
        print(f"✅ Upserted {len(fpm_rows)} player match records")

    # 5. Materialize mart_player_season_stats
    print("🚀 Materializing mart_player_season_stats...")
    cur.execute("""
        INSERT INTO mart_player_season_stats (
            player_season_key, player_id, season_id, player_name, position, nationality,
            team_id, team_name, total_matches, total_minutes, total_goals, total_assists,
            total_shots, total_shots_on_target, total_passes, total_key_passes,
            total_xg, total_xa, total_tackles, total_interceptions, total_duels, total_pressures,
            goals_per_90, assists_per_90, xg_per_90, xa_per_90, shots_per_90,
            key_passes_per_90, tackles_per_90, shot_accuracy_pct
        )
        SELECT
            concat(fpm.player_id::text, '_', dm.season_id::text),
            fpm.player_id,
            dm.season_id,
            coalesce(max(dp.name), ''),
            coalesce(max(dp.position), ''),
            coalesce(max(dp.nationality), ''),
            coalesce(max(fpm.team_id), 0),
            coalesce(max(dt.name), ''),
            count(DISTINCT fpm.match_id),
            sum(fpm.minutes),
            sum(fpm.goals),
            sum(fpm.assists),
            sum(fpm.shots),
            sum(fpm.shots_on_target),
            sum(fpm.passes),
            sum(fpm.key_passes),
            round(sum(fpm.xg)::numeric, 2),
            round(sum(fpm.xa)::numeric, 2),
            sum(fpm.tackles),
            sum(fpm.interceptions),
            sum(fpm.duels),
            sum(fpm.pressures),
            round((sum(fpm.goals)::numeric / NULLIF(sum(fpm.minutes),0) * 90)::numeric, 2),
            round((sum(fpm.assists)::numeric / NULLIF(sum(fpm.minutes),0) * 90)::numeric, 2),
            round((sum(fpm.xg)::numeric / NULLIF(sum(fpm.minutes),0) * 90)::numeric, 2),
            round((sum(fpm.xa)::numeric / NULLIF(sum(fpm.minutes),0) * 90)::numeric, 2),
            round((sum(fpm.shots)::numeric / NULLIF(sum(fpm.minutes),0) * 90)::numeric, 2),
            round((sum(fpm.key_passes)::numeric / NULLIF(sum(fpm.minutes),0) * 90)::numeric, 2),
            round((sum(fpm.tackles)::numeric / NULLIF(sum(fpm.minutes),0) * 90)::numeric, 2),
            round((sum(fpm.shots_on_target)::numeric / NULLIF(sum(fpm.shots),0) * 100)::numeric, 2)
        FROM fact_player_match fpm
        JOIN dim_match dm ON fpm.match_id = dm.match_id
        LEFT JOIN dim_player dp ON fpm.player_id = dp.player_id
        LEFT JOIN dim_team dt ON fpm.team_id = dt.team_id
        GROUP BY fpm.player_id, dm.season_id
        ON CONFLICT (player_season_key) DO UPDATE
          SET total_matches=EXCLUDED.total_matches, total_goals=EXCLUDED.total_goals,
              total_assists=EXCLUDED.total_assists, total_xg=EXCLUDED.total_xg,
              goals_per_90=EXCLUDED.goals_per_90, xg_per_90=EXCLUDED.xg_per_90
    """)
    conn.commit()
    print("🎉 PostgreSQL Data Ingestion & Mart Materialization Complete!")

    cur.close()
    conn.close()


if __name__ == "__main__":
    seed()


