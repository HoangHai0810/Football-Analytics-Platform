package com.football.analytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public class Season {
    @JsonProperty("season_id")
    private Long seasonId;

    @JsonProperty("competition_id")
    private Long competitionId;

    private String name;

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    public Season() {}

    public Season(Long seasonId, Long competitionId, String name, LocalDate startDate, LocalDate endDate) {
        this.seasonId = seasonId;
        this.competitionId = competitionId;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getSeasonId() { return seasonId; }
    public void setSeasonId(Long seasonId) { this.seasonId = seasonId; }

    public Long getCompetitionId() { return competitionId; }
    public void setCompetitionId(Long competitionId) { this.competitionId = competitionId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
