package com.football.analytics.service;

import com.football.analytics.dto.AiAnalysisRequest;
import com.football.analytics.dto.AiAnalysisResponse;
import com.football.analytics.model.Player;
import com.football.analytics.model.PlayerSeasonStats;
import com.football.analytics.repository.ClickHouseRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI Analyst — ClickHouse-only. Never invents stats.
 * If data is missing, returns an explicit unavailable response.
 */
@Service
public class AiAnalystService {
    private final ClickHouseRepository clickHouseRepository;

    public AiAnalystService(ClickHouseRepository clickHouseRepository) {
        this.clickHouseRepository = clickHouseRepository;
    }

    public AiAnalysisResponse analyze(AiAnalysisRequest request) {
        String query = request.getQuery() != null ? request.getQuery().trim() : "";
        Long playerId = request.getPlayerId();

        if (playerId != null) {
            return analyzePlayerById(playerId, query);
        }

        if (!query.isBlank()) {
            List<Player> matches = clickHouseRepository.getAllPlayers(extractSearchTerm(query), null);
            if (matches != null && !matches.isEmpty()) {
                Player player = matches.get(0);
                return analyzePlayerById(player.getPlayerId(), query);
            }
        }

        return unavailableResponse(query);
    }

    private AiAnalysisResponse analyzePlayerById(Long playerId, String query) {
        Optional<Player> playerOpt = clickHouseRepository.getPlayer(playerId);
        if (playerOpt.isEmpty()) {
            return unavailableResponse(query);
        }

        Player player = playerOpt.get();
        Optional<PlayerSeasonStats> statsOpt = clickHouseRepository.getPlayerStats(playerId, null);
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

        String answer = "### Phân tích từ ClickHouse: " + player.getName() + "\n\n"
            + "Dữ liệu lấy từ `mart_player_season_stats` / `fact_player_match` (không ước lượng).\n\n"
            + "- Bàn thắng: **" + stats.getGoals() + "** (xG " + stats.getXg() + ")\n"
            + "- Kiến tạo: **" + stats.getAssists() + "** (xA " + stats.getXa() + ")\n"
            + "- Phút thi đấu: **" + stats.getMinutes() + "** qua **"
            + stats.getMatchesPlayed() + "** trận\n"
            + "- Goals/90: **" + stats.getGoalsPer90() + "**, Assists/90: **"
            + stats.getAssistsPer90() + "**";

        List<String> suggestions = List.of(
            "Xem shot map của " + player.getName() + "?",
            "So sánh " + player.getName() + " với cầu thủ khác trong cùng mùa?"
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
            "Không có số liệu ClickHouse khớp với truy vấn.",
            "Hệ thống không bịa thống kê khi dữ liệu thiếu."
        );
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("query", q);
        table.put("status", "DATA_UNAVAILABLE");

        String answer = "### Dữ liệu không khả dụng\n\n"
            + "Không tìm thấy số liệu thật trong ClickHouse cho: *\"" + q + "\"*.\n"
            + "Vui lòng thử tên cầu thủ đã được ingest (ví dụ từ La Liga StatsBomb open data), "
            + "hoặc kiểm tra `/api/v1/system/status` để xác nhận ClickHouse ONLINE.";

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
            .replaceAll("(?i)\\b(phân tích|analyze|compare|so sánh|của|cầu thủ|player|stats|thống kê)\\b", " ")
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
