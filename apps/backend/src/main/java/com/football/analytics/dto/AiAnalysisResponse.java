package com.football.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class AiAnalysisResponse {
    private String intent;
    private String answer;

    @JsonProperty("grounded_facts")
    private List<String> groundedFacts;

    @JsonProperty("statistics_table")
    private Map<String, Object> statisticsTable;

    @JsonProperty("data_source")
    private String dataSource;

    @JsonProperty("confidence_score")
    private Double confidenceScore;

    @JsonProperty("suggested_questions")
    private List<String> suggestedQuestions;

    public AiAnalysisResponse() {}

    public AiAnalysisResponse(String intent, String answer, List<String> groundedFacts,
                              Map<String, Object> statisticsTable, String dataSource,
                              Double confidenceScore, List<String> suggestedQuestions) {
        this.intent = intent;
        this.answer = answer;
        this.groundedFacts = groundedFacts;
        this.statisticsTable = statisticsTable;
        this.dataSource = dataSource;
        this.confidenceScore = confidenceScore;
        this.suggestedQuestions = suggestedQuestions;
    }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<String> getGroundedFacts() { return groundedFacts; }
    public void setGroundedFacts(List<String> groundedFacts) { this.groundedFacts = groundedFacts; }

    public Map<String, Object> getStatisticsTable() { return statisticsTable; }
    public void setStatisticsTable(Map<String, Object> statisticsTable) { this.statisticsTable = statisticsTable; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public List<String> getSuggestedQuestions() { return suggestedQuestions; }
    public void setSuggestedQuestions(List<String> suggestedQuestions) { this.suggestedQuestions = suggestedQuestions; }
}
