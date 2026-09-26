import pytest
from unittest.mock import AsyncMock, patch
from data_platform.providers.football_data_org_provider import FootballDataOrgProvider

@pytest.mark.asyncio
async def test_get_competitions_parses_correctly():
    mock_response = {
        "competitions": [
            {
                "id": 2021,
                "name": "Premier League",
                "code": "PL",
                "area": {"name": "England"},
                "type": "LEAGUE",
            }
        ]
    }
    provider = FootballDataOrgProvider(api_key="test_key")
    with patch.object(provider, "_get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_response
        competitions = await provider.get_competitions()
        assert len(competitions) == 1
        assert competitions[0]["competition_id"] == 2021
        assert competitions[0]["name"] == "Premier League"
        assert competitions[0]["country"] == "England"

@pytest.mark.asyncio
async def test_get_matches_normalizes_correctly():
    mock_response = {
        "matches": [
            {
                "id": 4321,
                "utcDate": "2026-09-26T15:00:00Z",
                "status": "IN_PLAY",
                "matchday": 5,
                "homeTeam": {"id": 65, "name": "Manchester City FC"},
                "awayTeam": {"id": 66, "name": "Manchester United FC"},
                "score": {
                    "winner": None,
                    "fullTime": {"home": 2, "away": 1},
                },
                "competition": {"id": 2021},
                "season": {"id": 2026},
            }
        ]
    }
    provider = FootballDataOrgProvider(api_key="test_key")
    with patch.object(provider, "_get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_response
        matches = await provider.get_matches(competition_id=2021)
        assert len(matches) == 1
        assert matches[0]["match_id"] == 4321
        assert matches[0]["home_score"] == 2
        assert matches[0]["away_score"] == 1
        assert matches[0]["status"] == "IN_PLAY"
        assert matches[0]["home_team_name"] == "Manchester City FC"

@pytest.mark.asyncio
async def test_get_events_extracts_goals_and_cards():
    mock_match_details = {
        "id": 4321,
        "goals": [
            {
                "minute": 23,
                "scorer": {"id": 100, "name": "Haaland"},
                "team": {"id": 65},
            }
        ],
        "bookings": [
            {
                "minute": 45,
                "player": {"id": 200, "name": "Casemiro"},
                "team": {"id": 66},
                "card": "YELLOW_CARD",
            }
        ],
    }
    provider = FootballDataOrgProvider(api_key="test_key")
    with patch.object(provider, "_get", new_callable=AsyncMock) as mock_get:
        mock_get.return_value = mock_match_details
        events = await provider.get_events(match_id=4321)
        assert len(events) == 2
        assert events[0]["event_type"] == "GOAL"
        assert events[0]["minute"] == 23
        assert events[0]["player_id"] == 100
        assert events[1]["event_type"] == "YELLOW_CARD"
        assert events[1]["minute"] == 45
