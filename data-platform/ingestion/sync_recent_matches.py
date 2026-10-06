"""
Rolling Window Match Sync Pipeline.
Synchronizes live, recent, and upcoming matches into PostgreSQL,
plus team squads (players) and verified crests from football-data.org.

Data Sources:
  1. Primary (REQUIRED for recent/live fixtures): football-data.org v4
     Set FOOTBALL_DATA_API_KEY in GitHub Secrets / .env
  2. Emergency fallback: StatsBomb Open Data — historical only, NOT recent 48h
"""

import os
import sys
import time
from datetime import datetime, timedelta, timezone
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
FOOTBALL_DATA_API_KEY = (os.getenv("FOOTBALL_DATA_API_KEY") or "").strip()

# Wider window so UI always has recent + upcoming fixtures
DAYS_BACK = int(os.getenv("SYNC_DAYS_BACK", "21"))
DAYS_FORWARD = int(os.getenv("SYNC_DAYS_FORWARD", "7"))
# Fail cron when key missing (default). Set ALLOW_STATSBOMB_FALLBACK=1 only for emergency.
ALLOW_STATSBOMB_FALLBACK = os.getenv("ALLOW_STATSBOMB_FALLBACK", "").strip().lower() in (
    "1", "true", "yes",
)


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


def ensure_schema(cur, conn):
    """Idempotent schema patches + cleanup of invented / wrong-mapped fields."""
    sql_path = root_dir / "infrastructure" / "postgres" / "init_analytics.sql"
    if sql_path.exists():
        cur.execute(sql_path.read_text(encoding="utf-8"))
        conn.commit()
        print("✅ Schema verified from init_analytics.sql")
    else:
        cur.execute("ALTER TABLE dim_player ADD COLUMN IF NOT EXISTS jersey_number INTEGER DEFAULT 0")
        cur.execute("ALTER TABLE dim_player ADD COLUMN IF NOT EXISTS avatar_url TEXT DEFAULT ''")
        cur.execute("ALTER TABLE dim_player ADD COLUMN IF NOT EXISTS team_id BIGINT DEFAULT 0")
        conn.commit()
        print("✅ Minimal schema patches applied")

    # Remove invented stadium placeholders
    cur.execute("""
        UPDATE dim_team
        SET stadium = '', updated_at = NOW()
        WHERE stadium ILIKE 'Stadium of %' OR lower(stadium) = 'stadium'
    """)
    # Crests wrongly glued onto StatsBomb-origin teams (competition name stored as country)
    cur.execute("""
        UPDATE dim_team t
        SET logo_url = '', updated_at = NOW()
        WHERE t.logo_url LIKE 'https://crests.football-data.org/%'
          AND (
            EXISTS (SELECT 1 FROM dim_competition c WHERE c.name = t.country)
            OR t.stadium ILIKE 'Stadium of %'
          )
    """)
    # Never keep generated avatar placeholders on players
    cur.execute("""
        UPDATE dim_player
        SET avatar_url = '', updated_at = NOW()
        WHERE avatar_url LIKE '%ui-avatars.com%'
           OR avatar_url LIKE '%crests.football-data.org%'
    """)
    conn.commit()
    print("✅ Invented stadium / mis-mapped crest / fake avatar cleanup done")


def map_position(raw: str) -> str:
    p = (raw or "").strip().lower()
    if p in ("goalkeeper", "gk"):
        return "GK"
    if p in ("defence", "defender", "df", "back"):
        return "DF"
    if p in ("midfield", "midfielder", "mf"):
        return "MF"
    if p in ("offence", "offense", "forward", "attacker", "fw", "striker"):
        return "FW"
    return ""


def upsert_team(cur, team_id: int, name: str, country: str, crest: str, stadium: str = ""):
    if not team_id:
        return
    cur.execute("""
        INSERT INTO dim_team (team_id, name, country, stadium, logo_url)
        VALUES (%s, %s, %s, %s, %s)
        ON CONFLICT (team_id) DO UPDATE
          SET name=EXCLUDED.name,
              logo_url=COALESCE(NULLIF(EXCLUDED.logo_url, ''), dim_team.logo_url),
              stadium=COALESCE(NULLIF(EXCLUDED.stadium, ''), dim_team.stadium),
              country=COALESCE(NULLIF(EXCLUDED.country, ''), dim_team.country),
              updated_at=NOW()
    """, (team_id, name or "Unknown", country or "", stadium or "", crest or ""))


def upsert_player(cur, player_id: int, name: str, dob, nationality: str, position: str,
                  jersey: int, team_id: int):
    if not player_id or not name:
        return
    cur.execute("""
        INSERT INTO dim_player
          (player_id, name, date_of_birth, nationality, position, preferred_foot, jersey_number, avatar_url, team_id)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        ON CONFLICT (player_id) DO UPDATE
          SET name=EXCLUDED.name,
              date_of_birth=COALESCE(EXCLUDED.date_of_birth, dim_player.date_of_birth),
              nationality=COALESCE(NULLIF(EXCLUDED.nationality, ''), dim_player.nationality),
              position=COALESCE(NULLIF(EXCLUDED.position, ''), dim_player.position),
              jersey_number=CASE WHEN EXCLUDED.jersey_number > 0 THEN EXCLUDED.jersey_number ELSE dim_player.jersey_number END,
              team_id=CASE WHEN EXCLUDED.team_id > 0 THEN EXCLUDED.team_id ELSE dim_player.team_id END,
              updated_at=NOW()
    """, (
        player_id,
        name.strip(),
        dob,
        nationality or "",
        position or "",
        "",  # never invent preferred foot
        jersey or 0,
        "",  # no reliable free photo URL from football-data free tier
        team_id or 0,
    ))


def sync_team_squad(cur, client, team_id: int, country: str):
    """Pull real squad roster + crest for a team."""
    try:
        res = client.get(f"https://api.football-data.org/v4/teams/{team_id}")
        if res.status_code == 429:
            print("⚠️ Rate limit on team squad — waiting 12s...")
            time.sleep(12)
            res = client.get(f"https://api.football-data.org/v4/teams/{team_id}")
        if res.status_code != 200:
            return 0
        data = res.json()
    except Exception as e:
        print(f"  ⚠️ Squad fetch failed for team {team_id}: {e}")
        return 0

    crest = data.get("crest") or ""
    stadium = (data.get("venue") or "") if isinstance(data.get("venue"), str) else ""
    area = (data.get("area") or {}).get("name") or country or ""
    upsert_team(cur, team_id, data.get("name", "Unknown"), area, crest, stadium)

    count = 0
    for p in (data.get("squad") or []):
        pid = int(p.get("id") or 0)
        pname = (p.get("name") or "").strip()
        if not pid or not pname:
            continue
        dob = None
        if p.get("dateOfBirth"):
            try:
                dob = datetime.strptime(p["dateOfBirth"][:10], "%Y-%m-%d").date()
            except Exception:
                dob = None
        jersey = int(p.get("shirtNumber") or 0)
        upsert_player(
            cur, pid, pname, dob,
            p.get("nationality") or "",
            map_position(p.get("position") or ""),
            jersey, team_id,
        )
        count += 1
    return count


def sync_via_football_data_org(cur, conn, api_key: str):
    import httpx

    now = datetime.now(timezone.utc)
    date_from = (now - timedelta(days=DAYS_BACK)).strftime("%Y-%m-%d")
    date_to = (now + timedelta(days=DAYS_FORWARD)).strftime("%Y-%m-%d")

    print(f"🔄 Rolling Window Sync: {date_from} → {date_to} via football-data.org")

    headers = {"X-Auth-Token": api_key}
    url = f"https://api.football-data.org/v4/matches?dateFrom={date_from}&dateTo={date_to}"

    try:
        with httpx.Client(headers=headers, timeout=30.0) as client:
            res = client.get(url)
            if res.status_code == 429:
                print("⚠️ Rate limit reached on football-data.org. Waiting 15s...")
                time.sleep(15)
                res = client.get(url)
            if res.status_code == 403:
                print("❌ FOOTBALL_DATA_API_KEY rejected (403). Check the secret value.")
                return 0
            res.raise_for_status()
            data = res.json()

            matches = data.get("matches", [])
            if not matches:
                print(f"ℹ️ No matches found in window {date_from} to {date_to}.")
                return 0

            print(f"📋 Found {len(matches)} match(es) across tracked competitions.")

            team_ids = set()
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
                country = (comp.get("area") or {}).get("name", "")
                season_id = int(season.get("id") or 0) or int(str(season.get("startDate", "2024"))[:4] or 2024)
                start_y = (season.get("startDate") or "2024")[:4]
                end_y = (season.get("endDate") or "2025")[:4]
                season_name = f"{start_y}/{end_y}"

                cur.execute("""
                    INSERT INTO dim_competition (competition_id, name, country, type)
                    VALUES (%s, %s, %s, %s)
                    ON CONFLICT (competition_id) DO UPDATE
                      SET name=EXCLUDED.name, country=EXCLUDED.country, updated_at=NOW()
                """, (comp_id, comp_name, country, "LEAGUE"))

                cur.execute("""
                    INSERT INTO dim_season (season_id, competition_id, name)
                    VALUES (%s, %s, %s)
                    ON CONFLICT (competition_id, season_id) DO UPDATE
                      SET name=EXCLUDED.name, updated_at=NOW()
                """, (season_id, comp_id, season_name))

                for t in [home_team, away_team]:
                    t_id = int(t.get("id") or 0)
                    if t_id:
                        team_ids.add(t_id)
                        upsert_team(cur, t_id, t.get("name", "Unknown"), country, t.get("crest") or "")

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
                    print(f"  ⚠️ Skipping match {match_id}: invalid utcDate")
                    continue

                home_id = int(home_team.get("id") or 0)
                away_id = int(away_team.get("id") or 0)

                cur.execute("""
                    INSERT INTO dim_match (match_id, competition_id, season_id, home_team_id, away_team_id, match_date, status)
                    VALUES (%s, %s, %s, %s, %s, %s, %s)
                    ON CONFLICT (match_id) DO UPDATE
                      SET status=EXCLUDED.status, match_date=EXCLUDED.match_date, updated_at=NOW()
                """, (match_id, comp_id, season_id, home_id, away_id, m_dt, status))

                # Scores only — NEVER invent xG from score formulas
                h_score = full_time.get("home")
                a_score = full_time.get("away")
                h_score_i = int(h_score) if h_score is not None else 0
                a_score_i = int(a_score) if a_score is not None else 0

                cur.execute("""
                    INSERT INTO fact_match (match_id, home_team_id, away_team_id, home_score, away_score, home_xg, away_xg, attendance, duration)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                    ON CONFLICT (match_id) DO UPDATE
                      SET home_score=EXCLUDED.home_score, away_score=EXCLUDED.away_score
                """, (match_id, home_id, away_id, h_score_i, a_score_i, 0, 0, 0, 90))

                synced_count += 1
                kickoff = m_dt.strftime("%Y-%m-%d %H:%M UTC")
                score_txt = f"{h_score_i}-{a_score_i}" if status == "FINISHED" else "-:-"
                print(f"  ⚽ [{status}] {kickoff} | {home_team.get('name')} {score_txt} {away_team.get('name')} ({comp_name})")

            conn.commit()

            # Sync squads for teams seen in this window (rate-limit friendly)
            print(f"👥 Syncing squads for {len(team_ids)} team(s)...")
            total_players = 0
            for i, tid in enumerate(sorted(team_ids)):
                n = sync_team_squad(cur, client, tid, "")
                total_players += n
                if i < len(team_ids) - 1:
                    time.sleep(6.5)  # free tier ~10 req/min
                if (i + 1) % 5 == 0:
                    conn.commit()
                    print(f"  … {i + 1}/{len(team_ids)} teams, {total_players} players so far")

            conn.commit()
            print(f"✅ Synced {synced_count} match(es), {total_players} player roster rows.")
            return synced_count
    except Exception as e:
        print(f"❌ Failed to sync from football-data.org: {e}")
        return 0


def backfill_players_from_statsbomb_lineups(cur, conn, max_matches: int = 30):
    """Ensure dim_player has real lineup players from already-stored StatsBomb matches."""
    print("👥 Backfilling players from StatsBomb lineups of existing matches...")
    from statsbombpy import sb

    cur.execute("""
        SELECT match_id FROM dim_match
        ORDER BY match_date DESC
        LIMIT %s
    """, (max_matches,))
    match_ids = [int(r[0]) for r in cur.fetchall()]
    total = 0
    for match_id in match_ids:
        try:
            lineups = sb.lineups(match_id=match_id)
        except Exception as e:
            print(f"  ⚠️ lineups {match_id}: {e}")
            continue
        for _, df_lineup in lineups.items():
            for _, p_row in df_lineup.iterrows():
                pid = int(p_row["player_id"])
                pname = str(p_row.get("player_name") or p_row.get("player_nickname") or "").strip()
                if not pid or not pname:
                    continue
                p_country = str(p_row.get("country") or "").strip()
                if p_country.lower() in ("nan", "none"):
                    p_country = ""
                p_jersey = int(p_row.get("jersey_number") or 0)
                positions_list = p_row.get("positions")
                pos_short = ""
                if isinstance(positions_list, list) and len(positions_list) > 0:
                    pos_name = str(positions_list[0].get("position", ""))
                    if any(k in pos_name for k in ["Forward", "Striker", "Wing"]):
                        pos_short = "FW"
                    elif "Midfield" in pos_name:
                        pos_short = "MF"
                    elif any(k in pos_name for k in ["Back", "Defender"]):
                        pos_short = "DF"
                    elif "Goalkeeper" in pos_name:
                        pos_short = "GK"
                upsert_player(cur, pid, pname, None, p_country, pos_short, p_jersey, 0)
                total += 1
        conn.commit()
    print(f"✅ Player backfill upserts: {total}")
    return total


def fallback_statsbomb_sync(cur, conn):
    """Emergency only: StatsBomb open data is historical — cannot provide live 48h fixtures."""
    print("⚠️ StatsBomb fallback is HISTORICAL open data — not recent live fixtures.")
    from statsbombpy import sb
    comps = sb.competitions()
    comps = comps.sort_values(by="season_name", ascending=False).head(3)

    total = 0
    for _, row in comps.iterrows():
        comp_id = int(row["competition_id"])
        season_id = int(row["season_id"])
        matches_df = sb.matches(competition_id=comp_id, season_id=season_id).head(5)
        for _, m in matches_df.iterrows():
            match_id = int(m["match_id"])
            cur.execute("SELECT 1 FROM fact_match WHERE match_id = %s", (match_id,))
            if cur.fetchone():
                continue

            h_id = int(m["home_team_id"])
            a_id = int(m["away_team_id"])
            m_date_str = str(m.get("match_date", ""))
            try:
                m_dt = datetime.strptime(m_date_str[:10], "%Y-%m-%d")
            except Exception:
                continue  # never invent NOW() as match date

            # Teams without inventing logos/stadiums
            for tid, tname in [(h_id, m["home_team"]), (a_id, m["away_team"])]:
                cur.execute("""
                    INSERT INTO dim_team (team_id, name, country, stadium, logo_url)
                    VALUES (%s, %s, %s, %s, %s)
                    ON CONFLICT (team_id) DO UPDATE
                      SET name=EXCLUDED.name, updated_at=NOW()
                """, (tid, str(tname), "", "", ""))

            cur.execute("""
                INSERT INTO dim_match (match_id, competition_id, season_id, home_team_id, away_team_id, match_date, status)
                VALUES (%s, %s, %s, %s, %s, %s, 'FINISHED')
                ON CONFLICT (match_id) DO NOTHING
            """, (match_id, comp_id, season_id, h_id, a_id, m_dt))
            cur.execute("""
                INSERT INTO fact_match (match_id, home_team_id, away_team_id, home_score, away_score, home_xg, away_xg)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
                ON CONFLICT (match_id) DO NOTHING
            """, (
                match_id, h_id, a_id,
                int(m.get("home_score", 0) or 0),
                int(m.get("away_score", 0) or 0),
                float(m["home_team_xg"]) if m.get("home_team_xg") is not None else 0,
                float(m["away_team_xg"]) if m.get("away_team_xg") is not None else 0,
            ))
            total += 1
            print(f"  ⚽ Synced new match {match_id}: {m['home_team']} vs {m['away_team']}")

    conn.commit()
    backfill_players_from_statsbomb_lineups(cur, conn)
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
        ensure_schema(cur, conn)
        has_key = bool(FOOTBALL_DATA_API_KEY) and FOOTBALL_DATA_API_KEY not in (
            "YOUR_FOOTBALL_DATA_API_KEY_HERE", "changeme", "null", "None",
        )

        if has_key:
            n = sync_via_football_data_org(cur, conn, FOOTBALL_DATA_API_KEY)
            if n == 0:
                print("ℹ️ football-data.org returned 0 matches in the window (or request failed).")
        else:
            print("❌ FOOTBALL_DATA_API_KEY not configured or empty.")
            print("   Rolling sync CANNOT fetch recent/live fixtures without it.")
            print("   Register free key: https://www.football-data.org/client/register")
            print("   Add GitHub Secret: FOOTBALL_DATA_API_KEY")
            if ALLOW_STATSBOMB_FALLBACK:
                print("⚠️ ALLOW_STATSBOMB_FALLBACK=1 — running historical emergency path...")
                fallback_statsbomb_sync(cur, conn)
            else:
                # Still try to repair empty player table from existing matches
                cur.execute("SELECT count(*) FROM dim_player")
                player_count = int(cur.fetchone()[0] or 0)
                if player_count == 0:
                    print("ℹ️ dim_player empty — attempting StatsBomb lineup backfill...")
                    backfill_players_from_statsbomb_lineups(cur, conn)
                else:
                    # Refresh a batch of lineups so team_id / jersey stay accurate
                    backfill_players_from_statsbomb_lineups(cur, conn, max_matches=15)
                raise SystemExit(
                    "FATAL: FOOTBALL_DATA_API_KEY required for rolling sync of recent matches. "
                    "Set the GitHub secret and re-run."
                )
    except SystemExit:
        conn.rollback()
        raise
    except Exception as e:
        print(f"❌ Error during sync: {e}")
        conn.rollback()
        raise e
    finally:
        cur.close()
        conn.close()


if __name__ == "__main__":
    main()
