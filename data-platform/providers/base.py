from abc import ABC, abstractmethod
from typing import Any, List

class FootballDataProvider(ABC):
    """Abstract base — decouples ingestion logic from vendor-specific APIs."""

    @abstractmethod
    async def get_competitions(self) -> List[dict[str, Any]]: ...

    @abstractmethod
    async def get_matches(self, competition_id: int, season_id: int) -> List[dict[str, Any]]: ...

    @abstractmethod
    async def get_events(self, match_id: int) -> List[dict[str, Any]]: ...

    @abstractmethod
    async def get_lineups(self, match_id: int) -> List[dict[str, Any]]: ...
