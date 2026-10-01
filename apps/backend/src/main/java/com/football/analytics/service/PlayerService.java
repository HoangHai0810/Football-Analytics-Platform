package com.football.analytics.service;

import com.football.analytics.dto.ComparisonDto;
import com.football.analytics.exception.ResourceNotFoundException;
import com.football.analytics.model.Player;
import com.football.analytics.model.PlayerSeasonStats;
import com.football.analytics.model.ShotEvent;
import com.football.analytics.repository.ClickHouseRepository;
import com.football.analytics.repository.SeedDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PlayerService {
    private static final Logger log = LoggerFactory.getLogger(PlayerService.class);

    private final ClickHouseRepository clickHouseRepository;
    private final SeedDataStore seedDataStore;

    public PlayerService(ClickHouseRepository clickHouseRepository, SeedDataStore seedDataStore) {
        this.clickHouseRepository = clickHouseRepository;
        this.seedDataStore = seedDataStore;
    }

    public List<Player> getAllPlayers(String query, String position) {
        try {
            List<Player> players = clickHouseRepository.getAllPlayers(query, position);
            if (players != null && !players.isEmpty()) {
                return players;
            }
        } catch (Exception e) {
            log.warn("ClickHouse players query failed: {}", e.getMessage());
        }
        return seedDataStore.getAllPlayers(query, position);
    }

    public Player getPlayerById(Long id) {
        try {
            var p = clickHouseRepository.getPlayer(id);
            if (p.isPresent()) {
                return p.get();
            }
        } catch (Exception e) {
            log.warn("ClickHouse getPlayer query failed: {}", e.getMessage());
        }
        return seedDataStore.getPlayer(id)
            .orElseThrow(() -> new ResourceNotFoundException("PLAYER_NOT_FOUND", "Player with id " + id + " not found."));
    }

    public PlayerSeasonStats getPlayerStats(Long playerId, Long seasonId) {
        getPlayerById(playerId); // validate player exists
        try {
            var stats = clickHouseRepository.getPlayerStats(playerId, seasonId);
            if (stats.isPresent()) {
                return stats.get();
            }
        } catch (Exception e) {
            log.warn("ClickHouse getPlayerStats query failed: {}", e.getMessage());
        }
        return seedDataStore.getPlayerStats(playerId, seasonId)
            .orElseThrow(() -> new ResourceNotFoundException("STATS_NOT_FOUND", "Statistics for player " + playerId + " in season " + seasonId + " not found."));
    }

    public List<ShotEvent> getPlayerShots(Long playerId, Long seasonId) {
        getPlayerById(playerId);
        try {
            List<ShotEvent> shots = clickHouseRepository.getPlayerShots(playerId, seasonId);
            if (shots != null && !shots.isEmpty()) {
                return shots;
            }
        } catch (Exception e) {
            log.warn("ClickHouse getPlayerShots query failed: {}", e.getMessage());
        }
        return seedDataStore.getPlayerShots(playerId, seasonId);
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
        Double fin1 = s1.getFinishingRating() != null ? s1.getFinishingRating().doubleValue() : 50.0;
        Double fin2 = s2.getFinishingRating() != null ? s2.getFinishingRating().doubleValue() : 50.0;
        addMetric(metricMap, "Finishing Rating", fin1, fin2, p1.getName(), p2.getName());

        Double cre1 = s1.getCreationRating() != null ? s1.getCreationRating().doubleValue() : 50.0;
        Double cre2 = s2.getCreationRating() != null ? s2.getCreationRating().doubleValue() : 50.0;
        addMetric(metricMap, "Creation Rating", cre1, cre2, p1.getName(), p2.getName());

        Double pro1 = s1.getProgressionRating() != null ? s1.getProgressionRating().doubleValue() : 50.0;
        Double pro2 = s2.getProgressionRating() != null ? s2.getProgressionRating().doubleValue() : 50.0;
        addMetric(metricMap, "Progression Rating", pro1, pro2, p1.getName(), p2.getName());

        return new ComparisonDto(playerList, statsList, metricMap);
    }

    private void addMetric(Map<String, ComparisonDto.MetricComparison> map, String name,
                           Double v1, Double v2, String name1, String name2) {
        double val1 = v1 != null ? v1 : 0.0;
        double val2 = v2 != null ? v2 : 0.0;
        String leader = val1 > val2 ? name1 : (val2 > val1 ? name2 : "Tied");
        double diff = Math.round(Math.abs(val1 - val2) * 100.0) / 100.0;
        map.put(name, new ComparisonDto.MetricComparison(name, val1, val2, leader, diff));
    }
}

