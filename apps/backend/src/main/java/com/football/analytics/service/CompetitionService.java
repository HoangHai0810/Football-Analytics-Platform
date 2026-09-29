package com.football.analytics.service;

import com.football.analytics.exception.ResourceNotFoundException;
import com.football.analytics.model.Competition;
import com.football.analytics.model.Season;
import com.football.analytics.repository.SeedDataStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompetitionService {
    private final SeedDataStore seedDataStore;

    public CompetitionService(SeedDataStore seedDataStore) {
        this.seedDataStore = seedDataStore;
    }

    public List<Competition> getAllCompetitions() {
        return seedDataStore.getAllCompetitions();
    }

    public Competition getCompetitionById(Long id) {
        return seedDataStore.getCompetition(id)
            .orElseThrow(() -> new ResourceNotFoundException("COMPETITION_NOT_FOUND", "Competition with id " + id + " not found."));
    }

    public List<Season> getSeasons(Long competitionId) {
        getCompetitionById(competitionId); // validate existence
        return seedDataStore.getSeasonsByCompetition(competitionId);
    }
}
