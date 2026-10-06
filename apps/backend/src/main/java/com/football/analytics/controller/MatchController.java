package com.football.analytics.controller;

import com.football.analytics.dto.ApiResponse;
import com.football.analytics.model.Match;
import com.football.analytics.service.MatchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "date_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "date_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit) {
        long start = System.currentTimeMillis();
        List<Match> data = matchService.getMatches(competitionId, seasonId, status, dateFrom, dateTo, limit);
        return ApiResponse.success(data, start, false);
    }

    @GetMapping("/{id}")
    public ApiResponse<Match> getMatch(@PathVariable Long id) {
        long start = System.currentTimeMillis();
        Match data = matchService.getMatchById(id);
        return ApiResponse.success(data, start, false);
    }
}
