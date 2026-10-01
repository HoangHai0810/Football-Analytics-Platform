package com.football.analytics.service;

import com.football.analytics.exception.ResourceNotFoundException;
import com.football.analytics.model.Match;
import com.football.analytics.repository.ClickHouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class MatchService {
    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final ClickHouseRepository clickHouseRepository;

    public MatchService(ClickHouseRepository clickHouseRepository) {
        this.clickHouseRepository = clickHouseRepository;
    }

    public List<Match> getMatches(Long competitionId, Long seasonId, String status) {
        try {
            List<Match> matches = clickHouseRepository.getAllMatches(competitionId, seasonId, status);
            return matches != null ? matches : Collections.emptyList();
        } catch (Exception e) {
            log.error("ClickHouse matches query failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Match getMatchById(Long id) {
        try {
            return clickHouseRepository.getMatch(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "MATCH_NOT_FOUND", "Match with id " + id + " not found."));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("ClickHouse getMatch query failed: {}", e.getMessage());
            throw new ResourceNotFoundException(
                "MATCH_NOT_FOUND", "Match with id " + id + " not found.");
        }
    }
}
