from data_platform.schemas.kafka_payload import CompetitionPayload, MatchPayload, EventPayload, LineupPayload
from typing import List, Any

def validate_competitions(payload: List[dict]) -> List[dict]:
    """Validate a list of competition dicts and return validated dicts."""
    return [CompetitionPayload(**p).dict() for p in payload]

def validate_matches(payload: List[dict]) -> List[dict]:
    """Validate a list of match dicts and return validated dicts."""
    return [MatchPayload(**p).dict() for p in payload]

def validate_events(payload: List[dict]) -> List[dict]:
    """Validate a list of event dicts and return validated dicts."""
    return [EventPayload(**p).dict() for p in payload]

def validate_lineups(payload: List[Any]) -> List[dict]:
    """Validate lineup payloads (list of team dicts)."""
    return [LineupPayload(**p).dict() for p in payload]
