package com.football.analytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class Competition {
    @JsonProperty("competition_id")
    private Long competitionId;

    private String name;
    private String country;
    private String type; // "LEAGUE" | "CUP"

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public Competition() {}

    public Competition(Long competitionId, String name, String country, String type, LocalDateTime updatedAt) {
        this.competitionId = competitionId;
        this.name = name;
        this.country = country;
        this.type = type;
        this.updatedAt = updatedAt;
    }

    public Long getCompetitionId() { return competitionId; }
    public void setCompetitionId(Long competitionId) { this.competitionId = competitionId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
