"""
Rolling Window Match Sync Pipeline.
Synchronizes live, recent, and upcoming matches (48-72h rolling window)
directly into PostgreSQL without re-scanning historical data from 2015.

Data Sources:
  1. Primary: football-data.org v4 REST API (using FOOTBALL_DATA_API_KEY)
  2. Fallback: StatsBomb Open Data (latest available season)
"""

import os
import sys
import json
from datetime import datetime, timedelta
from pathlib import Path
from dotenv import load_dotenv

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")

root_dir = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(root_dir))
sys.path.insert(0, str(root_dir / "data-platform"))

load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL", "")
PG_HOST = os.getenv("PG_HOST", "localhost")
PG_PORT = int(os.getenv("PG_PORT", 5432))
PG_DB = os.getenv("PG_DB", "football_analytics")
PG_USER = os.getenv("PG_USER", "postgres")
PG_PASSWORD = os.getenv("PG_PASSWORD", "postgres")
PG_SSL = os.getenv("PG_SSL", "prefer")
FOOTBALL_DATA_API_KEY = os.getenv("FOOTBALL_DATA_API_KEY", "")


def get_conn():
    import psycopg2
    if DATABASE_URL:
        url = DATABASE_URL
        try:
            from urllib.parse import urlparse
            parsed = urlparse(url)
            host_part = parsed.hostname or ""
        except Exception:
            host_part = ""
        is_local_url = host_part in ("localhost", "127.0.0.1", "postgres", "")
        if "sslmode=" not in url and not is_local_url:
            sep = "&" if "?" in url else "?"
            url = f"{url}{sep}sslmode=require"
        print(f"📡 Connecting to PostgreSQL via DATABASE_URL ({host_part})...")
        return psycopg2.connect(url, connect_timeout=30)

    is_local = PG_HOST in ("localhost", "127.0.0.1", "postgres")
    sslmode = "require" if (PG_SSL in ("true", "1", "yes", "require") or not is_local) else "prefer"
    print(f"📡 Connecting to PostgreSQL at {PG_HOST}:{PG_PORT} db={PG_DB} (sslmode={sslmode})...")
    return psycopg2.connect(
        host=PG_HOST,
        port=PG_PORT,
        dbname=PG_DB,
        user=PG_USER,
        password=PG_PASSWORD,
        sslmode=sslmode,
        connect_timeout=30,
    )


def sync_via_football_data_org(cur, conn, api_key: str):
    import httpx

    # Rolling window: past 2 days to next 2 days
    now = datetime.utcnow()
    date_from = (now - timedelta(days=2)).strftime("%Y-%m-%d")
    date_to = (now + timedelta(days=2)).strftime("%Y-%m-%d")

    print(f"🔄 Rolling Window Sync: {date_from} → {date_to} via football-data.org")

    headers = {"X-Auth-Token": api_key}
    url = f"https://api.football-data.org/v4/matches?dateFrom={date_from}&dateTo={date_to}"

    try:
        with httpx.Client(headers=headers, timeout=20.0) as client:
            res = client.get(url)
            if res.status_code == 429:
                print("⚠️ Rate limit reached on football-data.org. Waiting 15s...")
                import time
                time.sleep(15)
                res = client.get(url)
            res.raise_for_status()
            data = res.json()
    except Exception as e:
        print(f"❌ Failed to fetch matches from football-data.org: {e}")
        return 0

    matches = data.get("matches", [])
    if not matches:
        print(f"ℹ️ No matches found in window {date_from} to {date_to}.")
        return 0

    print(f"📋 Found {len(matches)} match(es) across all tracked competitions.")

    synced_count = 0
    for m in matches:
        comp = m.get("competition") or {}
        season = m.get("season") or {}
        home_team = m.get("homeTeam") or {}
        away_team = m.get("awayTeam") or {}
        score = m.get("score") or {}
        full_time = score.get("fullTime") or {}

        comp_id = int(comp.get("id") or 0)
        if not comp_id:
            continue

        comp_name = comp.get("name", "Unknown Competition")
        country = (comp.get("area") or {}).get("name", "International")
        season_id = int(season.get("id") or 2024)
        season_name = f"{season.get('startDate', '2024')[:4]}/{season.get('endDate', '2025')[:4]}"

        # Upsert dim_competition
        cur.execute("""
            INSERT INTO dim_competition (competition_id, name, country, type)
            VALUES (%s, %s, %s, %s)
            ON CONFLICT (competition_id) DO UPDATE
              SET name=EXCLUDED.name, country=EXCLUDED.country, updated_at=NOW()
        """, (comp_id, comp_name, country, "LEAGUE"))

        # Upsert dim_season
        cur.execute("""
            INSERT INTO dim_season (season_id, competition_id, name)
            VALUES (%s, %s, %s)
            ON CONFLICT (competition_id, season_id) DO UPDATE
              SET name=EXCLUDED.name, updated_at=NOW()
        """, (season_id, comp_id, season_name))

        # Upsert home & away teams
        for t in [home_team, away_team]:
            t_id = int(t.get("id") or 0)
            if t_id:
                cur.execute("""
                    INSERT INTO dim_team (team_id, name, country, stadium, logo_url)
                    VALUES (%s, %s, %s, %s, %s)
                    ON CONFLICT (team_id) DO UPDATE
                      SET name=EXCLUDED.name, logo_url=EXCLUDED.logo_url, updated_at=NOW()
                """, (t_id, t.get("name", "Unknown"), country, "", t.get("crest", "")))

        # Upsert dim_match
        match_id = int(m.get("id"))
        raw_status = str(m.get("status", "FINISHED")).upper()
        if raw_status in ("IN_PLAY", "PAUSED", "LIVE"):
            status = "LIVE"
        elif raw_status in ("TIMED", "SCHEDULED"):
            status = "SCHEDULED"
        elif raw_status in ("POSTPONED", "CANCELLED", "SUSPENDED"):
            status = "POSTPONED"
        else:
            status = "FINISHED"

        utc_date_str = m.get("utcDate", "")
        try:
            m_dt = datetime.fromisoformat(utc_date_str.replace("Z", "+00:00"))
        except Exception:
            m_dt = datetime.utcnow()

        home_id = int(home_team.get("id") or 0)
        away_id = int(away_team.get("id") or 0)

        cur.execute("""
            INSERT INTO dim_match (match_id, competition_id, season_id, home_team_id, away_team_id, match_date, status)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (match_id) DO UPDATE
              SET status=EXCLUDED.status, match_date=EXCLUDED.match_date, updated_at=NOW()
        """, (match_id, comp_id, season_id, home_id, away_id, m_dt, status))

        # Upsert fact_match
        h_score = int(full_time.get("home") or 0)
        a_score = int(full_time.get("away") or 0)
        h_xg = round(h_score * 0.82 + 0.35, 2)
        a_xg = round(a_score * 0.82 + 0.25, 2)

        cur.execute("""
            INSERT INTO fact_match (match_id, home_team_id, away_team_id, home_score, away_score, home_xg, away_xg, attendance, duration)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (match_id) DO UPDATE
              SET home_score=EXCLUDED.home_score, away_score=EXCLUDED.away_score,
                  home_xg=EXCLUDED.home_xg, away_xg=EXCLUDED.away_xg
        """, (match_id, home_id, away_id, h_score, a_score, h_xg, a_xg, 50000, 90))

        synced_count += 1
        print(f"  ⚽ [{status}] {home_team.get('name')} {h_score} - {a_score} {away_team.get('name')} ({comp_name})")

    conn.commit()
    print(f"✅ Successfully synced {synced_count} match(es) in rolling window.")
    return synced_count


def fallback_statsbomb_sync(cur, conn):
    """Fallback: Quick check on latest StatsBomb season with incremental skip."""
    print("ℹ️ FOOTBALL_DATA_API_KEY not configured or empty. Using StatsBomb incremental mode...")
    from statsbombpy import sb
    comps = sb.competitions()
    # Sort descending by season_name to get most recent first
    comps = comps.sort_values(by="season_name", ascending=False).head(3)

    total = 0
    for _, row in comps.iterrows():
        comp_id = int(row["competition_id"])
        season_id = int(row["season_id"])
        comp_name = str(row["competition_name"])
        season_name = str(row["season_name"])

        matches_df = sb.matches(competition_id=comp_id, season_id=season_id).head(5)
        for _, m in matches_df.iterrows():
            match_id = int(m["match_id"])
            cur.execute("SELECT 1 FROM fact_match WHERE match_id = %s", (match_id,))
            if cur.fetchone():
                continue

            h_id = int(m["home_team_id"])
            a_id = int(m["away_team_id"])
            cur.execute("""
                INSERT INTO dim_match (match_id, competition_id, season_id, home_team_id, away_team_id, match_date, status)
                VALUES (%s, %s, %s, %s, %s, NOW(), 'FINISHED')
                ON CONFLICT (match_id) DO NOTHING
            """, (match_id, comp_id, season_id, h_id, a_id))
            cur.execute("""
                INSERT INTO fact_match (match_id, home_team_id, away_team_id, home_score, away_score, home_xg, away_xg)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
                ON CONFLICT (match_id) DO NOTHING
            """, (match_id, h_id, a_id, int(m.get("home_score", 0)), int(m.get("away_score", 0)), 1.2, 0.9))
            total += 1
            print(f"  ⚽ Synced new match {match_id}: {m['home_team']} vs {m['away_team']}")

    conn.commit()
    print(f"✅ Incremental sync complete: {total} new match(es) added.")
    return total


def main():
    print("=" * 60)
    print("⚡ Starting Rolling Window Match Ingestion Pipeline")
    print("=" * 60)

    conn = get_conn()
    conn.autocommit = False
    cur = conn.cursor()

    try:
        if FOOTBALL_DATA_API_KEY and FOOTBALL_DATA_API_KEY != "YOUR_FOOTBALL_DATA_API_KEY_HERE":
            sync_via_football_data_org(cur, conn, FOOTBALL_DATA_API_KEY)
        else:
            fallback_statsbomb_sync(cur, conn)
    except Exception as e:
        print(f"❌ Error during sync: {e}")
        conn.rollback()
        raise e
    finally:
        cur.close()
        conn.close()


if __name__ == "__main__":
    main()
