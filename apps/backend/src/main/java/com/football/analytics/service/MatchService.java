package com.football.analytics.service;

import com.football.analytics.exception.ResourceNotFoundException;
import com.football.analytics.model.Match;
import com.football.analytics.repository.ClickHouseRepository;
import com.football.analytics.repository.SeedDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchService {
    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final ClickHouseRepository clickHouseRepository;
    private final SeedDataStore seedDataStore;

    public MatchService(ClickHouseRepository clickHouseRepository, SeedDataStore seedDataStore) {
        this.clickHouseRepository = clickHouseRepository;
        this.seedDataStore = seedDataStore;
    }

    public List<Match> getMatches(Long competitionId, Long seasonId, String status) {
        try {
            List<Match> matches = clickHouseRepository.getAllMatches(competitionId, seasonId, status);
            if (matches != null && !matches.isEmpty()) {
                return matches;
            }
        } catch (Exception e) {
            log.warn("ClickHouse matches query failed: {}", e.getMessage());
        }
        return seedDataStore.getAllMatches(competitionId, seasonId, status);
    }

    public Match getMatchById(Long id) {
        try {
            var match = clickHouseRepository.getMatch(id);
            if (match.isPresent()) {
                return match.get();
            }
        } catch (Exception e) {
            log.warn("ClickHouse getMatch query failed: {}", e.getMessage());
        }
        return seedDataStore.getMatch(id)
            .orElseThrow(() -> new ResourceNotFoundException("MATCH_NOT_FOUND", "Match with id " + id + " not found."));
    }
}

