import pytest
from data_platform.validators.kafka_payload_validator import (
    validate_competitions,
    validate_matches,
    validate_events,
    validate_lineups,
)


def test_validate_competitions():
    payload = [{"competition_id": 1, "competition_name": "Premier League", "country_name": "England"}]
    result = validate_competitions(payload)
    assert isinstance(result, list)
    assert result[0]["competition_id"] == 1
    assert result[0]["competition_name"] == "Premier League"


def test_validate_matches():
    payload = [{"match_id": 100, "competition_id": 1, "season_id": 2022, "home_team_id": 10, "away_team_id": 20}]
    result = validate_matches(payload)
    assert result[0]["match_id"] == 100
    assert result[0]["home_team_id"] == 10


def test_validate_events():
    payload = [{"event_id": 555, "match_id": 100, "player_id": 9, "team_id": 10, "event_type": "goal", "minute": 23, "second": 0, "x": 0.5, "y": 0.5, "outcome": "success"}]
    result = validate_events(payload)
    assert result[0]["event_type"] == "goal"
    assert result[0]["minute"] == 23


def test_validate_lineups():
    payload = [{"team_id": 10, "players": [{"id": 1, "name": "Player A"}]}]
    result = validate_lineups(payload)
    assert isinstance(result, list)
    assert result[0]["team_id"] == 10
