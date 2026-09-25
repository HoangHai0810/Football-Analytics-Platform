from statsbombpy import sb
from .base import FootballDataProvider

class StatsBombProvider(FootballDataProvider):
    """Reads StatsBomb Open Data (free, no auth required)."""

    async def get_competitions(self) -> list[dict]:
        df = sb.competitions()
        return df.to_dict(orient="records")

    async def get_matches(self, competition_id: int, season_id: int) -> list[dict]:
        df = sb.matches(competition_id=competition_id, season_id=season_id)
        return df.to_dict(orient="records")

    async def get_events(self, match_id: int) -> list[dict]:
        df = sb.events(match_id=match_id, split=False, flatten_attrs=False)
        return df.to_dict(orient="records")

    async def get_lineups(self, match_id: int) -> list[dict]:
        lineups = sb.lineups(match_id=match_id)
        # Convert to simple list of dicts per team
        result = []
        for team_id, df in lineups.items():
            result.append({"team_id": team_id, "players": df.to_dict(orient="records")})
        return result
