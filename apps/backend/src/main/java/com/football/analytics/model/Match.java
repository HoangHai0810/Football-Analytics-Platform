package com.football.analytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class Match {
    @JsonProperty("match_id")
    private Long matchId;

    @JsonProperty("competition_id")
    private Long competitionId;

    @JsonProperty("competition_name")
    private String competitionName;

    @JsonProperty("season_id")
    private Long seasonId;

    @JsonProperty("home_team_id")
    private Long homeTeamId;

    @JsonProperty("home_team_name")
    private String homeTeamName;

    @JsonProperty("home_team_logo")
    private String homeTeamLogo;

    @JsonProperty("away_team_id")
    private Long awayTeamId;

    @JsonProperty("away_team_name")
    private String awayTeamName;

    @JsonProperty("away_team_logo")
    private String awayTeamLogo;

    @JsonProperty("home_score")
    private Integer homeScore;

    @JsonProperty("away_score")
    private Integer awayScore;

    @JsonProperty("home_xg")
    private Double homeXg;

    @JsonProperty("away_xg")
    private Double awayXg;

    @JsonProperty("match_date")
    private LocalDateTime matchDate;

    private String status; // "SCHEDULED", "LIVE", "FINISHED"
    private String stadium;
    private Long attendance;

    public Match() {}

    public Match(Long matchId, Long competitionId, String competitionName, Long seasonId,
                 Long homeTeamId, String homeTeamName, String homeTeamLogo,
                 Long awayTeamId, String awayTeamName, String awayTeamLogo,
                 Integer homeScore, Integer awayScore, Double homeXg, Double awayXg,
                 LocalDateTime matchDate, String status, String stadium, Long attendance) {
        this.matchId = matchId;
        this.competitionId = competitionId;
        this.competitionName = competitionName;
        this.seasonId = seasonId;
        this.homeTeamId = homeTeamId;
        this.homeTeamName = homeTeamName;
        this.homeTeamLogo = homeTeamLogo;
        this.awayTeamId = awayTeamId;
        this.awayTeamName = awayTeamName;
        this.awayTeamLogo = awayTeamLogo;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.homeXg = homeXg;
        this.awayXg = awayXg;
        this.matchDate = matchDate;
        this.status = status;
        this.stadium = stadium;
        this.attendance = attendance;
    }

    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    public Long getCompetitionId() { return competitionId; }
    public void setCompetitionId(Long competitionId) { this.competitionId = competitionId; }

    public String getCompetitionName() { return competitionName; }
    public void setCompetitionName(String competitionName) { this.competitionName = competitionName; }

    public Long getSeasonId() { return seasonId; }
    public void setSeasonId(Long seasonId) { this.seasonId = seasonId; }

    public Long getHomeTeamId() { return homeTeamId; }
    public void setHomeTeamId(Long homeTeamId) { this.homeTeamId = homeTeamId; }

    public String getHomeTeamName() { return homeTeamName; }
    public void setHomeTeamName(String homeTeamName) { this.homeTeamName = homeTeamName; }

    public String getHomeTeamLogo() { return homeTeamLogo; }
    public void setHomeTeamLogo(String homeTeamLogo) { this.homeTeamLogo = homeTeamLogo; }

    public Long getAwayTeamId() { return awayTeamId; }
    public void setAwayTeamId(Long awayTeamId) { this.awayTeamId = awayTeamId; }

    public String getAwayTeamName() { return awayTeamName; }
    public void setAwayTeamName(String awayTeamName) { this.awayTeamName = awayTeamName; }

    public String getAwayTeamLogo() { return awayTeamLogo; }
    public void setAwayTeamLogo(String awayTeamLogo) { this.awayTeamLogo = awayTeamLogo; }

    public Integer getHomeScore() { return homeScore; }
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }

    public Integer getAwayScore() { return awayScore; }
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }

    public Double getHomeXg() { return homeXg; }
    public void setHomeXg(Double homeXg) { this.homeXg = homeXg; }

    public Double getAwayXg() { return awayXg; }
    public void setAwayXg(Double awayXg) { this.awayXg = awayXg; }

    public LocalDateTime getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDateTime matchDate) { this.matchDate = matchDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStadium() { return stadium; }
    public void setStadium(String stadium) { this.stadium = stadium; }

    public Long getAttendance() { return attendance; }
    public void setAttendance(Long attendance) { this.attendance = attendance; }
}
