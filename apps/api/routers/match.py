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

@router.get("/", response_model=List[Match])
async def list_matches():
    # Real match data is served by the Spring Boot API from ClickHouse.
    # This FastAPI stub intentionally returns empty — no mock data.
    return []
