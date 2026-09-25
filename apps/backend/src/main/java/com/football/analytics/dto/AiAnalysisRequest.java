package com.football.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AiAnalysisRequest {
    private String query;

    @JsonProperty("player_id")
    private Long playerId;

    @JsonProperty("match_id")
    private Long matchId;

    @JsonProperty("season_id")
    private Long seasonId;

    public AiAnalysisRequest() {}

    public AiAnalysisRequest(String query, Long playerId, Long matchId, Long seasonId) {
        this.query = query;
        this.playerId = playerId;
        this.matchId = matchId;
        this.seasonId = seasonId;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    public Long getSeasonId() { return seasonId; }
    public void setSeasonId(Long seasonId) { this.seasonId = seasonId; }
}
