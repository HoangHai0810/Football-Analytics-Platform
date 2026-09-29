from data_platform.schemas.kafka_payload import CompetitionPayload, MatchPayload, EventPayload, LineupPayload
from typing import List, Any

def _dump(model_instance):
    if hasattr(model_instance, "model_dump"):
        return model_instance.model_dump()
    return model_instance.dict()

def validate_competitions(payload: List[dict]) -> List[dict]:
    """Validate a list of competition dicts and return validated dicts."""
    return [_dump(CompetitionPayload(**p)) for p in payload]

def validate_matches(payload: List[dict]) -> List[dict]:
    """Validate a list of match dicts and return validated dicts."""
    return [_dump(MatchPayload(**p)) for p in payload]

def validate_events(payload: List[dict]) -> List[dict]:
    """Validate a list of event dicts and return validated dicts."""
    return [_dump(EventPayload(**p)) for p in payload]

def validate_lineups(payload: List[Any]) -> List[dict]:
    """Validate lineup payloads (list of team dicts)."""
    return [_dump(LineupPayload(**p)) for p in payload]
