import argparse
import asyncio
import os
import sys
from pathlib import Path
from dotenv import load_dotenv

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")

root_dir = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(root_dir))
sys.path.insert(0, str(root_dir / "data-platform"))

try:
    from data_platform.providers.football_data_org_provider import FootballDataOrgProvider
    from data_platform.ingestion.producers import FootballKafkaProducer
except ModuleNotFoundError:
    from providers.football_data_org_provider import FootballDataOrgProvider
    from ingestion.producers import FootballKafkaProducer

# Load environment variables
load_dotenv()

# Top European Competitions in football-data.org Free Tier:
# 2021: Premier League, 2014: La Liga, 2019: Serie A, 2002: Bundesliga, 2001: Champions League
DEFAULT_COMPETITIONS = [2021, 2014]

async def ingest_live_or_recent_matches(provider: FootballDataOrgProvider, producer: FootballKafkaProducer):
    print("\n🔍 Checking for live in-play matches from football-data.org...")
    try:
        live_matches = await provider.get_live_matches()
    except Exception as e:
        print(f"⚠️ Error checking live matches: {e}")
        live_matches = []

    matches_to_publish = []
    if live_matches:
        print(f"⚡ Found {len(live_matches)} LIVE match(es) in progress!")
        matches_to_publish.extend(live_matches)
    else:
        print("ℹ️ No matches currently live. Fetching latest matchday fixtures for Premier League & La Liga...")
        for comp_id in DEFAULT_COMPETITIONS:
            try:
                comp_matches = await provider.get_matches(competition_id=comp_id)
                # Take recent or upcoming 5 matches per competition
                matches_to_publish.extend(comp_matches[:5])
            except Exception as e:
                print(f"⚠️ Error fetching matches for competition {comp_id}: {e}")

    if not matches_to_publish:
        print("❌ No matches retrieved. Check API key in .env or network connectivity.")
        return 0

    print(f"🚀 Publishing {len(matches_to_publish)} match record(s) to Kafka topic 'football.raw.matches'...")
    published_count = 0
    for match in matches_to_publish:
        m_id = str(match["match_id"])
        producer.publish(
            topic="football.raw.matches",
            source="football_data_org",
            payload=match,
            key=m_id,
        )
        published_count += 1
        print(f"  ⚽ Match {match['match_id']}: {match.get('home_team_name')} ({match.get('home_score')}) vs ({match.get('away_score')}) {match.get('away_team_name')} [{match.get('status')}]")

        # If live, also fetch goals and bookings
        if match.get("status") in ["IN_PLAY", "PAUSED", "LIVE"]:
            try:
                events = await provider.get_events(match_id=int(match["match_id"]))
                for ev in events:
                    producer.publish(
                        topic="football.raw.events",
                        source="football_data_org",
                        payload=ev,
                        key=m_id,
                    )
                if events:
                    print(f"    ↳ Published {len(events)} live event(s) to 'football.raw.events'")
            except Exception as e:
                print(f"    ⚠️ Could not fetch events for match {match['match_id']}: {e}")

    producer.flush()
    print(f"✅ Successfully published {published_count} match(es) to Kafka.\n")
    return published_count

async def main():
    parser = argparse.ArgumentParser(description="Live matches Kafka producer for football-data.org")
    parser.add_argument("--poll", action="store_true", help="Continuously poll for live match updates")
    parser.add_argument("--interval", type=int, default=60, help="Polling interval in seconds (default: 60s)")
    args = parser.parse_args()

    api_key = os.getenv("FOOTBALL_DATA_API_KEY", "")
    if not api_key or api_key == "YOUR_FOOTBALL_DATA_API_KEY_HERE":
        print("\n" + "=" * 60)
        print("⚠️ WARNING: FOOTBALL_DATA_API_KEY is not set in .env")
        print("👉 Register a free API token in 30 seconds at:")
        print("   https://www.football-data.org/client/register")
        print("   Then set: FOOTBALL_DATA_API_KEY=<your_token> in your .env file.")
        print("=" * 60 + "\n")

    kafka_brokers = os.getenv("KAFKA_BROKERS", "localhost:9092")
    provider = FootballDataOrgProvider(api_key=api_key)
    producer = FootballKafkaProducer(brokers=kafka_brokers)

    if args.poll:
        print(f"🔄 Starting continuous live polling every {args.interval} seconds (Press Ctrl+C to stop)...")
        while True:
            await ingest_live_or_recent_matches(provider, producer)
            await asyncio.sleep(args.interval)
    else:
        await ingest_live_or_recent_matches(provider, producer)

if __name__ == "__main__":
    asyncio.run(main())
