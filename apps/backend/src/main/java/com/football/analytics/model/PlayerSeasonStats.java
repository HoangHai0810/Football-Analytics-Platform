package com.football.analytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerSeasonStats {
    @JsonProperty("player_id")
    private Long playerId;

    @JsonProperty("season_id")
    private Long seasonId;

    @JsonProperty("season_name")
    private String seasonName;

    @JsonProperty("matches_played")
    private Integer matchesPlayed;

    private Integer minutes;
    private Integer goals;
    private Integer assists;
    private Double xg;
    private Double xa;

    private Integer shots;
    @JsonProperty("shots_on_target")
    private Integer shotsOnTarget;

    private Integer passes;
    @JsonProperty("key_passes")
    private Integer keyPasses;

    @JsonProperty("pass_completion_rate")
    private Double passCompletionRate;

    // Per 90 metrics
    @JsonProperty("goals_per_90")
    private Double goalsPer90;

    @JsonProperty("assists_per_90")
    private Double assistsPer90;

    @JsonProperty("xg_per_90")
    private Double xgPer90;

    @JsonProperty("xa_per_90")
    private Double xaPer90;

    @JsonProperty("shots_per_90")
    private Double shotsPer90;

    // Defensive & Physical metrics
    private Integer tackles;
    private Integer interceptions;
    private Integer duels;

    @JsonProperty("duel_win_rate")
    private Double duelWinRate;

    private Integer pressures;

    // Normalized Radar Dimensions (0..100) for Frontend Radar Chart
    @JsonProperty("finishing_rating")
    private Integer finishingRating;

    @JsonProperty("creation_rating")
    private Integer creationRating;

    @JsonProperty("progression_rating")
    private Integer progressionRating;

    @JsonProperty("pressing_rating")
    private Integer pressingRating;

    @JsonProperty("aerial_rating")
    private Integer aerialRating;

    @JsonProperty("defending_rating")
    private Integer defendingRating;

    public PlayerSeasonStats() {}

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public Long getSeasonId() { return seasonId; }
    public void setSeasonId(Long seasonId) { this.seasonId = seasonId; }

    public String getSeasonName() { return seasonName; }
    public void setSeasonName(String seasonName) { this.seasonName = seasonName; }

    public Integer getMatchesPlayed() { return matchesPlayed; }
    public void setMatchesPlayed(Integer matchesPlayed) { this.matchesPlayed = matchesPlayed; }

    public Integer getMinutes() { return minutes; }
    public void setMinutes(Integer minutes) { this.minutes = minutes; }

    public Integer getGoals() { return goals; }
    public void setGoals(Integer goals) { this.goals = goals; }

    public Integer getAssists() { return assists; }
    public void setAssists(Integer assists) { this.assists = assists; }

    public Double getXg() { return xg; }
    public void setXg(Double xg) { this.xg = xg; }

    public Double getXa() { return xa; }
    public void setXa(Double xa) { this.xa = xa; }

    public Integer getShots() { return shots; }
    public void setShots(Integer shots) { this.shots = shots; }

    public Integer getShotsOnTarget() { return shotsOnTarget; }
    public void setShotsOnTarget(Integer shotsOnTarget) { this.shotsOnTarget = shotsOnTarget; }

    public Integer getPasses() { return passes; }
    public void setPasses(Integer passes) { this.passes = passes; }

    public Integer getKeyPasses() { return keyPasses; }
    public void setKeyPasses(Integer keyPasses) { this.keyPasses = keyPasses; }

    public Double getPassCompletionRate() { return passCompletionRate; }
    public void setPassCompletionRate(Double passCompletionRate) { this.passCompletionRate = passCompletionRate; }

    public Double getGoalsPer90() { return goalsPer90; }
    public void setGoalsPer90(Double goalsPer90) { this.goalsPer90 = goalsPer90; }

    public Double getAssistsPer90() { return assistsPer90; }
    public void setAssistsPer90(Double assistsPer90) { this.assistsPer90 = assistsPer90; }

    public Double getXgPer90() { return xgPer90; }
    public void setXgPer90(Double xgPer90) { this.xgPer90 = xgPer90; }

    public Double getXaPer90() { return xaPer90; }
    public void setXaPer90(Double xaPer90) { this.xaPer90 = xaPer90; }

    public Double getShotsPer90() { return shotsPer90; }
    public void setShotsPer90(Double shotsPer90) { this.shotsPer90 = shotsPer90; }

    public Integer getTackles() { return tackles; }
    public void setTackles(Integer tackles) { this.tackles = tackles; }

    public Integer getInterceptions() { return interceptions; }
    public void setInterceptions(Integer interceptions) { this.interceptions = interceptions; }

    public Integer getDuels() { return duels; }
    public void setDuels(Integer duels) { this.duels = duels; }

    public Double getDuelWinRate() { return duelWinRate; }
    public void setDuelWinRate(Double duelWinRate) { this.duelWinRate = duelWinRate; }

    public Integer getPressures() { return pressures; }
    public void setPressures(Integer pressures) { this.pressures = pressures; }

    public Integer getFinishingRating() { return finishingRating; }
    public void setFinishingRating(Integer finishingRating) { this.finishingRating = finishingRating; }

    public Integer getCreationRating() { return creationRating; }
    public void setCreationRating(Integer creationRating) { this.creationRating = creationRating; }

    public Integer getProgressionRating() { return progressionRating; }
    public void setProgressionRating(Integer progressionRating) { this.progressionRating = progressionRating; }

    public Integer getPressingRating() { return pressingRating; }
    public void setPressingRating(Integer pressingRating) { this.pressingRating = pressingRating; }

    public Integer getAerialRating() { return aerialRating; }
    public void setAerialRating(Integer aerialRating) { this.aerialRating = aerialRating; }

    public Integer getDefendingRating() { return defendingRating; }
    public void setDefendingRating(Integer defendingRating) { this.defendingRating = defendingRating; }
}
