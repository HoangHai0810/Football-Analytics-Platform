package com.football.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.football.analytics.dto.AiAnalysisRequest;
import com.football.analytics.dto.AiAnalysisResponse;
import com.football.analytics.model.Player;
import com.football.analytics.model.PlayerSeasonStats;
import com.football.analytics.repository.PostgresRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * AI Analyst — Grounded football data analytics with LLM synthesis support.
 * Data from PostgreSQL / ClickHouse is the single source of truth.
 */
@Service
public class AiAnalystService {
    private final PostgresRepository postgresRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiAnalystService(PostgresRepository postgresRepository) {
        this.postgresRepository = postgresRepository;
    }

    public AiAnalysisResponse analyze(AiAnalysisRequest request) {
        String query = request.getQuery() != null ? request.getQuery().trim() : "";
        Long playerId = request.getPlayerId();
        Long matchId = request.getMatchId();

        String qLower = query.toLowerCase();

        // Check if query is explicitly asking about a different team, player, or competition,
        // which should take precedence over any stale matchId / playerId parameter
        boolean isAskingTeamOrComp = qLower.contains("phong độ") ||
                                     qLower.contains("giải đấu") ||
                                     qLower.contains("bảng xếp hạng") ||
                                     qLower.contains("cùng giải");

        if (matchId != null && !isAskingTeamOrComp && (query.isBlank() || qLower.contains("trận") || qLower.contains("vs"))) {
            return analyzeMatchById(matchId, query);
        }

        if (playerId != null && !isAskingTeamOrComp && (query.isBlank() || qLower.contains("cầu thủ") || qLower.contains("chỉ số"))) {
            return analyzePlayerById(playerId, query);
        }

        if (!query.isBlank()) {
            // 1. Check if query is asking about a specific match by two teams or match keywords
            AiAnalysisResponse matchAnalysis = tryFindMatchFromQuery(query);
            if (matchAnalysis != null) {
                return matchAnalysis;
            }

            // 2. Check if query mentions a team or asks about competition/matches/overview
            AiAnalysisResponse generalOrTeam = analyzeGeneralOrTeam(query);
            if (generalOrTeam != null && !"DATA_UNAVAILABLE".equals(generalOrTeam.getIntent())) {
                return generalOrTeam;
            }

            // 3. Check if user specifically requested a player
            String searchTerm = extractSearchTerm(query);
            if (!searchTerm.isBlank()) {
                List<Player> matches = postgresRepository.getAllPlayers(searchTerm, null);
                if (matches != null && !matches.isEmpty()) {
                    Player player = matches.get(0);
                    return analyzePlayerById(player.getPlayerId(), query);
                }
            }

            if (matchId != null) {
                return analyzeMatchById(matchId, query);
            }

            return generalOrTeam != null ? generalOrTeam : unavailableResponse(query);
        }

        if (matchId != null) {
            return analyzeMatchById(matchId, query);
        }

        if (playerId != null) {
            return analyzePlayerById(playerId, query);
        }

        return unavailableResponse(query);
    }

    private AiAnalysisResponse tryFindMatchFromQuery(String query) {
        String qLower = query.toLowerCase();
        List<com.football.analytics.model.Match> matches = postgresRepository.getAllMatches(null, null, null, null, null, 300);
        if (matches == null || matches.isEmpty()) return null;

        // 1. Check if both home and away teams are present in the query
        for (com.football.analytics.model.Match m : matches) {
            String h = cleanTeamName(m.getHomeTeamName());
            String a = cleanTeamName(m.getAwayTeamName());
            if (h.length() >= 3 && a.length() >= 3 && qLower.contains(h) && qLower.contains(a)) {
                return analyzeMatchById(m.getMatchId(), query);
            }
        }

        // 2. If query mentions a specific match ID
        if (qLower.matches(".*\\b\\d{4,9}\\b.*")) {
            for (com.football.analytics.model.Match m : matches) {
                if (query.contains(String.valueOf(m.getMatchId()))) {
                    return analyzeMatchById(m.getMatchId(), query);
                }
            }
        }

        return null;
    }

    private String cleanTeamName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
            .replaceAll("(?i)\\b(fc|cf|club|de|ac|as|rb)\\b", "")
            .replaceAll("[^\\p{L}\\p{N}\\s]", "")
            .replaceAll("\\s+", " ")
            .trim();
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
        table.put("Tỷ số chung cuộc", m.getHomeScore() + " – " + m.getAwayScore());
        table.put("Trạng thái", m.getStatus());
        if (m.getHomeXg() != null && m.getAwayXg() != null) {
            table.put("Expected Goals (xG)", m.getHomeXg() + " vs " + m.getAwayXg());
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
            facts.add("Thời gian thi đấu: " + m.getMatchDate().toString().replace('T', ' '));
        }
        facts.add("Giải đấu: " + m.getCompetitionName());

        StringBuilder answer = new StringBuilder();
        answer.append("### ⚽ Báo Cáo Phân Tích Chuyên Sâu: ").append(m.getHomeTeamName()).append(" vs ").append(m.getAwayTeamName()).append("\n\n");
        answer.append("🏆 **Giải đấu:** ").append(m.getCompetitionName()).append("\n");
        if (m.getStadium() != null && !m.getStadium().isBlank()) {
            answer.append("🏟️ **Sân vận động:** ").append(m.getStadium()).append("\n");
        }
        answer.append("\n---\n\n");

        answer.append("#### 1. Diễn biến & Tỷ số chung cuộc\n");
        answer.append("- **Tỷ số:** `").append(m.getHomeTeamName()).append(" ").append(m.getHomeScore())
              .append(" – ").append(m.getAwayScore()).append(" ").append(m.getAwayTeamName())
              .append("` *(").append(m.getStatus()).append(")*\n");

        if ("FINISHED".equalsIgnoreCase(m.getStatus())) {
            if (m.getHomeScore() > m.getAwayScore()) {
                if (m.getAwayScore() == 0) {
                    answer.append("- **Đánh giá thế trận:** ").append(m.getHomeTeamName())
                          .append(" đã phát huy triệt để lợi thế sân nhà để giành trọn 3 điểm, đồng thời giữ sạch lưới trước sức ép của ")
                          .append(m.getAwayTeamName()).append(".\n");
                } else {
                    answer.append("- **Đánh giá thế trận:** ").append(m.getHomeTeamName())
                          .append(" làm chủ thế trận tấn công và chuyển hóa cơ hội sắc bén hơn để giành chiến thắng chung cuộc.\n");
                }
            } else if (m.getHomeScore() < m.getAwayScore()) {
                answer.append("- **Đánh giá thế trận:** Chiến thắng thuyết phục cho đội khách ")
                      .append(m.getAwayTeamName()).append(", vượt qua áp lực sân khách với lối chơi kỷ luật và khả năng chớp thời cơ tốt.\n");
            } else {
                if (m.getHomeScore() == 0) {
                    answer.append("- **Đánh giá thế trận:** Trận hòa không bàn thắng (0-0) phản ánh thế trận chặt chẽ và sự thận trọng từ cả hai ban huấn luyện, ưu tiên sự an toàn nơi hàng phòng ngự.\n");
                } else {
                    answer.append("- **Đánh giá thế trận:** Màn rượt đuổi tỷ số kịch tính khép lại với kết quả hòa, chia đều điểm số cho hai đội bóng.\n");
                }
            }
        } else if ("LIVE".equalsIgnoreCase(m.getStatus())) {
            answer.append("- **Trận đấu đang diễn ra:** Hai đội đang tranh chấp quyết liệt từng mét vuông cỏ. Thế trận biến chuyển liên tục theo từng pha bóng.\n");
        } else {
            answer.append("- **Trận đấu sắp diễn ra:** Cuộc so tài giữa ").append(m.getHomeTeamName())
                  .append(" và ").append(m.getAwayTeamName()).append(" hứa hẹn mang đến nhiều bất ngờ chiến thuật.\n");
        }

        answer.append("\n#### 2. Phân tích Dữ liệu Cơ hội & Chỉ số xG\n");
        if (m.getHomeXg() != null && m.getAwayXg() != null && (m.getHomeXg() > 0 || m.getAwayXg() > 0)) {
            answer.append("- **Chỉ số bàn thắng kỳ vọng (xG):** `").append(m.getHomeTeamName()).append(" ").append(m.getHomeXg())
                  .append("` vs `").append(m.getAwayXg()).append(" ").append(m.getAwayTeamName()).append("`\n");
            double diff = m.getHomeXg() - m.getAwayXg();
            if (diff > 0.4) {
                answer.append("- **Nhận định cơ hội:** ").append(m.getHomeTeamName())
                      .append(" hoàn toàn áp đảo về chất lượng dứt điểm trong vòng cấm, tạo ra số cơ hội ăn bàn vượt trội.\n");
            } else if (diff < -0.4) {
                answer.append("- **Nhận định cơ hội:** ").append(m.getAwayTeamName())
                      .append(" tạo ra nhiều cơ hội mười mươi hơn, cho thấy tính đột biến cao từ các pha phản công.\n");
            } else {
                answer.append("- **Nhận định cơ hội:** Hai đội giằng co quyết liệt với chất lượng cơ hội tương đương nhau.\n");
            }
        } else {
            answer.append("- Trận đấu đề cao tính kỷ luật chiến thuật. Cả hai đội tập trung kiểm soát khu trung tuyến và hạn chế khoảng trống nguy hiểm trước khung thành.\n");
        }

        answer.append("\n#### 3. Tác động Cục diện & Triển vọng\n");
        answer.append("- Kết quả này có ý nghĩa quan trọng trong cuộc đua vị trí tại giải đấu **").append(m.getCompetitionName()).append("**.\n");
        answer.append("- ").append(m.getHomeTeamName()).append(" cần tiếp tục duy trì cự ly đội hình, trong khi ")
              .append(m.getAwayTeamName()).append(" cần cải thiện độ chính xác ở các tình huống quyết định ở 1/3 cuối sân đối phương.\n");

        List<String> suggestions = List.of(
            "Phân tích phong độ " + m.getHomeTeamName(),
            "Phân tích phong độ " + m.getAwayTeamName(),
            "Xem các trận đấu cùng giải " + m.getCompetitionName()
        );

        String llmAnswer = callOpenAiIfAvailable("Phân tích trận đấu giữa " + m.getHomeTeamName() + " và " + m.getAwayTeamName() + " (" + m.getHomeScore() + " - " + m.getAwayScore() + ")", facts);
        String finalAnswer = (llmAnswer != null && !llmAnswer.isBlank()) ? llmAnswer : answer.toString();
        String source = (llmAnswer != null && !llmAnswer.isBlank()) ? "OpenAI gpt-4o-mini + PitchPulse Lakehouse" : "PitchPulse Analytical Star-Schema";

        return new AiAnalysisResponse(
            "MATCH_ANALYSIS",
            finalAnswer,
            facts,
            table,
            source,
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
            String cleanT = cleanTeamName(teamName);
            boolean isHome = cleanTeamName(m.getHomeTeamName()).contains(cleanT) || cleanT.contains(cleanTeamName(m.getHomeTeamName()));
            boolean isAway = cleanTeamName(m.getAwayTeamName()).contains(cleanT) || cleanT.contains(cleanTeamName(m.getAwayTeamName()));
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
        table.put("Số trận ghi nhận", teamMatches.size());
        table.put("Thành tích (Thắng-Hòa-Thua)", wins + " - " + draws + " - " + losses);
        table.put("Bàn thắng / Bàn thua", goalsScored + " / " + goalsConceded);

        StringBuilder sb = new StringBuilder();
        sb.append("### 📊 Đánh Giá Phong Độ Toàn Diện: ").append(teamName).append("\n\n");
        sb.append("Dữ liệu phân tích phong độ từ các trận đấu đã ghi nhận trong cơ sở dữ liệu:\n\n");
        sb.append("- **Số trận đã đấu:** `").append(teamMatches.size()).append("` trận\n");
        sb.append("- **Hiệu suất thi đấu:** `").append(wins).append("` Thắng · `")
          .append(draws).append("` Hòa · `").append(losses).append("` Thua\n");
        sb.append("- **Hiệu số bàn thắng:** Ghi được `").append(goalsScored).append("` bàn, để lọt lưới `")
          .append(goalsConceded).append("` bàn (Hiệu số: `").append(goalsScored - goalsConceded > 0 ? "+" : "")
          .append(goalsScored - goalsConceded).append("`).\n\n");

        if (!teamMatches.isEmpty()) {
            sb.append("#### Các trận đấu gần đây:\n");
            int count = 0;
            for (com.football.analytics.model.Match tm : teamMatches) {
                if (count++ >= 5) break;
                sb.append("- ").append(tm.getHomeTeamName()).append(" `")
                  .append(tm.getHomeScore()).append(" – ").append(tm.getAwayScore()).append("` ")
                  .append(tm.getAwayTeamName()).append(" *(").append(tm.getStatus()).append(")*\n");
            }
        }

        List<String> facts = List.of(
            teamName + ": " + wins + " Thắng, " + draws + " Hòa, " + losses + " Thua.",
            "Tổng bàn thắng: " + goalsScored + ", bàn thua: " + goalsConceded,
            "Số trận theo dõi: " + teamMatches.size() + " trận."
        );

        List<String> suggestions = List.of(
            "Trận đấu gần nhất của " + teamName,
            "Xem cầu thủ của " + teamName,
            "Lịch thi đấu hôm nay"
        );

        String llmAnswer = callOpenAiIfAvailable("Phân tích phong độ toàn diện của đội bóng " + teamName, facts);
        String finalAnswer = (llmAnswer != null && !llmAnswer.isBlank()) ? llmAnswer : sb.toString();
        String source = (llmAnswer != null && !llmAnswer.isBlank()) ? "OpenAI gpt-4o-mini + PitchPulse Lakehouse" : "PitchPulse Analytical Lakehouse";

        return new AiAnalysisResponse(
            "TEAM_ANALYSIS",
            finalAnswer,
            facts,
            table,
            source,
            0.98,
            suggestions
        );
    }

    private AiAnalysisResponse summarizeCompetitionMatches(String compKey, List<com.football.analytics.model.Match> allMatches) {
        List<com.football.analytics.model.Match> compMatches = new ArrayList<>();
        String properCompName = "";
        for (com.football.analytics.model.Match m : allMatches) {
            if (m.getCompetitionName() != null && m.getCompetitionName().toLowerCase().contains(compKey)) {
                compMatches.add(m);
                if (properCompName.isEmpty()) properCompName = m.getCompetitionName();
            }
        }

        if (compMatches.isEmpty()) {
            return summarizeRecentMatches(allMatches);
        }

        int finished = 0;
        int live = 0;
        int scheduled = 0;
        for (com.football.analytics.model.Match m : compMatches) {
            if ("FINISHED".equalsIgnoreCase(m.getStatus())) finished++;
            else if ("LIVE".equalsIgnoreCase(m.getStatus())) live++;
            else scheduled++;
        }

        Map<String, Object> table = new LinkedHashMap<>();
        table.put("Giải đấu", properCompName);
        table.put("Tổng số trận ghi nhận", compMatches.size());
        table.put("Đã kết thúc", finished);
        table.put("Sắp diễn ra", scheduled);

        StringBuilder sb = new StringBuilder();
        sb.append("### 🏆 Báo Cáo Giải Đấu: ").append(properCompName).append("\n\n");
        sb.append("Tổng hợp các trận đấu đã đồng bộ trong cơ sở dữ liệu giải đấu **").append(properCompName).append("**:\n\n");
        sb.append("- **Trạng thái:** `").append(finished).append("` trận đã kết thúc · `").append(scheduled).append("` trận sắp khởi tranh\n\n");
        sb.append("#### Danh sách các trận đấu:\n");
        int count = 0;
        for (com.football.analytics.model.Match m : compMatches) {
            if (count++ >= 8) break;
            sb.append("- ").append(m.getHomeTeamName()).append(" `")
              .append(m.getHomeScore() != null ? m.getHomeScore() : 0).append(" – ")
              .append(m.getAwayScore() != null ? m.getAwayScore() : 0).append("` ")
              .append(m.getAwayTeamName()).append(" *(").append(m.getStatus()).append(")*\n");
        }

        List<String> facts = List.of(
            "Giải đấu " + properCompName + ": ghi nhận " + compMatches.size() + " trận đấu.",
            "Bao gồm " + finished + " trận đã có tỷ số và " + scheduled + " trận lịch sắp tới."
        );

        List<String> suggestions = List.of(
            "Phân tích phong độ các đội đầu bảng",
            "Xem lịch thi đấu hôm nay",
            "Top cầu thủ giải " + properCompName
        );

        String llmAnswer = callOpenAiIfAvailable("Tổng hợp diễn biến và kết quả giải đấu " + properCompName, facts);
        String finalAnswer = (llmAnswer != null && !llmAnswer.isBlank()) ? llmAnswer : sb.toString();
        String source = (llmAnswer != null && !llmAnswer.isBlank()) ? "OpenAI gpt-4o-mini + PitchPulse Lakehouse" : "PitchPulse Analytical Star-Schema";

        return new AiAnalysisResponse(
            "COMPETITION_ANALYSIS",
            finalAnswer,
            facts,
            table,
            source,
            0.99,
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
        table.put("Tổng số trận ghi nhận", matches.size());
        table.put("Đã kết thúc", finishedCount);
        table.put("Trực tiếp (LIVE)", liveCount);
        table.put("Sắp diễn ra", scheduledCount);

        StringBuilder sb = new StringBuilder();
        sb.append("### 🏆 Báo Cáo Tổng Quan Lịch Thi Đấu & Kết Quả\n\n");
        sb.append("Kho phân tích hiện đang theo dõi **").append(matches.size()).append("** trận đấu qua các giải đấu hàng đầu châu Âu và Nam Mỹ:\n\n");
        sb.append("- **Trực tiếp (LIVE):** `").append(liveCount).append("` trận đang tranh tài\n");
        sb.append("- **Sắp diễn ra:** `").append(scheduledCount).append("` trận chuẩn bị khởi tranh\n");
        sb.append("- **Đã kết thúc:** `").append(finishedCount).append("` trận đã có kết quả chung cuộc\n\n");

        sb.append("#### Điểm nhấn các trận mới nhất:\n");
        int count = 0;
        for (com.football.analytics.model.Match m : matches) {
            if (count++ >= 6) break;
            sb.append("- **").append(m.getCompetitionName()).append("**: ")
              .append(m.getHomeTeamName()).append(" `")
              .append(m.getHomeScore() != null ? m.getHomeScore() : 0).append(" – ")
              .append(m.getAwayScore() != null ? m.getAwayScore() : 0).append("` ")
              .append(m.getAwayTeamName()).append(" *(").append(m.getStatus()).append(")*\n");
        }

        List<String> facts = List.of(
            "Theo dõi trực tiếp " + matches.size() + " trận đấu.",
            "Bao gồm: Ngoại Hạng Anh, La Liga, Serie A, Champions League, Ligue 1..."
        );

        List<String> suggestions = List.of(
            "Phân tích trận đấu trực tiếp",
            "Xem các trận đấu hôm nay",
            "Phong độ các đội bóng hàng đầu"
        );

        String llmAnswer = callOpenAiIfAvailable("Tổng quan các trận đấu và giải đấu gần nhất", facts);
        String finalAnswer = (llmAnswer != null && !llmAnswer.isBlank()) ? llmAnswer : sb.toString();
        String source = (llmAnswer != null && !llmAnswer.isBlank()) ? "OpenAI gpt-4o-mini + PitchPulse Lakehouse" : "PitchPulse Analytical Lakehouse";

        return new AiAnalysisResponse(
            "SYSTEM_OVERVIEW",
            finalAnswer,
            facts,
            table,
            source,
            0.98,
            suggestions
        );
    }

    private AiAnalysisResponse analyzeGeneralOrTeam(String query) {
        List<com.football.analytics.model.Match> matches = postgresRepository.getAllMatches(null, null, null, null, null, 300);
        String qLower = query.toLowerCase();

        if (matches != null) {
            // 1. Check if user asks about a specific competition
            String[] competitions = {"premier league", "la liga", "serie a", "bundesliga", "ligue 1", "champions league", "brasileiro"};
            for (String comp : competitions) {
                if (qLower.contains(comp)) {
                    return summarizeCompetitionMatches(comp, matches);
                }
            }

            // 2. Check if any team name is mentioned in the query
            for (com.football.analytics.model.Match m : matches) {
                String cleanH = cleanTeamName(m.getHomeTeamName());
                String cleanA = cleanTeamName(m.getAwayTeamName());
                if (cleanH.length() >= 4 && qLower.contains(cleanH)) {
                    return analyzeTeamMatches(m.getHomeTeamId(), m.getHomeTeamName(), matches);
                }
                if (cleanA.length() >= 4 && qLower.contains(cleanA)) {
                    return analyzeTeamMatches(m.getAwayTeamId(), m.getAwayTeamName(), matches);
                }
            }

            // 3. Check if query is about recent matches or summary
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
            Map<String, Object> table = new LinkedHashMap<>();
            table.put("Cầu thủ", player.getName());
            table.put("Đội bóng", player.getTeamName().isBlank() ? "—" : player.getTeamName());
            table.put("Vị trí", player.getPosition().isBlank() ? "—" : player.getPosition());
            table.put("Số áo", player.getJerseyNumber() > 0 ? "#" + player.getJerseyNumber() : "—");
            table.put("Quốc tịch", player.getNationality().isBlank() ? "—" : player.getNationality());

            String answer = "### 👤 Hồ Sơ Cầu Thủ: " + player.getName() + "\n\n"
                + "- **Đội bóng hiện tại:** " + (player.getTeamName().isBlank() ? "—" : player.getTeamName()) + "\n"
                + "- **Vị trí thi đấu:** " + (player.getPosition().isBlank() ? "—" : player.getPosition()) + "\n"
                + "- **Số áo:** " + (player.getJerseyNumber() > 0 ? "#" + player.getJerseyNumber() : "—") + "\n"
                + "- **Quốc tịch:** " + (player.getNationality().isBlank() ? "—" : player.getNationality()) + "\n\n"
                + "💡 *Ghi chú phân tích:* Cầu thủ đã được ghi nhận trong danh sách đăng ký thi đấu chính thức. "
                + "Số liệu sự kiện chi tiết mùa giải sẽ tiếp tục được cập nhật theo tiến trình thi đấu.";

            List<String> facts = List.of(
                player.getName() + " thuộc biên chế " + player.getTeamName() + ", vị trí: " + player.getPosition()
            );

            return new AiAnalysisResponse(
                "PLAYER_PROFILE",
                answer,
                facts,
                table,
                "PitchPulse Player Lakehouse",
                0.95,
                List.of("So sánh với cầu thủ khác", "Xem danh sách cầu thủ")
            );
        }

        PlayerSeasonStats stats = statsOpt.get();
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("Cầu thủ", player.getName());
        table.put("Đội bóng", player.getTeamName());
        table.put("Mùa giải", stats.getSeasonName());
        table.put("Trận đấu", stats.getMatchesPlayed());
        table.put("Phút thi đấu", stats.getMinutes());
        table.put("Bàn thắng", stats.getGoals());
        table.put("Kiến tạo", stats.getAssists());
        table.put("xG (Kỳ vọng bàn thắng)", stats.getXg());
        table.put("xA (Kỳ vọng kiến tạo)", stats.getXa());
        table.put("Bàn/90 phút", stats.getGoalsPer90());
        table.put("Kiến tạo/90 phút", stats.getAssistsPer90());

        List<String> facts = List.of(
            player.getName() + " (" + player.getTeamName() + "): "
                + stats.getGoals() + " bàn thắng, " + stats.getAssists() + " kiến tạo.",
            "xG=" + stats.getXg() + ", xA=" + stats.getXa()
                + " qua " + stats.getMatchesPlayed() + " trận (" + stats.getMinutes() + " phút).",
            "Chỉ số hiệu quả 90 phút: " + stats.getGoalsPer90() + " bàn/90', " + stats.getAssistsPer90() + " kiến tạo/90'."
        );

        String answer = "### 🌟 Báo Cáo Phân Tích Cầu Thủ: " + player.getName() + "\n\n"
            + "Số liệu được xác thực trực tiếp từ kho phân tích dữ liệu sự kiện (không ước lượng hay làm tròn cảm tính):\n\n"
            + "- **Bàn thắng ghi được:** `" + stats.getGoals() + "` (xG kỳ vọng: `" + stats.getXg() + "`)\n"
            + "- **Khả năng kiến thiết (xA):** `" + stats.getAssists() + "` kiến tạo (xA: `" + stats.getXa() + "`)\n"
            + "- **Thời lượng thi đấu:** `" + stats.getMinutes() + "` phút qua `"
            + stats.getMatchesPlayed() + "` lần ra sân\n"
            + "- **Hiệu suất 90 phút:** `" + stats.getGoalsPer90() + "` bàn/90', `"
            + stats.getAssistsPer90() + "` kiến tạo/90'\n\n"
            + "🎯 **Đánh giá chuyên môn:** "
            + (stats.getGoals() > 0 ? player.getName() + " là mũi nhọn dứt điểm chủ lực trên hàng công." : "Cầu thủ đóng góp tích cực vào lối chơi tập thể và phân phối bóng.");

        List<String> suggestions = List.of(
            "Xem bản đồ dứt điểm của " + player.getName(),
            "So sánh " + player.getName() + " với cầu thủ khác",
            "Phân tích phong độ đội " + player.getTeamName()
        );

        String llmAnswer = callOpenAiIfAvailable("Phân tích chuyên sâu phong độ cầu thủ " + player.getName() + " (" + player.getTeamName() + ")", facts);
        String finalAnswer = (llmAnswer != null && !llmAnswer.isBlank()) ? llmAnswer : answer;
        String source = (llmAnswer != null && !llmAnswer.isBlank()) ? "OpenAI gpt-4o-mini + PitchPulse Lakehouse" : "PitchPulse Analytical Star-Schema";

        return new AiAnalysisResponse(
            "PLAYER_STATS",
            finalAnswer,
            facts,
            table,
            source,
            0.99,
            suggestions
        );
    }

    private AiAnalysisResponse unavailableResponse(String query) {
        String q = query == null || query.isBlank() ? "" : query.trim();
        List<String> facts = List.of(
            "Dữ liệu được xác thực nghiêm ngặt từ cơ sở dữ liệu trận đấu và sự kiện.",
            "Không suy đoán hoặc bịa số liệu khi chưa có dữ liệu chính thức."
        );

        String answer = "### ℹ️ Thông Tin Phân Tích Dữ Liệu\n\n"
            + "Hiện tại kho dữ liệu chưa ghi nhận trận đấu hoặc cầu thủ phù hợp với từ khóa: *\"" + q + "\"*.\n\n"
            + "👉 **Gợi ý dành cho bạn:**\n"
            + "- Bạn có thể vào tab **Lịch & Trận Đấu**, bấm vào bất kỳ trận đấu nào và chọn **'⚡ Hỏi AI Phân Tích'** để nhận báo cáo chuyên sâu ngay lập tức.\n"
            + "- Hoặc chọn các chủ đề phân tích có sẵn bên dưới:";

        return new AiAnalysisResponse(
            "DATA_UNAVAILABLE",
            answer,
            facts,
            null,
            "PitchPulse Analytical Lakehouse",
            0.0,
            List.of("Xem các trận đấu hôm nay", "Tổng quan các giải đấu", "Phong độ các đội bóng hàng đầu")
        );
    }

    private String extractSearchTerm(String query) {
        String cleaned = query.toLowerCase()
            .replaceAll("(?i)\\b(phân tích|analyze|compare|so sánh|của|cầu thủ|player|stats|thống kê|xem|cho tôi biết|trận đấu|giữa|và)\\b", " ")
            .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        if (cleaned.isBlank()) {
            return query.trim();
        }
        return cleaned;
    }

    /**
     * Optional LLM Integration: When OPENAI_API_KEY is configured in environment,
     * invoke OpenAI gpt-4o-mini to synthesize dynamic, eloquent analysis grounded strictly in DB facts.
     */
    private String callOpenAiIfAvailable(String prompt, List<String> facts) {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("sk-...")) {
            apiKey = System.getenv("AI_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("sk-...")) {
            return null; // LLM not configured, fall back to rich analytical engine
        }

        try {
            String model = System.getenv("AI_MODEL");
            if (model == null || model.isBlank()) model = "gpt-4o-mini";

            StringBuilder factsStr = new StringBuilder();
            if (facts != null) {
                for (String f : facts) {
                    factsStr.append("- ").append(f).append("\n");
                }
            }

            String systemPrompt = "Bạn là Trợ lý AI Phân tích Bóng đá Chuyên sâu PitchPulse. "
                + "Hãy trả lời câu hỏi của người dùng bằng tiếng Việt lưu loát, giàu chuyên môn chiến thuật và tự nhiên. "
                + "QUY TẮC BẮT BUỘC: Bạn CHỈ ĐƯỢC sử dụng các số liệu thực tế được cung cấp dưới đây. Tuyệt đối không bịa đặt số liệu hoặc kết quả.\n\n"
                + "Dữ liệu xác thực từ kho dữ liệu:\n" + factsStr;

            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("model", model);
            reqMap.put("temperature", 0.3);
            reqMap.put("max_tokens", 800);

            List<Map<String, String>> msgList = new ArrayList<>();
            msgList.add(Map.of("role", "system", "content", systemPrompt));
            msgList.add(Map.of("role", "user", "content", prompt));
            reqMap.put("messages", msgList);

            String requestBody = objectMapper.writeValueAsString(reqMap);

            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(7))
                .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null) {
                JsonNode root = objectMapper.readTree(resp.body());
                JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
                if (!contentNode.isMissingNode() && !contentNode.asText().isBlank()) {
                    return contentNode.asText();
                }
            }
        } catch (Exception e) {
            // Fail gracefully on timeout or network error to deterministic engine
        }
        return null;
    }
}
