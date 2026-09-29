package com.football.analytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ShotEvent {
    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("match_id")
    private Long matchId;

    @JsonProperty("player_id")
    private Long playerId;

    @JsonProperty("player_name")
    private String playerName;

    @JsonProperty("team_id")
    private Long teamId;

    private Integer minute;
    private Integer second;

    // Standard pitch coordinates: x: 0..120, y: 0..80
    private Double x;
    private Double y;

    private Double xg;
    private String outcome; // "GOAL", "SAVED", "BLOCKED", "MISSED", "POST"

    @JsonProperty("body_part")
    private String bodyPart; // "RIGHT_FOOT", "LEFT_FOOT", "HEAD"

    @JsonProperty("situation")
    private String situation; // "OPEN_PLAY", "PENALTY", "FREE_KICK", "CORNER"

    public ShotEvent() {}

    public ShotEvent(String eventId, Long matchId, Long playerId, String playerName, Long teamId,
                     Integer minute, Integer second, Double x, Double y, Double xg,
                     String outcome, String bodyPart, String situation) {
        this.eventId = eventId;
        this.matchId = matchId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.teamId = teamId;
        this.minute = minute;
        this.second = second;
        this.x = x;
        this.y = y;
        this.xg = xg;
        this.outcome = outcome;
        this.bodyPart = bodyPart;
        this.situation = situation;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public Integer getMinute() { return minute; }
    public void setMinute(Integer minute) { this.minute = minute; }

    public Integer getSecond() { return second; }
    public void setSecond(Integer second) { this.second = second; }

    public Double getX() { return x; }
    public void setX(Double x) { this.x = x; }

    public Double getY() { return y; }
    public void setY(Double y) { this.y = y; }

    public Double getXg() { return xg; }
    public void setXg(Double xg) { this.xg = xg; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getBodyPart() { return bodyPart; }
    public void setBodyPart(String bodyPart) { this.bodyPart = bodyPart; }

    public String getSituation() { return situation; }
    public void setSituation(String situation) { this.situation = situation; }
}
