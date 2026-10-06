package com.football.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.football.analytics.model.Player;
import com.football.analytics.model.PlayerSeasonStats;
import java.util.List;
import java.util.Map;

public class ComparisonDto {
    private List<Player> players;

    @JsonProperty("season_stats")
    private List<PlayerSeasonStats> seasonStats;

    @JsonProperty("metric_comparisons")
    private Map<String, MetricComparison> metricComparisons;

    public ComparisonDto() {}

    public ComparisonDto(List<Player> players, List<PlayerSeasonStats> seasonStats, Map<String, MetricComparison> metricComparisons) {
        this.players = players;
        this.seasonStats = seasonStats;
        this.metricComparisons = metricComparisons;
    }

    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }

    public List<PlayerSeasonStats> getSeasonStats() { return seasonStats; }
    public void setSeasonStats(List<PlayerSeasonStats> seasonStats) { this.seasonStats = seasonStats; }

    public Map<String, MetricComparison> getMetricComparisons() { return metricComparisons; }
    public void setMetricComparisons(Map<String, MetricComparison> metricComparisons) { this.metricComparisons = metricComparisons; }

    public static class MetricComparison {
        private String metric;

        @JsonProperty("player1Value")
        private Double player1Value;

        @JsonProperty("player2Value")
        private Double player2Value;

        private String leader;
        private Double difference;

        public MetricComparison() {}

        public MetricComparison(String metric, Double player1Value, Double player2Value, String leader, Double difference) {
            this.metric = metric;
            this.player1Value = player1Value;
            this.player2Value = player2Value;
            this.leader = leader;
            this.difference = difference;
        }

        public String getMetric() { return metric; }
        public void setMetric(String metric) { this.metric = metric; }

        public Double getPlayer1Value() { return player1Value; }
        public void setPlayer1Value(Double player1Value) { this.player1Value = player1Value; }

        public Double getPlayer2Value() { return player2Value; }
        public void setPlayer2Value(Double player2Value) { this.player2Value = player2Value; }

        public String getLeader() { return leader; }
        public void setLeader(String leader) { this.leader = leader; }

        public Double getDifference() { return difference; }
        public void setDifference(Double difference) { this.difference = difference; }
    }
}
