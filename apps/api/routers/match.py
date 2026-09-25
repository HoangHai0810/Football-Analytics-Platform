from fastapi import APIRouter
from typing import List
from pydantic import BaseModel

router = APIRouter()

class Match(BaseModel):
    match_id: int
    competition_id: int
    season_id: int
    home_team_id: int
    away_team_id: int
    match_date: str
    status: str

# Dummy data placeholder – real data will come from ClickHouse via DE layer
FAKE_MATCHES: List[Match] = []

@router.get("/", response_model=List[Match])
async def list_matches():
    return FAKE_MATCHES
