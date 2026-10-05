package com.football.analytics.service;

import com.football.analytics.exception.ResourceNotFoundException;
import com.football.analytics.model.Team;
import com.football.analytics.repository.PostgresRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class TeamService {
    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private final PostgresRepository PostgresRepository;

    public TeamService(PostgresRepository PostgresRepository) {
        this.PostgresRepository = PostgresRepository;
    }

    public List<Team> getAllTeams() {
        try {
            List<Team> teams = PostgresRepository.getAllTeams();
            return teams != null ? teams : Collections.emptyList();
        } catch (Exception e) {
            log.error("ClickHouse team query failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Team getTeamById(Long id) {
        try {
            return PostgresRepository.getTeam(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "TEAM_NOT_FOUND", "Team with id " + id + " not found."));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("ClickHouse getTeam query failed: {}", e.getMessage());
            throw new ResourceNotFoundException(
                "TEAM_NOT_FOUND", "Team with id " + id + " not found.");
        }
    }
}
