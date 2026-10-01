/**
 * Football Analytics Platform - API Service
 *
 * STRICT MODE: No mock data. All data comes from the real backend API.
 * If the API is unreachable, an error is thrown and the UI must handle it.
 *
 * Backend: https://football-analytics-platform.onrender.com
 * Data Source: Spring Boot → ClickHouse OLAP (real DE pipeline data)
 */

// Production backend — always absolute, always includes /api/v1
const PRODUCTION_BACKEND = 'https://football-analytics-platform.onrender.com/api/v1';

function resolveApiBase() {
  const raw = import.meta.env.VITE_API_BASE_URL;

  // No env var → use production in prod, localhost in dev
  if (!raw) {
    return import.meta.env.PROD ? PRODUCTION_BACKEND : 'http://localhost:8000/api/v1';
  }

  // Guard: if raw is a relative path (e.g. '/api/v1' for Nginx reverse proxy in Docker), keep it
  if (raw.startsWith('/')) {
    return raw.replace(/\/$/, '');
  }

  // Ensure protocol prefix for absolute URLs
  let url = (!raw.startsWith('http://') && !raw.startsWith('https://'))
    ? `https://${raw}`
    : raw;

  // Ensure /api/v1 suffix — guard against env var set without it
  if (!url.includes('/api/v')) {
    url = url.replace(/\/$/, '') + '/api/v1';
  }

  return url;
}

const API_BASE_URL = resolveApiBase();

// Always log so browser DevTools shows the URL being used
console.info('[API] Base URL:', API_BASE_URL);

/**
 * Core fetch wrapper — NO fallback, throws on failure.
 * Components must handle loading/error states themselves.
 */
async function apiFetch(path, options = {}) {
  const url = `${API_BASE_URL}${path}`;

  const response = await fetch(url, {
    ...options,
    headers: {
      'Accept': 'application/json',
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });

  if (!response.ok) {
    const errorBody = await response.text().catch(() => '');
    throw new Error(`API Error ${response.status} at ${url}: ${errorBody}`);
  }

  const json = await response.json();

  // Backend envelope: { data: [...], meta: {...} }
  return {
    data: json.data !== undefined ? json.data : json,
    meta: json.meta || { version: 'v1', cached: false },
    fromApi: true,
  };
}

export const api = {
  /**
   * GET /competitions
   * Returns list of all competitions from ClickHouse dim_competition
   */
  async getCompetitions() {
    return apiFetch('/competitions');
  },

  /**
   * GET /competitions/:id
   */
  async getCompetition(id) {
    return apiFetch(`/competitions/${id}`);
  },

  /**
   * GET /competitions/:id/seasons
   */
  async getSeasons(competitionId) {
    return apiFetch(`/competitions/${competitionId}/seasons`);
  },

  /**
   * GET /matches?competition_id=&season_id=&status=
   * Returns matches from ClickHouse dim_match + fact_match
   */
  async getMatches(params = {}) {
    const q = new URLSearchParams(
      Object.fromEntries(Object.entries(params).filter(([, v]) => v != null && v !== ''))
    ).toString();
    return apiFetch(`/matches${q ? `?${q}` : ''}`);
  },

  /**
   * GET /matches/:id
   */
  async getMatch(id) {
    return apiFetch(`/matches/${id}`);
  },

  /**
   * GET /players?query=&position=
   * Returns players from ClickHouse dim_player
   */
  async getPlayers(params = {}) {
    const q = new URLSearchParams(
      Object.fromEntries(Object.entries(params).filter(([, v]) => v != null && v !== '' && v !== 'ALL'))
    ).toString();
    return apiFetch(`/players${q ? `?${q}` : ''}`);
  },

  /**
   * GET /players/:id
   */
  async getPlayer(id) {
    return apiFetch(`/players/${id}`);
  },

  /**
   * GET /players/:id/stats?season_id=
   * Returns stats from mart_player_season_stats or fact_player_match aggregation
   */
  async getPlayerStats(playerId, seasonId = 2024) {
    return apiFetch(`/players/${playerId}/stats?season_id=${seasonId}`);
  },

  /**
   * GET /players/:id/shots?season_id=
   * Returns shot events from fact_event
   */
  async getPlayerShots(playerId, seasonId = 2024) {
    return apiFetch(`/players/${playerId}/shots?season_id=${seasonId}`);
  },

  /**
   * GET /players/compare?ids=p1,p2&season_id=
   */
  async comparePlayers(player1Id, player2Id, seasonId = 2024) {
    return apiFetch(`/players/compare?ids=${player1Id},${player2Id}&season_id=${seasonId}`);
  },

  /**
   * GET /teams
   */
  async getTeams() {
    return apiFetch('/teams');
  },

  /**
   * GET /teams/:id
   */
  async getTeam(id) {
    return apiFetch(`/teams/${id}`);
  },

  /**
   * POST /ai/analyze
   */
  async queryAiAnalyst(query, playerId = null) {
    return apiFetch('/ai/analyze', {
      method: 'POST',
      body: JSON.stringify({ query, player_id: playerId }),
    });
  },

  /**
   * GET /system/status
   */
  async getSystemStatus() {
    return apiFetch('/system/status');
  },

  getBaseUrl() {
    return API_BASE_URL;
  },
};
