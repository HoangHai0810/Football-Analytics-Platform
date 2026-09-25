package com.football.analytics.controller;

import com.football.analytics.dto.ApiResponse;
import com.football.analytics.model.Match;
import com.football.analytics.service.MatchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {
    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    public ApiResponse<List<Match>> getMatches(
            @RequestParam(value = "competition_id", required = false) Long competitionId,
            @RequestParam(value = "season_id", required = false) Long seasonId,
            @RequestParam(value = "status", required = false) String status) {
        long start = System.currentTimeMillis();
        List<Match> data = matchService.getMatches(competitionId, seasonId, status);
        return ApiResponse.success(data, start, false);
    }

    @GetMapping("/{id}")
    public ApiResponse<Match> getMatch(@PathVariable Long id) {
        long start = System.currentTimeMillis();
        Match data = matchService.getMatchById(id);
        return ApiResponse.success(data, start, false);
    }
}
