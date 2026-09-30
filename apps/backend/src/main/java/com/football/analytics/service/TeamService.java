package com.football.analytics.service;

import com.football.analytics.exception.ResourceNotFoundException;
import com.football.analytics.model.Team;
import com.football.analytics.repository.ClickHouseRepository;
import com.football.analytics.repository.SeedDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {
    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private final ClickHouseRepository clickHouseRepository;
    private final SeedDataStore seedDataStore;

    public TeamService(ClickHouseRepository clickHouseRepository, SeedDataStore seedDataStore) {
        this.clickHouseRepository = clickHouseRepository;
        this.seedDataStore = seedDataStore;
    }

    public List<Team> getAllTeams() {
        try {
            List<Team> teams = clickHouseRepository.getAllTeams();
            if (teams != null && !teams.isEmpty()) {
                return teams;
            }
        } catch (Exception e) {
            log.warn("ClickHouse team query failed: {}", e.getMessage());
        }
        return seedDataStore.getAllTeams();
    }

    public Team getTeamById(Long id) {
        try {
            var team = clickHouseRepository.getTeam(id);
            if (team.isPresent()) {
                return team.get();
            }
        } catch (Exception e) {
            log.warn("ClickHouse getTeam query failed: {}", e.getMessage());
        }
        return seedDataStore.getTeam(id)
            .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND", "Team with id " + id + " not found."));
    }
}

