import asyncio
import os
import time
from typing import Any, Dict, List, Optional
import httpx
from .base import FootballDataProvider

BASE_URL = "https://api.football-data.org/v4"

class FootballDataOrgProvider(FootballDataProvider):
    """
    Provider implementation for football-data.org v4 REST API.
    Provides live match scores, fixtures, standings, lineups, and main events.
    Enforces rate-limiting (max 10 requests/minute for Free Tier).
    """

    def __init__(self, api_key: Optional[str] = None):
        self.api_key = api_key or os.getenv("FOOTBALL_DATA_API_KEY", "")
        self.headers = {"X-Auth-Token": self.api_key} if self.api_key else {}
        self._last_request_time = 0.0
        # 6 seconds spacing guarantees at most 10 requests per minute
        self._min_interval = 6.0

    async def _throttle(self):
        """Throttle requests to respect Free tier 10 req/min rate limit."""
        now = time.time()
        elapsed = now - self._last_request_time
        if elapsed < self._min_interval:
            await asyncio.sleep(self._min_interval - elapsed)
        self._last_request_time = time.time()

    async def _get(self, endpoint: str, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        await self._throttle()
        url = f"{BASE_URL}/{endpoint.lstrip('/')}"
        async with httpx.AsyncClient(headers=self.headers, timeout=15.0) as client:
            response = await client.get(url, params=params)
            if response.status_code == 429:
                # Rate limited: wait 30s and retry once
                await asyncio.sleep(30.0)
                response = await client.get(url, params=params)
            response.raise_for_status()
            return response.json()

    async def get_competitions(self) -> List[Dict[str, Any]]:
        """Fetch available competitions."""
        data = await self._get("competitions")
        competitions = data.get("competitions", [])
        return [
            {
                "competition_id": c.get("id"),
                "name": c.get("name"),
                "code": c.get("code"),
                "country": (c.get("area") or {}).get("name", "International"),
                "type": c.get("type", "LEAGUE"),
            }
            for c in competitions
        ]

    async def get_matches(self, competition_id: int, season_id: Optional[int] = None) -> List[Dict[str, Any]]:
        """Fetch matches for a competition (optionally by season or status)."""
        params = {}
        if season_id:
            params["season"] = season_id
        data = await self._get(f"competitions/{competition_id}/matches", params=params)
        return self._normalize_matches(data.get("matches", []), competition_id=competition_id)

    async def get_live_matches(self) -> List[Dict[str, Any]]:
        """Fetch currently live / in-play matches across all active competitions."""
        data = await self._get("matches", params={"status": "IN_PLAY,PAUSED,LIVE"})
        return self._normalize_matches(data.get("matches", []))

    async def get_events(self, match_id: int) -> List[Dict[str, Any]]:
        """
        football-data.org does not provide touch-by-touch coordinate events.
        Extracts goal and card events from match details.
        """
        match = await self._get(f"matches/{match_id}")
        events: List[Dict[str, Any]] = []

        # Extract goals
        goals = match.get("goals") or []
        for g in goals:
            scorer = g.get("scorer") or {}
            team = g.get("team") or {}
            events.append({
                "event_id": f"goal_{match_id}_{g.get('minute')}_{scorer.get('id')}",
                "match_id": match_id,
                "player_id": scorer.get("id", 0),
                "team_id": team.get("id", 0),
                "event_type": "GOAL",
                "minute": g.get("minute", 0),
                "second": 0,
                "x": 105.0,  # Goal area approximation
                "y": 40.0,
                "outcome": "GOAL",
            })

        # Extract bookings / cards
        bookings = match.get("bookings") or []
        for b in bookings:
            player = b.get("player") or {}
            team = b.get("team") or {}
            events.append({
                "event_id": f"card_{match_id}_{b.get('minute')}_{player.get('id')}",
                "match_id": match_id,
                "player_id": player.get("id", 0),
                "team_id": team.get("id", 0),
                "event_type": b.get("card", "YELLOW_CARD"),
                "minute": b.get("minute", 0),
                "second": 0,
                "x": 60.0,
                "y": 40.0,
                "outcome": "CARD",
            })

        return events

    async def get_lineups(self, match_id: int) -> List[Dict[str, Any]]:
        """Extract lineups for home and away teams."""
        match = await self._get(f"matches/{match_id}")
        lineups = []
        for side in ["homeTeam", "awayTeam"]:
            team_info = match.get(side) or {}
            players = team_info.get("lineup") or []
            bench = team_info.get("bench") or []
            all_players = players + bench
            lineups.append({
                "team_id": team_info.get("id", 0),
                "team_name": team_info.get("name", ""),
                "players": [
                    {
                        "player_id": p.get("id"),
                        "name": p.get("name"),
                        "position": p.get("position"),
                        "shirt_number": p.get("shirtNumber"),
                    }
                    for p in all_players
                ],
            })
        return lineups

    async def get_standings(self, competition_id: int) -> List[Dict[str, Any]]:
        """Fetch current standings table for a competition."""
        data = await self._get(f"competitions/{competition_id}/standings")
        standings_list = data.get("standings", [])
        total_table = []
        for s in standings_list:
            if s.get("type") == "TOTAL":
                total_table = s.get("table", [])
                break
        if not total_table and standings_list:
            total_table = standings_list[0].get("table", [])

        return [
            {
                "position": row.get("position"),
                "team_id": (row.get("team") or {}).get("id"),
                "team_name": (row.get("team") or {}).get("name"),
                "played_games": row.get("playedGames"),
                "won": row.get("won"),
                "draw": row.get("draw"),
                "lost": row.get("lost"),
                "points": row.get("points"),
                "goals_for": row.get("goalsFor"),
                "goals_against": row.get("goalsAgainst"),
                "goal_difference": row.get("goalDifference"),
            }
            for row in total_table
        ]

    def _normalize_matches(self, matches: List[Dict[str, Any]], competition_id: Optional[int] = None) -> List[Dict[str, Any]]:
        normalized = []
        for m in matches:
            home = m.get("homeTeam") or {}
            away = m.get("awayTeam") or {}
            score = m.get("score") or {}
            full_time = score.get("fullTime") or {}
            season = m.get("season") or {}
            comp = m.get("competition") or {}

            comp_id = competition_id or comp.get("id", 0)
            season_id = season.get("id", int(m.get("utcDate", "2026")[:4]))

            normalized.append({
                "match_id": m.get("id"),
                "competition_id": comp_id,
                "season_id": season_id,
                "home_team_id": home.get("id"),
                "home_team_name": home.get("name"),
                "away_team_id": away.get("id"),
                "away_team_name": away.get("name"),
                "match_date": m.get("utcDate"),
                "status": m.get("status"),
                "matchday": m.get("matchday"),
                "home_score": full_time.get("home") if full_time.get("home") is not None else 0,
                "away_score": full_time.get("away") if full_time.get("away") is not None else 0,
                "winner": score.get("winner"),
            })
        return normalized
