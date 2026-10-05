"""
Seed Data Script — Real Data Ingestion via PostgreSQL (psycopg2)
Loads ALL StatsBomb open-data competitions from 2015 onwards into PostgreSQL.
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

DATABASE_URL = "".join((os.getenv("DATABASE_URL") or os.getenv("POSTGRES_URL") or "").strip().strip("'\"").split())
PG_HOST      = "".join(os.getenv("PG_HOST", "").strip().strip("'\"").split())
PG_PORT_RAW  = "".join(os.getenv("PG_PORT", "5432").strip().split())
PG_PORT      = int(PG_PORT_RAW) if PG_PORT_RAW.isdigit() else 5432
PG_DB        = "".join(os.getenv("PG_DB", "football_analytics").strip().strip("'\"").split())
PG_USER      = "".join(os.getenv("PG_USER", "postgres").strip().strip("'\"").split())
PG_PASSWORD  = os.getenv("PG_PASSWORD", "").strip()
PG_SSL       = "".join(os.getenv("PG_SSL", "").strip().lower().split())

# Max matches to seed per competition-season để giữ runtime hợp lý.
# Tăng lên nếu muốn nhiều dữ liệu hơn (vd: 38 cho full season).
MAX_MATCHES_PER_SEASON = int(os.getenv("MAX_MATCHES_PER_SEASON", "10"))
MIN_SEASON_YEAR = int(os.getenv("MIN_SEASON_YEAR", "2015"))

TEAM_LOGOS = {
    217: "https://crests.football-data.org/81.png",
    220: "https://crests.football-data.org/86.png",
    212: "https://crests.football-data.org/78.png",
    206: "https://crests.football-data.org/559.png",
    213: "https://crests.football-data.org/558.png",
}


def build_catalog():
    """
    Tự động lấy toàn bộ competition từ StatsBomb open-data API,
    lọc lấy các season từ MIN_SEASON_YEAR trở đi.
    Trả về list of tuples: (competition_id, season_id, competition_name, season_name, comp_type)
    """
    import re
    print(f"📡 Fetching StatsBomb competition catalog (seasons >= {MIN_SEASON_YEAR})...")
    all_comps = sb.competitions()

    # Xác định loại giải dựa trên tên competition
    def infer_type(comp_name: str, gender: str) -> str:
        name_lower = comp_name.lower()
        if any(k in name_lower for k in ["world cup", "euro", "copa america", "afcon",
                                          "african cup", "nations league", "olympics"]):
            return "INTERNATIONAL"
        if any(k in name_lower for k in ["champions league", "europa league", "fa cup",
                                          "copa del rey", "dfb-pokal"]):
            return "CUP"
        return "LEAGUE"

    # Trích năm từ tên season: "2015/2016" → 2015, "2023" → 2023
    def extract_year(season_name: str) -> int:
        years = re.findall(r"\d{4}", str(season_name))
        return int(years[0]) if years else 0

    catalog = []
    for _, row in all_comps.iterrows():
        year = extract_year(row["season_name"])
        if year < MIN_SEASON_YEAR:
            continue
        comp_type = infer_type(str(row["competition_name"]), str(row.get("competition_gender", "")))
        catalog.append((
            int(row["competition_id"]),
            int(row["season_id"]),
            str(row["competition_name"]),
            str(row["season_name"]),
            comp_type,
        ))

    # Sắp xếp: theo tên giải rồi năm
    catalog.sort(key=lambda x: (x[2], x[3]))
    print(f"✅ Found {len(catalog)} competition-seasons từ {MIN_SEASON_YEAR} trở đi.")
    return catalog




def get_conn():
    if DATABASE_URL:
        url = DATABASE_URL
        # Parse host from URL to determine if it's remote
        # Format: postgresql://user:pass@host:port/db
        try:
            from urllib.parse import urlparse
            parsed = urlparse(url)
            host_part = parsed.hostname or ""
        except Exception:
            host_part = ""
        is_local_url = host_part in ("localhost", "127.0.0.1", "postgres", "")
        # Add sslmode=require for all remote hosts (not just specific domains)
        if "sslmode=" not in url and not is_local_url:
            sep = "&" if "?" in url else "?"
            url = f"{url}{sep}sslmode=require"
        print(f"🔐 SSL mode in URL: {'sslmode=require' if 'sslmode=require' in url else 'not set'}")
        print(f"🌐 Connecting to host: {host_part}")
        return psycopg2.connect(url, connect_timeout=30)

    # Determine SSL mode:
    # If host is remote or PG_SSL explicitly requested, require SSL
    is_local = PG_HOST in ("localhost", "127.0.0.1", "postgres")
    sslmode = "require" if (PG_SSL in ("true", "1", "yes", "require") or not is_local) else "prefer"
    print(f"🔐 SSL mode: {sslmode}")
    print(f"🌐 Connecting to host: {PG_HOST}:{PG_PORT} db={PG_DB}")

    return psycopg2.connect(
        host=PG_HOST,
        port=PG_PORT,
        dbname=PG_DB,
        user=PG_USER,
        password=PG_PASSWORD,
        sslmode=sslmode,
        connect_timeout=30,
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


def seed_season(cur, conn, comp_id, season_id, comp_name, season_name, comp_type):
    """Seed one competition-season: teams, matches, events, player match stats."""
    from datetime import datetime

    print(f"\n{'='*60}")
    print(f"📋 {comp_name} — {season_name}  (comp={comp_id}, season={season_id})")
    print(f"{'='*60}")

    try:
        matches_df = sb.matches(competition_id=comp_id, season_id=season_id)
    except Exception as e:
        print(f"  ⚠️  Could not fetch matches: {e}")
        return 0

    matches_df = matches_df.head(MAX_MATCHES_PER_SEASON)
    if matches_df.empty:
        print("  ⚠️  No matches found, skipping.")
        return 0

    all_player_matches = {}

    for _, m in matches_df.iterrows():
        match_id = int(m["match_id"])

        # Teams
        import urllib.parse
        for team_col, name_col in [("home_team_id", "home_team"), ("away_team_id", "away_team")]:
            t_id = int(m[team_col])
            t_name = str(m[name_col])
            logo = TEAM_LOGOS.get(t_id, f"https://ui-avatars.com/api/?name={urllib.parse.quote(t_name)}&background=0f172a&color=38bdf8&bold=true")
            cur.execute("""
                INSERT INTO dim_team (team_id, name, country, stadium, logo_url)
                VALUES (%s, %s, %s, %s, %s)
                ON CONFLICT (team_id) DO UPDATE
                  SET name=EXCLUDED.name, logo_url=EXCLUDED.logo_url, updated_at=NOW()
            """, (t_id, t_name, comp_name, "Stadium of " + t_name, logo))

        # dim_match
        m_date_str = str(m.get("match_date", ""))
        try:
            m_dt = datetime.strptime(m_date_str, "%Y-%m-%d")
        except Exception:
            m_dt = datetime.now()

        cur.execute("""
            INSERT INTO dim_match (match_id, competition_id, season_id, home_team_id, away_team_id, match_date, status)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (match_id) DO UPDATE
              SET status=EXCLUDED.status, updated_at=NOW()
        """, (match_id, comp_id, season_id,
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

        # Check if match events already ingested to avoid re-downloading
        cur.execute("SELECT 1 FROM fact_player_match WHERE match_id = %s LIMIT 1", (match_id,))
        if cur.fetchone():
            print(f"  ⏩ Match {match_id}: {m['home_team']} vs {m['away_team']} (already ingested, skipping)")
            continue

        print(f"  ⚽ Match {match_id}: {m['home_team']} vs {m['away_team']}")

        # Fetch real player profiles from lineups (accurate nationality, jersey number, position, avatar)
        try:
            lineups = sb.lineups(match_id=match_id)
            for _, df_lineup in lineups.items():
                for _, p_row in df_lineup.iterrows():
                    pid = int(p_row["player_id"])
                    pname = str(p_row.get("player_name") or p_row.get("player_nickname") or "").strip()
                    p_country = str(p_row.get("country") or "International").strip()
                    p_jersey = int(p_row.get("jersey_number") or 10)

                    positions_list = p_row.get("positions")
                    pos_short = "FW"
                    if isinstance(positions_list, list) and len(positions_list) > 0:
                        pos_name = str(positions_list[0].get("position", "Forward"))
                        if any(k in pos_name for k in ["Forward", "Striker", "Wing"]):
                            pos_short = "FW"
                        elif any(k in pos_name for k in ["Midfield"]):
                            pos_short = "MF"
                        elif any(k in pos_name for k in ["Back", "Defender"]):
                            pos_short = "DF"
                        elif "Goalkeeper" in pos_name:
                            pos_short = "GK"

                    avatar = f"https://ui-avatars.com/api/?name={urllib.parse.quote(pname)}&background=0f172a&color=38bdf8&bold=true&size=128"

                    cur.execute("""
                        INSERT INTO dim_player (player_id, name, date_of_birth, nationality, position, preferred_foot, jersey_number, avatar_url)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                        ON CONFLICT (player_id) DO UPDATE
                          SET name=EXCLUDED.name, nationality=EXCLUDED.nationality,
                              position=EXCLUDED.position, jersey_number=EXCLUDED.jersey_number,
                              avatar_url=EXCLUDED.avatar_url, updated_at=NOW()
                    """, (pid, pname, None, p_country, pos_short, "RIGHT", p_jersey, avatar))
            conn.commit()
        except Exception as e:
            print(f"    ⚠️ Could not fetch lineups for match {match_id}: {e}")

        # Events + Players
        try:
            events = sb.events(match_id=match_id, split=False, flatten_attrs=False)
        except Exception as e:
            print(f"    ⚠️  Could not fetch events: {e}")
            conn.commit()
            continue

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
                    player_nat = "International"
                    if comp_type == "INTERNATIONAL":
                        t_name = str(m.get("home_team" if tid == int(m.get("home_team_id", 0)) else "away_team", "")).replace(" Women's", "").strip()
                        player_nat = t_name if t_name else "International"
                    else:
                        c_lower = comp_name.lower()
                        if "spain" in c_lower or "la liga" in c_lower:
                            player_nat = "Spain"
                        elif "france" in c_lower or "ligue" in c_lower:
                            player_nat = "France"
                        elif "england" in c_lower or "premier" in c_lower:
                            player_nat = "England"
                        elif "germany" in c_lower or "bundesliga" in c_lower:
                            player_nat = "Germany"
                        elif "italy" in c_lower or "serie" in c_lower:
                            player_nat = "Italy"
                        else:
                            player_nat = str(m.get("country_name") or "International")

                    cur.execute("""
                        INSERT INTO dim_player (player_id, name, date_of_birth, nationality, position, preferred_foot)
                        VALUES (%s, %s, %s, %s, %s, %s)
                        ON CONFLICT (player_id) DO UPDATE
                          SET name=EXCLUDED.name, nationality=EXCLUDED.nationality, position=EXCLUDED.position, updated_at=NOW()
                    """, (pid, pname, None, player_nat, pos_short, "RIGHT"))

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

    # fact_player_match for this season
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
        print(f"  ✅ {len(fpm_rows)} player-match records upserted")

    return len(matches_df)


def materialize_mart(cur, conn):
    """Rebuild mart_player_season_stats from all fact_player_match data."""
    print("\n🔄 Materializing mart_player_season_stats (all competitions)...")
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
            coalesce(sum(fpm.minutes), 0),
            coalesce(sum(fpm.goals), 0),
            coalesce(sum(fpm.assists), 0),
            coalesce(sum(fpm.shots), 0),
            coalesce(sum(fpm.shots_on_target), 0),
            coalesce(sum(fpm.passes), 0),
            coalesce(sum(fpm.key_passes), 0),
            coalesce(round(sum(fpm.xg)::numeric, 2), 0),
            coalesce(round(sum(fpm.xa)::numeric, 2), 0),
            coalesce(sum(fpm.tackles), 0),
            coalesce(sum(fpm.interceptions), 0),
            coalesce(sum(fpm.duels), 0),
            coalesce(sum(fpm.pressures), 0),
            coalesce(round((sum(fpm.goals)::numeric   / NULLIF(sum(fpm.minutes),0) * 90)::numeric, 2), 0),
            coalesce(round((sum(fpm.assists)::numeric / NULLIF(sum(fpm.minutes),0) * 90)::numeric, 2), 0),
            coalesce(round((sum(fpm.xg)::numeric      / NULLIF(sum(fpm.minutes),0) * 90)::numeric, 2), 0),
            coalesce(round((sum(fpm.xa)::numeric      / NULLIF(sum(fpm.minutes),0) * 90)::numeric, 2), 0),
            coalesce(round((sum(fpm.shots)::numeric   / NULLIF(sum(fpm.minutes),0) * 90)::numeric, 2), 0),
            coalesce(round((sum(fpm.key_passes)::numeric / NULLIF(sum(fpm.minutes),0) * 90)::numeric, 2), 0),
            coalesce(round((sum(fpm.tackles)::numeric / NULLIF(sum(fpm.minutes),0) * 90)::numeric, 2), 0),
            coalesce(round((sum(fpm.shots_on_target)::numeric / NULLIF(sum(fpm.shots),0) * 100)::numeric, 2), 0)
        FROM fact_player_match fpm
        JOIN dim_match dm ON fpm.match_id = dm.match_id
        LEFT JOIN dim_player dp ON fpm.player_id = dp.player_id
        LEFT JOIN dim_team dt ON fpm.team_id = dt.team_id
        GROUP BY fpm.player_id, dm.season_id
        ON CONFLICT (player_season_key) DO UPDATE
          SET total_matches=EXCLUDED.total_matches,      total_goals=EXCLUDED.total_goals,
              total_assists=EXCLUDED.total_assists,      total_xg=EXCLUDED.total_xg,
              total_shots=EXCLUDED.total_shots,          total_passes=EXCLUDED.total_passes,
              goals_per_90=EXCLUDED.goals_per_90,        xg_per_90=EXCLUDED.xg_per_90,
              assists_per_90=EXCLUDED.assists_per_90,    shot_accuracy_pct=EXCLUDED.shot_accuracy_pct,
              shots_per_90=EXCLUDED.shots_per_90,        tackles_per_90=EXCLUDED.tackles_per_90
    """)
    conn.commit()
    print("✅ mart_player_season_stats materialized.")


def seed():
    if not DATABASE_URL and not PG_HOST:
        print("=" * 60)
        print("❌ LỖI: Chưa cấu hình kết nối PostgreSQL!")
        print("   Vui lòng vào GitHub: Settings -> Secrets and variables -> Actions")
        print("   Thêm DATABASE_URL: postgresql://user:pass@host:5432/dbname")
        print("=" * 60)
        sys.exit(1)

    if DATABASE_URL:
        print("📡 Connecting to PostgreSQL via DATABASE_URL...")
    else:
        print(f"📡 Connecting to PostgreSQL at {PG_HOST}:{PG_PORT} db={PG_DB}...")

    conn = get_conn()
    conn.autocommit = False
    cur = conn.cursor()
    print("✅ Connected to PostgreSQL successfully.")

    init_schema(cur)
    conn.commit()

    from datetime import date

    total_matches = 0
    failed = []

    catalog = build_catalog()
    print(f"\n🌍 Seeding {len(catalog)} competition-seasons "
          f"({MAX_MATCHES_PER_SEASON} matches each)...")

    for (comp_id, season_id, comp_name, season_name, comp_type) in catalog:
        # Upsert competition
        cur.execute("""
            INSERT INTO dim_competition (competition_id, name, country, type)
            VALUES (%s, %s, %s, %s)
            ON CONFLICT (competition_id) DO UPDATE
              SET name=EXCLUDED.name, type=EXCLUDED.type, updated_at=NOW()
        """, (comp_id, comp_name, comp_name, comp_type))

        # Upsert season
        cur.execute("""
            INSERT INTO dim_season (season_id, competition_id, name)
            VALUES (%s, %s, %s)
            ON CONFLICT (competition_id, season_id) DO UPDATE
              SET name=EXCLUDED.name, updated_at=NOW()
        """, (season_id, comp_id, season_name))
        conn.commit()

        try:
            n = seed_season(cur, conn, comp_id, season_id, comp_name, season_name, comp_type)
            total_matches += n
        except Exception as e:
            print(f"  ❌ Failed to seed {comp_name} {season_name}: {e}")
            conn.rollback()
            failed.append(f"{comp_name} {season_name}")

    materialize_mart(cur, conn)

    print(f"\n{'='*60}")
    print(f"🎉 Ingestion complete! {total_matches} matches across {len(catalog)} competition-seasons.")
    if failed:
        print(f"⚠️  {len(failed)} season(s) failed: {', '.join(failed)}")
    print(f"{'='*60}")

    cur.close()
    conn.close()


if __name__ == "__main__":
    seed()


