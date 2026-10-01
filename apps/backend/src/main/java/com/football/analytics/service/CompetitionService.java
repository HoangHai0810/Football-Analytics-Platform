package com.football.analytics.service;

import com.football.analytics.exception.ResourceNotFoundException;
import com.football.analytics.model.Competition;
import com.football.analytics.model.Season;
import com.football.analytics.repository.ClickHouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CompetitionService {
    private static final Logger log = LoggerFactory.getLogger(CompetitionService.class);

    private final ClickHouseRepository clickHouseRepository;

    public CompetitionService(ClickHouseRepository clickHouseRepository) {
        this.clickHouseRepository = clickHouseRepository;
    }

    public List<Competition> getAllCompetitions() {
        try {
            List<Competition> comps = clickHouseRepository.getAllCompetitions();
            return comps != null ? comps : Collections.emptyList();
        } catch (Exception e) {
            log.error("ClickHouse competition query failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Competition getCompetitionById(Long id) {
        try {
            return clickHouseRepository.getCompetition(id)
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
            List<Season> seasons = clickHouseRepository.getSeasonsByCompetition(competitionId);
            return seasons != null ? seasons : Collections.emptyList();
        } catch (Exception e) {
            log.error("ClickHouse getSeasons query failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
