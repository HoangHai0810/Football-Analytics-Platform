package com.football.analytics.controller;

import com.football.analytics.dto.ApiResponse;
import com.football.analytics.model.Team;
import com.football.analytics.service.TeamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {
    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public ApiResponse<List<Team>> getTeams() {
        long start = System.currentTimeMillis();
        List<Team> data = teamService.getAllTeams();
        return ApiResponse.success(data, start, false);
    }

    @GetMapping("/{id}")
    public ApiResponse<Team> getTeam(@PathVariable Long id) {
        long start = System.currentTimeMillis();
        Team data = teamService.getTeamById(id);
        return ApiResponse.success(data, start, false);
    }
}
