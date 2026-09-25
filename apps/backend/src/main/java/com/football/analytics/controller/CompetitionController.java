package com.football.analytics.controller;

import com.football.analytics.dto.ApiResponse;
import com.football.analytics.model.Competition;
import com.football.analytics.model.Season;
import com.football.analytics.service.CompetitionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/competitions")
public class CompetitionController {
    private final CompetitionService competitionService;

    public CompetitionController(CompetitionService competitionService) {
        this.competitionService = competitionService;
    }

    @GetMapping
    public ApiResponse<List<Competition>> getCompetitions() {
        long start = System.currentTimeMillis();
        List<Competition> data = competitionService.getAllCompetitions();
        return ApiResponse.success(data, start, false);
    }

    @GetMapping("/{id}")
    public ApiResponse<Competition> getCompetition(@PathVariable Long id) {
        long start = System.currentTimeMillis();
        Competition data = competitionService.getCompetitionById(id);
        return ApiResponse.success(data, start, false);
    }

    @GetMapping("/{id}/seasons")
    public ApiResponse<List<Season>> getSeasons(@PathVariable Long id) {
        long start = System.currentTimeMillis();
        List<Season> data = competitionService.getSeasons(id);
        return ApiResponse.success(data, start, false);
    }
}
