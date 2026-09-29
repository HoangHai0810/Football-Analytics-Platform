package com.football.analytics.service;

import com.football.analytics.exception.ResourceNotFoundException;
import com.football.analytics.model.Match;
import com.football.analytics.repository.SeedDataStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchService {
    private final SeedDataStore seedDataStore;

    public MatchService(SeedDataStore seedDataStore) {
        this.seedDataStore = seedDataStore;
    }

    public List<Match> getMatches(Long competitionId, Long seasonId, String status) {
        return seedDataStore.getAllMatches(competitionId, seasonId, status);
    }

    public Match getMatchById(Long id) {
        return seedDataStore.getMatch(id)
            .orElseThrow(() -> new ResourceNotFoundException("MATCH_NOT_FOUND", "Match with id " + id + " not found."));
    }
}
