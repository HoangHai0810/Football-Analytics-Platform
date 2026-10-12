package com.football.analytics.service;

import com.football.analytics.dto.AiAnalysisRequest;
import com.football.analytics.dto.AiAnalysisResponse;
import com.football.analytics.model.Player;
import com.football.analytics.model.PlayerSeasonStats;
import com.football.analytics.repository.PostgresRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI Analyst — data-backed only. Never invents stats.
 */
@Service
public class AiAnalystService {
    private final PostgresRepository postgresRepository;

    public AiAnalystService(PostgresRepository postgresRepository) {
        this.postgresRepository = postgresRepository;
    }

    public AiAnalysisResponse analyze(AiAnalysisRequest request) {
        String query = request.getQuery() != null ? request.getQuery().trim() : "";
        Long playerId = request.getPlayerId();
        Long matchId = request.getMatchId();

        if (matchId != null) {
            return analyzeMatchById(matchId, query);
        }

        if (playerId != null) {
            return analyzePlayerById(playerId, query);
        }

        if (!query.isBlank()) {
            // Check if user specifically requested a player
            String searchTerm = extractSearchTerm(query);
            if (!searchTerm.isBlank()) {
                List<Player> matches = postgresRepository.getAllPlayers(searchTerm, null);
                if (matches != null && !matches.isEmpty()) {
                    Player player = matches.get(0);
                    return analyzePlayerById(player.getPlayerId(), query);
                }
            }

            // Check if query mentions a team or asks about matches/overview
            return analyzeGeneralOrTeam(query);
        }

        return unavailableResponse(query);
    }

    private AiAnalysisResponse analyzeMatchById(Long matchId, String query) {
        Optional<com.football.analytics.model.Match> matchOpt = postgresRepository.getMatch(matchId);
        if (matchOpt.isEmpty()) {
            return unavailableResponse(query);
        }

        com.football.analytics.model.Match m = matchOpt.get();
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("Giải đấu", m.getCompetitionName());
        table.put("Trận đấu", m.getHomeTeamName() + " vs " + m.getAwayTeamName());
        table.put("Tỷ số", m.getHomeScore() + " – " + m.getAwayScore());
        table.put("Trạng thái", m.getStatus());
        if (m.getHomeXg() != null && m.getAwayXg() != null) {
            table.put("xG", m.getHomeXg() + " vs " + m.getAwayXg());
        }
        if (m.getStadium() != null && !m.getStadium().isBlank()) {
            table.put("Sân vận động", m.getStadium());
        }

        List<String> facts = new ArrayList<>();
        facts.add(m.getHomeTeamName() + " (" + m.getHomeScore() + ") vs " + m.getAwayTeamName() + " (" + m.getAwayScore() + ") · " + m.getStatus());
        if (m.getHomeXg() != null && m.getAwayXg() != null) {
            facts.add("Expected Goals (xG): " + m.getHomeTeamName() + " " + m.getHomeXg() + " - " + m.getAwayXg() + " " + m.getAwayTeamName());
        }
        if (m.getMatchDate() != null) {
            facts.add("Thời gian: " + m.getMatchDate().toString().replace('T', ' '));
        }

        StringBuilder answer = new StringBuilder();
        answer.append("### ⚽ Phân tích trận đấu: ").append(m.getHomeTeamName()).append(" vs ").append(m.getAwayTeamName()).append("\n\n");
        answer.append("**Giải đấu:** ").append(m.getCompetitionName()).append("\n\n");
        answer.append("- **Tỷ số:** `").append(m.getHomeScore()).append(" – ").append(m.getAwayScore()).append("` (").append(m.getStatus()).append(")\n");
        if (m.getHomeXg() != null && m.getAwayXg() != null) {
            answer.append("- **xG (Kỳ vọng bàn thắng):** `").append(m.getHomeXg()).append("` (Chủ nhà) vs `").append(m.getAwayXg()).append("` (Đội khách)\n");
            double diff = m.getHomeXg() - m.getAwayXg();
            if (Math.abs(diff) < 0.3) {
                answer.append("  ↳ *Đánh giá:* Thế trận tương đối giằng co về chất lượng cơ hội tạo ra.\n");
            } else if (diff > 0) {
                answer.append("  ↳ *Đánh giá:* ").append(m.getHomeTeamName()).append(" kiểm soát tốt hơn các cơ hội nguy hiểm.\n");
            } else {
                answer.append("  ↳ *Đánh giá:* ").append(m.getAwayTeamName()).append(" vượt trội về chất lượng cơ hội dứt điểm.\n");
            }
        }
        if (m.getStadium() != null && !m.getStadium().isBlank()) {
            answer.append("- **Sân vận động:** ").append(m.getStadium()).append("\n");
        }

        List<String> suggestions = List.of(
            "Phân tích đội " + m.getHomeTeamName(),
            "Phân tích đội " + m.getAwayTeamName(),
            "Xem các trận đấu gần đây"
        );

        return new AiAnalysisResponse(
            "MATCH_ANALYSIS",
            answer.toString(),
            facts,
            table,
            "dim_match / fact_match",
            0.99,
            suggestions
        );
    }

    private AiAnalysisResponse analyzeTeamMatches(Long teamId, String teamName, List<com.football.analytics.model.Match> allMatches) {
        List<com.football.analytics.model.Match> teamMatches = new ArrayList<>();
        int goalsScored = 0;
        int goalsConceded = 0;
        int wins = 0;
        int draws = 0;
        int losses = 0;

        for (com.football.analytics.model.Match m : allMatches) {
            boolean isHome = teamName.equalsIgnoreCase(m.getHomeTeamName());
            boolean isAway = teamName.equalsIgnoreCase(m.getAwayTeamName());
            if (isHome || isAway) {
                teamMatches.add(m);
                if ("FINISHED".equalsIgnoreCase(m.getStatus())) {
                    int sc = isHome ? m.getHomeScore() : m.getAwayScore();
                    int cc = isHome ? m.getAwayScore() : m.getHomeScore();
                    goalsScored += sc;
                    goalsConceded += cc;
                    if (sc > cc) wins++;
                    else if (sc == cc) draws++;
                    else losses++;
                }
            }
        }

        Map<String, Object> table = new LinkedHashMap<>();
        table.put("Đội bóng", teamName);
        table.put("Số trận trong kho", teamMatches.size());
        table.put("Thắng - Hòa - Thua", wins + " - " + draws + " - " + losses);
        table.put("Bàn thắng / Bàn thua", goalsScored + " / " + goalsConceded);

        StringBuilder sb = new StringBuilder();
        sb.append("### 📊 Thống kê phong độ: ").append(teamName).append("\n\n");
        sb.append("Dữ liệu phân tích từ các trận đấu đã đồng bộ trong hệ thống:\n\n");
        sb.append("- **Tổng số trận đã ghi nhận:** ").append(teamMatches.size()).append(" trận\n");
        sb.append("- **Kết quả (các trận đã kết thúc):** ").append(wins).append(" Thắng · ")
          .append(draws).append(" Hòa · ").append(losses).append(" Thua\n");
        sb.append("- **Hiệu số bàn thắng:** ").append(goalsScored).append(" bàn thắng ghi được, ")
          .append(goalsConceded).append(" bàn thua.\n\n");

        if (!teamMatches.isEmpty()) {
            sb.append("**Các trận đấu đáng chú ý:**\n");
            int count = 0;
            for (com.football.analytics.model.Match tm : teamMatches) {
                if (count++ >= 4) break;
                sb.append("- ").append(tm.getHomeTeamName()).append(" `")
                  .append(tm.getHomeScore()).append(" - ").append(tm.getAwayScore()).append("` ")
                  .append(tm.getAwayTeamName()).append(" (").append(tm.getStatus()).append(")\n");
            }
        }

        List<String> facts = List.of(
            teamName + ": " + wins + "W-" + draws + "D-" + losses + "L trong " + (wins + draws + losses) + " trận hoàn tất.",
            "Tổng bàn thắng: " + goalsScored + ", bàn thua: " + goalsConceded
        );

        List<String> suggestions = List.of(
            "Trận đấu gần nhất của " + teamName,
            "Cầu thủ của " + teamName,
            "Xem các trận đấu hôm nay"
        );

        return new AiAnalysisResponse(
            "TEAM_ANALYSIS",
            sb.toString(),
            facts,
            table,
            "dim_match / fact_match",
            0.95,
            suggestions
        );
    }

    private AiAnalysisResponse summarizeRecentMatches(List<com.football.analytics.model.Match> matches) {
        int liveCount = 0;
        int scheduledCount = 0;
        int finishedCount = 0;
        for (com.football.analytics.model.Match m : matches) {
            if ("LIVE".equalsIgnoreCase(m.getStatus())) liveCount++;
            else if ("SCHEDULED".equalsIgnoreCase(m.getStatus())) scheduledCount++;
            else finishedCount++;
        }

        Map<String, Object> table = new LinkedHashMap<>();
        table.put("Tổng số trận", matches.size());
        table.put("Đã kết thúc", finishedCount);
        table.put("Trực tiếp (LIVE)", liveCount);
        table.put("Sắp diễn ra", scheduledCount);

        StringBuilder sb = new StringBuilder();
        sb.append("### 🏆 Tổng quan lịch & Kết quả trận đấu\n\n");
        sb.append("Hệ thống hiện ghi nhận **").append(matches.size()).append("** trận đấu qua các giải đấu:\n\n");
        sb.append("- **Trực tiếp (LIVE):** `").append(liveCount).append("` trận\n");
        sb.append("- **Sắp diễn ra:** `").append(scheduledCount).append("` trận\n");
        sb.append("- **Đã kết thúc:** `").append(finishedCount).append("` trận\n\n");

        sb.append("**Các trận đấu mới nhất:**\n");
        int count = 0;
        for (com.football.analytics.model.Match m : matches) {
            if (count++ >= 5) break;
            sb.append("- **").append(m.getCompetitionName()).append("**: ")
              .append(m.getHomeTeamName()).append(" `")
              .append(m.getHomeScore()).append(" – ").append(m.getAwayScore()).append("` ")
              .append(m.getAwayTeamName()).append(" *(").append(m.getStatus()).append(")*\n");
        }

        List<String> facts = List.of(
            "Cập nhật " + matches.size() + " trận từ kho dữ liệu phân tích.",
            "Bao gồm các giải đấu hàng đầu: Ngoại Hạng Anh, La Liga, Serie A, Champions League..."
        );

        List<String> suggestions = List.of(
            "Phân tích trận đấu trực tiếp",
            "Top ghi bàn",
            "Xem danh sách cầu thủ"
        );

        return new AiAnalysisResponse(
            "SYSTEM_OVERVIEW",
            sb.toString(),
            facts,
            table,
            "dim_match / dim_competition",
            0.98,
            suggestions
        );
    }

    private AiAnalysisResponse analyzeGeneralOrTeam(String query) {
        List<com.football.analytics.model.Match> matches = postgresRepository.getAllMatches(null, null, null, null, null, 100);
        String qLower = query.toLowerCase();

        // Check if a team is mentioned in query
        if (matches != null) {
            for (com.football.analytics.model.Match m : matches) {
                String home = m.getHomeTeamName().toLowerCase();
                String away = m.getAwayTeamName().toLowerCase();
                if (!home.isBlank() && home.length() > 3 && qLower.contains(home)) {
                    return analyzeTeamMatches(m.getHomeTeamId(), m.getHomeTeamName(), matches);
                }
                if (!away.isBlank() && away.length() > 3 && qLower.contains(away)) {
                    return analyzeTeamMatches(m.getAwayTeamId(), m.getAwayTeamName(), matches);
                }
            }

            // Check if query is asking about matches or summary
            if (qLower.contains("trận") || qLower.contains("match") || qLower.contains("gần nhất") ||
                qLower.contains("lịch") || qLower.contains("hôm nay") || qLower.contains("tổng quan") ||
                qLower.contains("kết quả") || qLower.contains("giải")) {
                return summarizeRecentMatches(matches);
            }
        }

        return unavailableResponse(query);
    }

    private AiAnalysisResponse analyzePlayerById(Long playerId, String query) {
        Optional<Player> playerOpt = postgresRepository.getPlayer(playerId);
        if (playerOpt.isEmpty()) {
            return unavailableResponse(query);
        }

        Player player = playerOpt.get();
        Optional<PlayerSeasonStats> statsOpt = postgresRepository.getPlayerStats(playerId, null);
        if (statsOpt.isEmpty()) {
            // Player exists, but has no aggregated stats yet (e.g. from squad sync)
            Map<String, Object> table = new LinkedHashMap<>();
            table.put("Cầu thủ", player.getName());
            table.put("Đội bóng", player.getTeamName());
            table.put("Vị trí", player.getPosition());
            table.put("Số áo", player.getJerseyNumber() > 0 ? "#" + player.getJerseyNumber() : "—");
            table.put("Quốc tịch", player.getNationality());

            String answer = "### 👤 Thông tin cầu thủ: " + player.getName() + "\n\n"
                + "- **Đội bóng:** " + (player.getTeamName().isBlank() ? "—" : player.getTeamName()) + "\n"
                + "- **Vị trí:** " + (player.getPosition().isBlank() ? "—" : player.getPosition()) + "\n"
                + "- **Số áo:** " + (player.getJerseyNumber() > 0 ? "#" + player.getJerseyNumber() : "—") + "\n"
                + "- **Quốc tịch:** " + (player.getNationality().isBlank() ? "—" : player.getNationality()) + "\n\n"
                + "*Ghi chú:* Cầu thủ đã được ghi nhận trong đội hình thi đấu chính thức. "
                + "Dữ liệu sự kiện chi tiết mùa giải sẽ được bổ sung khi trận đấu diễn ra hoặc chạy full backfill.";

            List<String> facts = List.of(
                player.getName() + " thuộc biên chế " + player.getTeamName() + ", vị trí: " + player.getPosition()
            );

            return new AiAnalysisResponse(
                "PLAYER_PROFILE",
                answer,
                facts,
                table,
                "dim_player",
                0.95,
                List.of("So sánh với cầu thủ khác", "Xem danh sách cầu thủ")
            );
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

        List<String> facts = List.of(
            player.getName() + " (" + player.getTeamName() + "): "
                + stats.getGoals() + " goals, " + stats.getAssists() + " assists.",
            "xG=" + stats.getXg() + ", xA=" + stats.getXa()
                + " across " + stats.getMatchesPlayed() + " matches (" + stats.getMinutes() + " mins).",
            "Per-90: goals=" + stats.getGoalsPer90()
                + ", assists=" + stats.getAssistsPer90()
                + ", xG=" + stats.getXgPer90() + "."
        );

        String answer = "### 🌟 Phân tích: " + player.getName() + "\n\n"
            + "Nguồn: `mart_player_season_stats` / `fact_player_match` (dữ liệu thật, không ước lượng).\n\n"
            + "- **Bàn thắng:** `" + stats.getGoals() + "` (xG " + stats.getXg() + ")\n"
            + "- **Kiến tạo:** `" + stats.getAssists() + "` (xA " + stats.getXa() + ")\n"
            + "- **Phút thi đấu:** `" + stats.getMinutes() + "` qua `"
            + stats.getMatchesPlayed() + "` trận\n"
            + "- **Goals/90:** `" + stats.getGoalsPer90() + "`, **Assists/90:** `"
            + stats.getAssistsPer90() + "`";

        List<String> suggestions = List.of(
            "Xem bản đồ sút của " + player.getName(),
            "So sánh " + player.getName() + " với cầu thủ khác",
            "Phân tích đội " + player.getTeamName()
        );

        return new AiAnalysisResponse(
            "PLAYER_STATS",
            answer,
            facts,
            table,
            "mart_player_season_stats / fact_player_match",
            0.99,
            suggestions
        );
    }

    private AiAnalysisResponse unavailableResponse(String query) {
        String q = query == null || query.isBlank() ? "(trống)" : query;
        List<String> facts = List.of(
            "Không có số liệu khớp chính xác với truy vấn.",
            "Hệ thống tuân thủ nguyên tắc không bịa đặt số liệu thống kê."
        );
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("Truy vấn", q);
        table.put("Trạng thái", "DATA_UNAVAILABLE");

        String answer = "### ℹ️ Không tìm thấy số liệu cho: *\"" + q + "\"*\n\n"
            + "Hệ thống chỉ phân tích dựa trên dữ liệu thật có trong database. Bạn có thể:\n"
            + "- Hỏi về các đội bóng (ví dụ: *Real Madrid*, *Juventus*, *Arsenal*...)\n"
            + "- Bấm nút **'⚡ Hỏi AI phân tích trận này'** trực tiếp từ màn hình Trận Đấu\n"
            + "- Tra cứu tên cầu thủ cụ thể từ màn hình Cầu Thủ\n"
            + "- Hoặc chọn các câu hỏi gợi ý bên dưới.";

        return new AiAnalysisResponse(
            "DATA_UNAVAILABLE",
            answer,
            facts,
            table,
            "PostgreSQL Analytics",
            0.0,
            List.of("Xem các trận đấu hôm nay", "Tổng quan giải đấu", "Phân tích đội Real Madrid")
        );
    }

    private String extractSearchTerm(String query) {
        String cleaned = query.toLowerCase()
            .replaceAll("(?i)\\b(phân tích|analyze|compare|so sánh|của|cầu thủ|player|stats|thống kê|xem|cho tôi biết)\\b", " ")
            .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        if (cleaned.isBlank()) {
            return query.trim();
        }
        return cleaned;
    }
}
