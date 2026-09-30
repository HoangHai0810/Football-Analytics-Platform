package com.football.analytics.service;

import com.football.analytics.exception.ResourceNotFoundException;
import com.football.analytics.model.Competition;
import com.football.analytics.model.Season;
import com.football.analytics.repository.ClickHouseRepository;
import com.football.analytics.repository.SeedDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompetitionService {
    private static final Logger log = LoggerFactory.getLogger(CompetitionService.class);

    private final ClickHouseRepository clickHouseRepository;
    private final SeedDataStore seedDataStore;

    public CompetitionService(ClickHouseRepository clickHouseRepository, SeedDataStore seedDataStore) {
        this.clickHouseRepository = clickHouseRepository;
        this.seedDataStore = seedDataStore;
    }

    public List<Competition> getAllCompetitions() {
        try {
            List<Competition> comps = clickHouseRepository.getAllCompetitions();
            if (comps != null && !comps.isEmpty()) {
                return comps;
            }
        } catch (Exception e) {
            log.warn("ClickHouse competition query failed: {}", e.getMessage());
        }
        return seedDataStore.getAllCompetitions();
    }

    public Competition getCompetitionById(Long id) {
        try {
            var comp = clickHouseRepository.getCompetition(id);
            if (comp.isPresent()) {
                return comp.get();
            }
        } catch (Exception e) {
            log.warn("ClickHouse getCompetition query failed: {}", e.getMessage());
        }
        return seedDataStore.getCompetition(id)
            .orElseThrow(() -> new ResourceNotFoundException("COMPETITION_NOT_FOUND", "Competition with id " + id + " not found."));
    }

    public List<Season> getSeasons(Long competitionId) {
        getCompetitionById(competitionId); // validate existence
        try {
            List<Season> seasons = clickHouseRepository.getSeasonsByCompetition(competitionId);
            if (seasons != null && !seasons.isEmpty()) {
                return seasons;
            }
        } catch (Exception e) {
            log.warn("ClickHouse getSeasons query failed: {}", e.getMessage());
        }
        return seedDataStore.getSeasonsByCompetition(competitionId);
    }
}

