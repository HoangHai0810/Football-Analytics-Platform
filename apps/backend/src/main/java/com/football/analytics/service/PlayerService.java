package com.football.analytics.service;

import com.football.analytics.dto.ComparisonDto;
import com.football.analytics.exception.ResourceNotFoundException;
import com.football.analytics.model.Player;
import com.football.analytics.model.PlayerSeasonStats;
import com.football.analytics.model.ShotEvent;
import com.football.analytics.repository.PostgresRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PlayerService {
    private static final Logger log = LoggerFactory.getLogger(PlayerService.class);

    private final PostgresRepository PostgresRepository;

    public PlayerService(PostgresRepository PostgresRepository) {
        this.PostgresRepository = PostgresRepository;
    }

    public List<Player> getAllPlayers(String query, String position) {
        try {
            List<Player> players = PostgresRepository.getAllPlayers(query, position);
            return players != null ? players : Collections.emptyList();
        } catch (Exception e) {
            log.error("ClickHouse players query failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Player getPlayerById(Long id) {
        try {
            return PostgresRepository.getPlayer(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "PLAYER_NOT_FOUND", "Player with id " + id + " not found."));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("ClickHouse getPlayer query failed: {}", e.getMessage());
            throw new ResourceNotFoundException(
                "PLAYER_NOT_FOUND", "Player with id " + id + " not found.");
        }
    }

    public PlayerSeasonStats getPlayerStats(Long playerId, Long seasonId) {
        getPlayerById(playerId);
        try {
            return PostgresRepository.getPlayerStats(playerId, seasonId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "STATS_NOT_FOUND",
                    "Statistics for player " + playerId + " in season " + seasonId + " not found."));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("ClickHouse getPlayerStats query failed: {}", e.getMessage());
            throw new ResourceNotFoundException(
                "STATS_NOT_FOUND",
                "Statistics for player " + playerId + " in season " + seasonId + " not found.");
        }
    }

    public List<ShotEvent> getPlayerShots(Long playerId, Long seasonId) {
        getPlayerById(playerId);
        try {
            List<ShotEvent> shots = PostgresRepository.getPlayerShots(playerId, seasonId);
            return shots != null ? shots : Collections.emptyList();
        } catch (Exception e) {
            log.error("ClickHouse getPlayerShots query failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public ComparisonDto comparePlayers(List<Long> ids, Long seasonId) {
        if (ids == null || ids.size() < 2) {
            throw new IllegalArgumentException("At least 2 player IDs are required for comparison.");
        }

        List<Player> playerList = new ArrayList<>();
        List<PlayerSeasonStats> statsList = new ArrayList<>();

        for (Long id : ids) {
            Player player = getPlayerById(id);
            PlayerSeasonStats stats = getPlayerStats(id, seasonId);
            playerList.add(player);
            statsList.add(stats);
        }

        Map<String, ComparisonDto.MetricComparison> metricMap = new LinkedHashMap<>();
        Player p1 = playerList.get(0);
        Player p2 = playerList.get(1);
        PlayerSeasonStats s1 = statsList.get(0);
        PlayerSeasonStats s2 = statsList.get(1);

        addMetric(metricMap, "Goals per 90", s1.getGoalsPer90(), s2.getGoalsPer90(), p1.getName(), p2.getName());
        addMetric(metricMap, "Assists per 90", s1.getAssistsPer90(), s2.getAssistsPer90(), p1.getName(), p2.getName());
        addMetric(metricMap, "xG per 90", s1.getXgPer90(), s2.getXgPer90(), p1.getName(), p2.getName());
        addMetric(metricMap, "xA per 90", s1.getXaPer90(), s2.getXaPer90(), p1.getName(), p2.getName());
        addMetric(metricMap, "Shots per 90", s1.getShotsPer90(), s2.getShotsPer90(), p1.getName(), p2.getName());
        addMetric(metricMap, "Pass Accuracy %", s1.getPassCompletionRate(), s2.getPassCompletionRate(), p1.getName(), p2.getName());
        addMetric(metricMap, "Duel Win %", s1.getDuelWinRate(), s2.getDuelWinRate(), p1.getName(), p2.getName());
        addMetric(metricMap, "Finishing Rating", (double) s1.getFinishingRating(), (double) s2.getFinishingRating(), p1.getName(), p2.getName());
        addMetric(metricMap, "Creation Rating", (double) s1.getCreationRating(), (double) s2.getCreationRating(), p1.getName(), p2.getName());
        addMetric(metricMap, "Progression Rating", (double) s1.getProgressionRating(), (double) s2.getProgressionRating(), p1.getName(), p2.getName());

        return new ComparisonDto(playerList, statsList, metricMap);
    }

    private void addMetric(Map<String, ComparisonDto.MetricComparison> map, String name,
                           Double v1, Double v2, String name1, String name2) {
        double left = v1 != null ? v1 : 0.0;
        double right = v2 != null ? v2 : 0.0;
        String leader = left > right ? name1 : (right > left ? name2 : "Tied");
        double diff = Math.round(Math.abs(left - right) * 100.0) / 100.0;
        map.put(name, new ComparisonDto.MetricComparison(name, left, right, leader, diff));
    }
}
