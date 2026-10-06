package com.football.analytics.service;

import com.football.analytics.exception.ResourceNotFoundException;
import com.football.analytics.model.Match;
import com.football.analytics.repository.PostgresRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

@Service
public class MatchService {
    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final PostgresRepository postgresRepository;

    public MatchService(PostgresRepository postgresRepository) {
        this.postgresRepository = postgresRepository;
    }

    public List<Match> getMatches(Long competitionId, Long seasonId, String status,
                                  LocalDate dateFrom, LocalDate dateTo, Integer limit) {
        try {
            LocalDateTime fromTs = dateFrom != null ? dateFrom.atStartOfDay() : null;
            LocalDateTime toTs = dateTo != null ? dateTo.atTime(LocalTime.MAX) : null;
            int lim = limit != null ? limit : 100;
            List<Match> matches = postgresRepository.getAllMatches(
                competitionId, seasonId, status, fromTs, toTs, lim);
            return matches != null ? matches : Collections.emptyList();
        } catch (Exception e) {
            log.error("Matches query failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Match getMatchById(Long id) {
        try {
            return postgresRepository.getMatch(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "MATCH_NOT_FOUND", "Match with id " + id + " not found."));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("getMatch query failed: {}", e.getMessage());
            throw new ResourceNotFoundException(
                "MATCH_NOT_FOUND", "Match with id " + id + " not found.");
        }
    }
}
