from .base import FootballDataProvider
from .football_data_org_provider import FootballDataOrgProvider

try:
    from .statsbomb_provider import StatsBombProvider
    __all__ = ["FootballDataProvider", "StatsBombProvider", "FootballDataOrgProvider"]
except (ImportError, ModuleNotFoundError):
    __all__ = ["FootballDataProvider", "FootballDataOrgProvider"]
