package com.football.analytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public class Player {
    @JsonProperty("player_id")
    private Long playerId;

    @JsonProperty("team_id")
    private Long teamId;

    @JsonProperty("team_name")
    private String teamName;

    private String name;

    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;

    private String nationality;
    private String position; // "FW", "MF", "DF", "GK"

    @JsonProperty("preferred_foot")
    private String preferredFoot; // "LEFT", "RIGHT", "BOTH"

    @JsonProperty("jersey_number")
    private Integer jerseyNumber;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    public Player() {}

    public Player(Long playerId, Long teamId, String teamName, String name, LocalDate dateOfBirth,
                  String nationality, String position, String preferredFoot, Integer jerseyNumber, String avatarUrl) {
        this.playerId = playerId;
        this.teamId = teamId;
        this.teamName = teamName;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
        this.position = position;
        this.preferredFoot = preferredFoot;
        this.jerseyNumber = jerseyNumber;
        this.avatarUrl = avatarUrl;
    }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getPreferredFoot() { return preferredFoot; }
    public void setPreferredFoot(String preferredFoot) { this.preferredFoot = preferredFoot; }

    public Integer getJerseyNumber() { return jerseyNumber; }
    public void setJerseyNumber(Integer jerseyNumber) { this.jerseyNumber = jerseyNumber; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
