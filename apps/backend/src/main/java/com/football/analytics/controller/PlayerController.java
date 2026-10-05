package com.football.analytics.controller;

import com.football.analytics.dto.ApiResponse;
import com.football.analytics.dto.ComparisonDto;
import com.football.analytics.model.Player;
import com.football.analytics.model.PlayerSeasonStats;
import com.football.analytics.model.ShotEvent;
import com.football.analytics.service.PlayerService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {
    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    public ApiResponse<List<Player>> getPlayers(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "position", required = false) String position) {
        long start = System.currentTimeMillis();
        List<Player> data = playerService.getAllPlayers(query, position);
        return ApiResponse.success(data, start, false);
    }

    @GetMapping("/compare")
    public ApiResponse<ComparisonDto> comparePlayers(
            @RequestParam("ids") String idsParam,
            @RequestParam(value = "season_id", required = false) Long seasonId) {
        long start = System.currentTimeMillis();
        List<Long> ids = Arrays.stream(idsParam.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());
        ComparisonDto data = playerService.comparePlayers(ids, seasonId);
        return ApiResponse.success(data, start, false);
    }

    @GetMapping("/{id}")
    public ApiResponse<Player> getPlayer(@PathVariable Long id) {
        long start = System.currentTimeMillis();
        Player data = playerService.getPlayerById(id);
        return ApiResponse.success(data, start, false);
    }

    @GetMapping("/{id}/stats")
    public ApiResponse<PlayerSeasonStats> getPlayerStats(
            @PathVariable Long id,
            @RequestParam(value = "season_id", required = false) Long seasonId) {
        long start = System.currentTimeMillis();
        PlayerSeasonStats data = playerService.getPlayerStats(id, seasonId);
        return ApiResponse.success(data, start, false);
    }

    @GetMapping("/{id}/shots")
    public ApiResponse<List<ShotEvent>> getPlayerShots(
            @PathVariable Long id,
            @RequestParam(value = "season_id", required = false) Long seasonId) {
        long start = System.currentTimeMillis();
        List<ShotEvent> data = playerService.getPlayerShots(id, seasonId);
        return ApiResponse.success(data, start, false);
    }
}
