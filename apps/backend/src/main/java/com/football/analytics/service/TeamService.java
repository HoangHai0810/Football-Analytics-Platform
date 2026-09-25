package com.football.analytics.service;

import com.football.analytics.exception.ResourceNotFoundException;
import com.football.analytics.model.Team;
import com.football.analytics.repository.SeedDataStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {
    private final SeedDataStore seedDataStore;

    public TeamService(SeedDataStore seedDataStore) {
        this.seedDataStore = seedDataStore;
    }

    public List<Team> getAllTeams() {
        return seedDataStore.getAllTeams();
    }

    public Team getTeamById(Long id) {
        return seedDataStore.getTeam(id)
            .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND", "Team with id " + id + " not found."));
    }
}
