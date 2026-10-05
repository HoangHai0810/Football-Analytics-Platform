package com.football.analytics.service;

import com.football.analytics.exception.ResourceNotFoundException;
import com.football.analytics.model.Competition;
import com.football.analytics.model.Season;
import com.football.analytics.repository.PostgresRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CompetitionService {
    private static final Logger log = LoggerFactory.getLogger(CompetitionService.class);

    private final PostgresRepository PostgresRepository;

    public CompetitionService(PostgresRepository PostgresRepository) {
        this.PostgresRepository = PostgresRepository;
    }

    public List<Competition> getAllCompetitions() {
        try {
            List<Competition> comps = PostgresRepository.getAllCompetitions();
            return comps != null ? comps : Collections.emptyList();
        } catch (Exception e) {
            log.error("ClickHouse competition query failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Competition getCompetitionById(Long id) {
        try {
            return PostgresRepository.getCompetition(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "COMPETITION_NOT_FOUND", "Competition with id " + id + " not found."));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("ClickHouse getCompetition query failed: {}", e.getMessage());
            throw new ResourceNotFoundException(
                "COMPETITION_NOT_FOUND", "Competition with id " + id + " not found.");
        }
    }

    public List<Season> getSeasons(Long competitionId) {
        getCompetitionById(competitionId);
        try {
            List<Season> seasons = PostgresRepository.getSeasonsByCompetition(competitionId);
            return seasons != null ? seasons : Collections.emptyList();
        } catch (Exception e) {
            log.error("ClickHouse getSeasons query failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
