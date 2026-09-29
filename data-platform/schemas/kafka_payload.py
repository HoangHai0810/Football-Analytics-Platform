from typing import Any, List, Optional
from pydantic import BaseModel, Field

class CompetitionPayload(BaseModel):
    competition_id: int
    competition_name: str
    country_name: Optional[str] = "International"
    competition_type: Optional[str] = "LEAGUE"

class MatchPayload(BaseModel):
    match_id: int
    competition_id: int
    season_id: int
    home_team_id: int
    away_team_id: int
    match_date: Optional[str] = None
    status: Optional[str] = "FINISHED"

class EventPayload(BaseModel):
    event_id: Any
    match_id: int
    player_id: Optional[int] = 0
    team_id: Optional[int] = 0
    event_type: str
    minute: int
    second: Optional[int] = 0
    x: float
    y: float
    end_x: Optional[float] = None
    end_y: Optional[float] = None
    outcome: Optional[str] = "SUCCESS"

class LineupPlayer(BaseModel):
    id: Any
    name: Optional[str] = ""
    position: Optional[str] = None
    shirt_number: Optional[int] = None

class LineupPayload(BaseModel):
    team_id: int
    players: List[Any] = Field(default_factory=list)
