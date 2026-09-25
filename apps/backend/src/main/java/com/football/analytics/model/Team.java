package com.football.analytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Team {
    @JsonProperty("team_id")
    private Long teamId;

    private String name;

    @JsonProperty("short_name")
    private String shortName;

    private String country;
    private String stadium;

    @JsonProperty("logo_url")
    private String logoUrl;

    public Team() {}

    public Team(Long teamId, String name, String shortName, String country, String stadium, String logoUrl) {
        this.teamId = teamId;
        this.name = name;
        this.shortName = shortName;
        this.country = country;
        this.stadium = stadium;
        this.logoUrl = logoUrl;
    }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getStadium() { return stadium; }
    public void setStadium(String stadium) { this.stadium = stadium; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
}
