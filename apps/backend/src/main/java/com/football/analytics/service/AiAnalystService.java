package com.football.analytics.service;

import com.football.analytics.dto.AiAnalysisRequest;
import com.football.analytics.dto.AiAnalysisResponse;
import com.football.analytics.model.Player;
import com.football.analytics.model.PlayerSeasonStats;
import com.football.analytics.repository.PostgresRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI Analyst â€” ClickHouse-only. Never invents stats.
 * If data is missing, returns an explicit unavailable response.
 */
@Service
public class AiAnalystService {
    private final PostgresRepository PostgresRepository;

    public AiAnalystService(PostgresRepository PostgresRepository) {
        this.PostgresRepository = PostgresRepository;
    }

    public AiAnalysisResponse analyze(AiAnalysisRequest request) {
        String query = request.getQuery() != null ? request.getQuery().trim() : "";
        Long playerId = request.getPlayerId();

        if (playerId != null) {
            return analyzePlayerById(playerId, query);
        }

        if (!query.isBlank()) {
            List<Player> matches = PostgresRepository.getAllPlayers(extractSearchTerm(query), null);
            if (matches != null && !matches.isEmpty()) {
                Player player = matches.get(0);
                return analyzePlayerById(player.getPlayerId(), query);
            }
        }

        return unavailableResponse(query);
    }

    private AiAnalysisResponse analyzePlayerById(Long playerId, String query) {
        Optional<Player> playerOpt = PostgresRepository.getPlayer(playerId);
        if (playerOpt.isEmpty()) {
            return unavailableResponse(query);
        }

        Player player = playerOpt.get();
        Optional<PlayerSeasonStats> statsOpt = PostgresRepository.getPlayerStats(playerId, null);
        if (statsOpt.isEmpty()) {
            return unavailableResponse(query);
        }

        PlayerSeasonStats stats = statsOpt.get();
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("Player", player.getName());
        table.put("Team", player.getTeamName());
        table.put("Season", stats.getSeasonName());
        table.put("Matches", stats.getMatchesPlayed());
        table.put("Minutes", stats.getMinutes());
        table.put("Goals", stats.getGoals());
        table.put("Assists", stats.getAssists());
        table.put("xG", stats.getXg());
        table.put("xA", stats.getXa());
        table.put("Goals/90", stats.getGoalsPer90());
        table.put("Assists/90", stats.getAssistsPer90());
        table.put("xG/90", stats.getXgPer90());
        table.put("Finishing Rating", stats.getFinishingRating());
        table.put("Creation Rating", stats.getCreationRating());

        List<String> facts = List.of(
            player.getName() + " (" + player.getTeamName() + "): "
                + stats.getGoals() + " goals, " + stats.getAssists() + " assists.",
            "xG=" + stats.getXg() + ", xA=" + stats.getXa()
                + " across " + stats.getMatchesPlayed() + " matches (" + stats.getMinutes() + " mins).",
            "Per-90: goals=" + stats.getGoalsPer90()
                + ", assists=" + stats.getAssistsPer90()
                + ", xG=" + stats.getXgPer90() + "."
        );

        String answer = "### PhĂ¢n tĂ­ch tá»« ClickHouse: " + player.getName() + "\n\n"
            + "Dá»¯ liá»‡u láº¥y tá»« `mart_player_season_stats` / `fact_player_match` (khĂ´ng Æ°á»›c lÆ°á»£ng).\n\n"
            + "- BĂ n tháº¯ng: **" + stats.getGoals() + "** (xG " + stats.getXg() + ")\n"
            + "- Kiáº¿n táº¡o: **" + stats.getAssists() + "** (xA " + stats.getXa() + ")\n"
            + "- PhĂºt thi Ä‘áº¥u: **" + stats.getMinutes() + "** qua **"
            + stats.getMatchesPlayed() + "** tráº­n\n"
            + "- Goals/90: **" + stats.getGoalsPer90() + "**, Assists/90: **"
            + stats.getAssistsPer90() + "**";

        List<String> suggestions = List.of(
            "Xem shot map cá»§a " + player.getName() + "?",
            "So sĂ¡nh " + player.getName() + " vá»›i cáº§u thá»§ khĂ¡c trong cĂ¹ng mĂ¹a?"
        );

        return new AiAnalysisResponse(
            "PLAYER_STATS_FROM_CLICKHOUSE",
            answer,
            facts,
            table,
            "mart_player_season_stats / fact_player_match (ClickHouse)",
            0.99,
            suggestions
        );
    }

    private AiAnalysisResponse unavailableResponse(String query) {
        String q = query == null || query.isBlank() ? "(empty)" : query;
        List<String> facts = List.of(
            "KhĂ´ng cĂ³ sá»‘ liá»‡u ClickHouse khá»›p vá»›i truy váº¥n.",
            "Há»‡ thá»‘ng khĂ´ng bá»‹a thá»‘ng kĂª khi dá»¯ liá»‡u thiáº¿u."
        );
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("query", q);
        table.put("status", "DATA_UNAVAILABLE");

        String answer = "### Dá»¯ liá»‡u khĂ´ng kháº£ dá»¥ng\n\n"
            + "KhĂ´ng tĂ¬m tháº¥y sá»‘ liá»‡u tháº­t trong ClickHouse cho: *\"" + q + "\"*.\n"
            + "Vui lĂ²ng thá»­ tĂªn cáº§u thá»§ Ä‘Ă£ Ä‘Æ°á»£c ingest (vĂ­ dá»¥ tá»« La Liga StatsBomb open data), "
            + "hoáº·c kiá»ƒm tra `/api/v1/system/status` Ä‘á»ƒ xĂ¡c nháº­n ClickHouse ONLINE.";

        return new AiAnalysisResponse(
            "DATA_UNAVAILABLE",
            answer,
            facts,
            table,
            "ClickHouse",
            0.0,
            List.of("GET /api/v1/players", "GET /api/v1/matches", "GET /api/v1/system/status")
        );
    }

    private String extractSearchTerm(String query) {
        String cleaned = query.toLowerCase()
            .replaceAll("(?i)\\b(phĂ¢n tĂ­ch|analyze|compare|so sĂ¡nh|cá»§a|cáº§u thá»§|player|stats|thá»‘ng kĂª)\\b", " ")
            .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        if (cleaned.isBlank()) {
            return query.trim();
        }
        // Prefer the longest token-ish phrase (likely the name)
        return cleaned;
    }
}
