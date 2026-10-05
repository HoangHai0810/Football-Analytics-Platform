package com.football.analytics.repository;

import com.football.analytics.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public class PostgresRepository {
    private static final Logger log = LoggerFactory.getLogger(PostgresRepository.class);


    @Value("${postgres.host:localhost}")
    private String host;

    @Value("${postgres.port:5432}")
    private int port;

    @Value("${postgres.database:football_analytics}")
    private String database;

    @Value("${postgres.user:postgres}")
    private String user;

    @Value("${postgres.password:}")
    private String password;

    @Value("${postgres.ssl:false}")
    private boolean ssl;

    private Connection getConnection() throws SQLException {
        String url = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password != null ? password : "");
        props.setProperty("connectTimeout", "8");
        props.setProperty("socketTimeout", "10");
        if (ssl) {
            props.setProperty("ssl", "true");
            props.setProperty("sslmode", "require");
        }
        return DriverManager.getConnection(url, props);
    }


    public boolean testConnection() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            if (rs.next()) {
                log.info("PostgreSQL connection verified: jdbc:postgresql://{}:{}/{}", host, port, database);
                return true;
            }
        } catch (Exception e) {
            log.error("PostgreSQL unreachable at {}:{}/{} - {}", host, port, database, e.getMessage());

        }
        return false;
    }

    public String getDatabaseUrl() {
        return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
    }

    public List<Competition> getAllCompetitions() {
        List<Competition> list = new ArrayList<>();
        String sql = "SELECT competition_id, name, country, type, updated_at FROM dim_competition ORDER BY competition_id";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("updated_at");
                LocalDateTime updatedAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                list.add(new Competition(
                    rs.getLong("competition_id"),
                    rs.getString("name"),
                    rs.getString("country"),
                    rs.getString("type"),
                    updatedAt
                ));
            }
        } catch (Exception e) {
            log.error("Failed to fetch competitions: {}", e.getMessage());
        }
        return list;
    }

    public Optional<Competition> getCompetition(Long id) {
        String sql = "SELECT competition_id, name, country, type, updated_at FROM dim_competition WHERE competition_id = " + id;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("updated_at");
                LocalDateTime updatedAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                return Optional.of(new Competition(
                    rs.getLong("competition_id"),
                    rs.getString("name"),
                    rs.getString("country"),
                    rs.getString("type"),
                    updatedAt
                ));
            }
        } catch (Exception e) {
            log.error("Failed to fetch competition {}: {}", id, e.getMessage());
        }
        return Optional.empty();
    }

    public List<Season> getSeasonsByCompetition(Long competitionId) {
        List<Season> list = new ArrayList<>();
        String sql = "SELECT season_id, competition_id, name, start_date, end_date FROM dim_season WHERE competition_id = " + competitionId + " ORDER BY season_id DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                java.sql.Date sDate = rs.getDate("start_date");
                java.sql.Date eDate = rs.getDate("end_date");
                LocalDate start = sDate != null ? sDate.toLocalDate() : LocalDate.of(2024, 8, 1);
                LocalDate end = eDate != null ? eDate.toLocalDate() : LocalDate.of(2025, 5, 31);
                list.add(new Season(
                    rs.getLong("season_id"),
                    rs.getLong("competition_id"),
                    rs.getString("name"),
                    start,
                    end
                ));
            }
        } catch (Exception e) {
            log.error("Failed to fetch seasons for competition {}: {}", competitionId, e.getMessage());
        }
        return list;
    }

    public List<Team> getAllTeams() {
        List<Team> list = new ArrayList<>();
        String sql = "SELECT team_id, name, country, stadium, logo_url FROM dim_team ORDER BY team_id";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("name");
                String shortName = name.replace(" FC", "").replace(" CF", "");
                list.add(new Team(
                    rs.getLong("team_id"),
                    name,
                    shortName,
                    rs.getString("country"),
                    rs.getString("stadium"),
                    rs.getString("logo_url")
                ));
            }
        } catch (Exception e) {
            log.error("Failed to fetch teams: {}", e.getMessage());
        }
        return list;
    }

    public Optional<Team> getTeam(Long id) {
        String sql = "SELECT team_id, name, country, stadium, logo_url FROM dim_team WHERE team_id = " + id;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                String name = rs.getString("name");
                String shortName = name.replace(" FC", "").replace(" CF", "");
                return Optional.of(new Team(
                    rs.getLong("team_id"),
                    name,
                    shortName,
                    rs.getString("country"),
                    rs.getString("stadium"),
                    rs.getString("logo_url")
                ));
            }
        } catch (Exception e) {
            log.error("Failed to fetch team {}: {}", id, e.getMessage());
        }
        return Optional.empty();
    }

    public List<Player> getAllPlayers(String query, String position) {
        List<Player> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.player_id AS player_id, p.name AS name, p.date_of_birth AS date_of_birth, p.nationality AS nationality, p.position AS position, p.preferred_foot AS preferred_foot, ")
           .append("coalesce(t.team_id, 0) as team_id, coalesce(t.name, 'Club') as team_name, coalesce(t.logo_url, '') as avatar_url ")
           .append("FROM dim_player p ")
           .append("LEFT JOIN (SELECT player_id, max(team_id) as team_id FROM fact_player_match GROUP BY player_id) fpm ON p.player_id = fpm.player_id ")
           .append("LEFT JOIN dim_team t ON fpm.team_id = t.team_id WHERE 1=1 ");

        if (query != null && !query.isBlank()) {
            String sanitized = query.replace("'", "''").trim().toLowerCase();
            sql.append(" AND (lower(p.name) LIKE '%").append(sanitized).append("%' OR lower(coalesce(t.name, '')) LIKE '%").append(sanitized).append("%') ");
        }
        if (position != null && !position.isBlank()) {
            String sanitizedPos = position.replace("'", "''").trim().toUpperCase();
            sql.append(" AND upper(p.position) = '").append(sanitizedPos).append("' ");
        }
        sql.append("ORDER BY p.player_id LIMIT 100");

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {
            while (rs.next()) {
                java.sql.Date dobDate = rs.getDate("date_of_birth");
                LocalDate dob = dobDate != null ? dobDate.toLocalDate() : LocalDate.of(1998, 1, 1);
                list.add(new Player(
                    rs.getLong("player_id"),
                    rs.getLong("team_id"),
                    rs.getString("team_name"),
                    rs.getString("name"),
                    dob,
                    rs.getString("nationality"),
                    rs.getString("position"),
                    rs.getString("preferred_foot"),
                    10,
                    rs.getString("avatar_url")
                ));
            }
        } catch (Exception e) {
            log.error("Failed to fetch players: {}", e.getMessage());
        }
        return list;
    }

    public Optional<Player> getPlayer(Long id) {
        String sql = "SELECT p.player_id AS player_id, p.name AS name, p.date_of_birth AS date_of_birth, p.nationality AS nationality, p.position AS position, p.preferred_foot AS preferred_foot, " +
                     "coalesce(t.team_id, 0) as team_id, coalesce(t.name, 'Club') as team_name, coalesce(t.logo_url, '') as avatar_url " +
                     "FROM dim_player p " +
                     "LEFT JOIN (SELECT player_id, max(team_id) as team_id FROM fact_player_match GROUP BY player_id) fpm ON p.player_id = fpm.player_id " +
                     "LEFT JOIN dim_team t ON fpm.team_id = t.team_id " +
                     "WHERE p.player_id = " + id;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                java.sql.Date dobDate = rs.getDate("date_of_birth");
                LocalDate dob = dobDate != null ? dobDate.toLocalDate() : LocalDate.of(1998, 1, 1);
                return Optional.of(new Player(
                    rs.getLong("player_id"),
                    rs.getLong("team_id"),
                    rs.getString("team_name"),
                    rs.getString("name"),
                    dob,
                    rs.getString("nationality"),
                    rs.getString("position"),
                    rs.getString("preferred_foot"),
                    10,
                    rs.getString("avatar_url")
                ));
            }
        } catch (Exception e) {
            log.error("Failed to fetch player {}: {}", id, e.getMessage());
        }
        return Optional.empty();
    }

    public List<Match> getAllMatches(Long competitionId, Long seasonId, String status) {
        List<Match> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT m.match_id AS match_id, m.competition_id AS competition_id, coalesce(c.name, 'Competition') as competition_name, m.season_id AS season_id, ")
           .append("m.home_team_id AS home_team_id, coalesce(ht.name, 'Home Team') as home_team_name, coalesce(ht.logo_url, '') as home_team_logo, ")
           .append("m.away_team_id AS away_team_id, coalesce(at.name, 'Away Team') as away_team_name, coalesce(at.logo_url, '') as away_team_logo, ")
           .append("coalesce(fm.home_score, 0) as home_score, coalesce(fm.away_score, 0) as away_score, ")
           .append("coalesce(fm.home_xg, 0.0) as home_xg, coalesce(fm.away_xg, 0.0) as away_xg, ")
           .append("m.match_date AS match_date, m.status AS status, coalesce(ht.stadium, 'Stadium') as stadium, coalesce(fm.attendance, 0) as attendance ")
           .append("FROM dim_match m ")
           .append("LEFT JOIN fact_match fm ON m.match_id = fm.match_id ")
           .append("LEFT JOIN dim_competition c ON m.competition_id = c.competition_id ")
           .append("LEFT JOIN dim_team ht ON m.home_team_id = ht.team_id ")
           .append("LEFT JOIN dim_team at2 ON m.away_team_id = at2.team_id WHERE 1=1 ");

        if (competitionId != null) {
            sql.append(" AND m.competition_id = ").append(competitionId);
        }
        if (seasonId != null) {
            sql.append(" AND m.season_id = ").append(seasonId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND upper(m.status) = '").append(status.replace("'", "''").trim().toUpperCase()).append("' ");
        }
        sql.append(" ORDER BY m.match_date DESC LIMIT 50");

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("match_date");
                LocalDateTime matchDate = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                list.add(new Match(
                    rs.getLong("match_id"),
                    rs.getLong("competition_id"),
                    rs.getString("competition_name"),
                    rs.getLong("season_id"),
                    rs.getLong("home_team_id"),
                    rs.getString("home_team_name"),
                    rs.getString("home_team_logo"),
                    rs.getLong("away_team_id"),
                    rs.getString("away_team_name"),
                    rs.getString("away_team_logo"),
                    rs.getInt("home_score"),
                    rs.getInt("away_score"),
                    rs.getDouble("home_xg"),
                    rs.getDouble("away_xg"),
                    matchDate,
                    rs.getString("status"),
                    rs.getString("stadium"),
                    rs.getLong("attendance")
                ));
            }
        } catch (Exception e) {
            log.error("Failed to fetch matches: {}", e.getMessage());
        }
        return list;
    }

    public Optional<Match> getMatch(Long id) {
        String sql = "SELECT m.match_id AS match_id, m.competition_id AS competition_id, coalesce(c.name, 'Competition') as competition_name, m.season_id AS season_id, " +
                     "m.home_team_id AS home_team_id, coalesce(ht.name, 'Home Team') as home_team_name, coalesce(ht.logo_url, '') as home_team_logo, " +
                     "m.away_team_id AS away_team_id, coalesce(at.name, 'Away Team') as away_team_name, coalesce(at.logo_url, '') as away_team_logo, " +
                     "coalesce(fm.home_score, 0) as home_score, coalesce(fm.away_score, 0) as away_score, " +
                     "coalesce(fm.home_xg, 0.0) as home_xg, coalesce(fm.away_xg, 0.0) as away_xg, " +
                     "m.match_date AS match_date, m.status AS status, coalesce(ht.stadium, 'Stadium') as stadium, coalesce(fm.attendance, 0) as attendance " +
                     "FROM dim_match m " +
                     "LEFT JOIN fact_match fm ON m.match_id = fm.match_id " +
                     "LEFT JOIN dim_competition c ON m.competition_id = c.competition_id " +
                     "LEFT JOIN dim_team ht ON m.home_team_id = ht.team_id " +
                     "LEFT JOIN dim_team at2 ON m.away_team_id = at2.team_id " +
                     "WHERE m.match_id = " + id;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("match_date");
                LocalDateTime matchDate = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                return Optional.of(new Match(
                    rs.getLong("match_id"),
                    rs.getLong("competition_id"),
                    rs.getString("competition_name"),
                    rs.getLong("season_id"),
                    rs.getLong("home_team_id"),
                    rs.getString("home_team_name"),
                    rs.getString("home_team_logo"),
                    rs.getLong("away_team_id"),
                    rs.getString("away_team_name"),
                    rs.getString("away_team_logo"),
                    rs.getInt("home_score"),
                    rs.getInt("away_score"),
                    rs.getDouble("home_xg"),
                    rs.getDouble("away_xg"),
                    matchDate,
                    rs.getString("status"),
                    rs.getString("stadium"),
                    rs.getLong("attendance")
                ));
            }
        } catch (Exception e) {
            log.error("Failed to fetch match {}: {}", id, e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<PlayerSeasonStats> getPlayerStats(Long playerId, Long seasonId) {
        // Try dbt mart first: mart_player_season_stats
        String martSql = "SELECT * FROM mart_player_season_stats WHERE player_id = " + playerId +
                         (seasonId != null ? " AND season_id = " + seasonId : "") + " LIMIT 1";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(martSql)) {
            if (rs.next()) {
                PlayerSeasonStats stats = new PlayerSeasonStats();
                stats.setPlayerId(rs.getLong("player_id"));
                stats.setSeasonId(rs.getLong("season_id"));
                stats.setSeasonName("2024/2025");
                stats.setMatchesPlayed(rs.getInt("total_matches"));
                stats.setMinutes(rs.getInt("total_minutes"));
                stats.setGoals(rs.getInt("total_goals"));
                stats.setAssists(rs.getInt("total_assists"));
                stats.setShots(rs.getInt("total_shots"));
                stats.setShotsOnTarget(rs.getInt("total_shots_on_target"));
                stats.setPasses(rs.getInt("total_passes"));
                stats.setKeyPasses(rs.getInt("total_key_passes"));
                stats.setXg(rs.getDouble("total_xg"));
                stats.setXa(rs.getDouble("total_xa"));
                stats.setTackles(rs.getInt("total_tackles"));
                stats.setInterceptions(rs.getInt("total_interceptions"));
                stats.setDuels(rs.getInt("total_duels"));
                stats.setPressures(rs.getInt("total_pressures"));
                stats.setGoalsPer90(rs.getDouble("goals_per_90"));
                stats.setAssistsPer90(rs.getDouble("assists_per_90"));
                stats.setXgPer90(rs.getDouble("xg_per_90"));
                stats.setXaPer90(rs.getDouble("xa_per_90"));
                stats.setShotsPer90(rs.getDouble("shots_per_90"));
                stats.setPassCompletionRate(82.5);
                stats.setDuelWinRate(54.0);

                calculateRadarRatings(stats);
                return Optional.of(stats);
            }
        } catch (Exception e) {
            log.debug("mart_player_season_stats not queried, trying fact_player_match aggregation: {}", e.getMessage());
        }

        // Direct aggregation from fact_player_match
        String aggSql = "SELECT player_id, count(DISTINCT match_id) as total_matches, sum(minutes) as total_minutes, " +
                        "sum(goals) as total_goals, sum(assists) as total_assists, sum(shots) as total_shots, " +
                        "sum(shots_on_target) as total_shots_on_target, sum(passes) as total_passes, " +
                        "sum(key_passes) as total_key_passes, round(sum(xg), 2) as total_xg, round(sum(xa), 2) as total_xa, " +
                        "sum(tackles) as total_tackles, sum(interceptions) as total_interceptions, " +
                        "sum(duels) as total_duels, sum(pressures) as total_pressures " +
                        "FROM fact_player_match WHERE player_id = " + playerId + " GROUP BY player_id";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(aggSql)) {
            if (rs.next()) {
                PlayerSeasonStats stats = new PlayerSeasonStats();
                stats.setPlayerId(rs.getLong("player_id"));
                stats.setSeasonId(seasonId != null ? seasonId : 2024L);
                stats.setSeasonName("2024/2025");
                int matches = rs.getInt("total_matches");
                int minutes = rs.getInt("total_minutes");
                int goals = rs.getInt("total_goals");
                int assists = rs.getInt("total_assists");
                int shots = rs.getInt("total_shots");
                double xg = rs.getDouble("total_xg");
                double xa = rs.getDouble("total_xa");

                stats.setMatchesPlayed(matches);
                stats.setMinutes(minutes);
                stats.setGoals(goals);
                stats.setAssists(assists);
                stats.setShots(shots);
                stats.setShotsOnTarget(rs.getInt("total_shots_on_target"));
                stats.setPasses(rs.getInt("total_passes"));
                stats.setKeyPasses(rs.getInt("total_key_passes"));
                stats.setXg(xg);
                stats.setXa(xa);
                stats.setTackles(rs.getInt("total_tackles"));
                stats.setInterceptions(rs.getInt("total_interceptions"));
                stats.setDuels(rs.getInt("total_duels"));
                stats.setPressures(rs.getInt("total_pressures"));

                double mins = minutes > 0 ? minutes : (matches * 90.0);
                if (mins > 0) {
                    stats.setGoalsPer90(Math.round((goals * 90.0 / mins) * 100.0) / 100.0);
                    stats.setAssistsPer90(Math.round((assists * 90.0 / mins) * 100.0) / 100.0);
                    stats.setXgPer90(Math.round((xg * 90.0 / mins) * 100.0) / 100.0);
                    stats.setXaPer90(Math.round((xa * 90.0 / mins) * 100.0) / 100.0);
                    stats.setShotsPer90(Math.round((shots * 90.0 / mins) * 100.0) / 100.0);
                } else {
                    stats.setGoalsPer90(0.0);
                    stats.setAssistsPer90(0.0);
                    stats.setXgPer90(0.0);
                    stats.setXaPer90(0.0);
                    stats.setShotsPer90(0.0);
                }
                stats.setPassCompletionRate(81.5);
                stats.setDuelWinRate(52.0);

                calculateRadarRatings(stats);
                return Optional.of(stats);
            }
        } catch (Exception e) {
            log.error("Failed to fetch player stats for {}: {}", playerId, e.getMessage());
        }
        return Optional.empty();
    }

    private void calculateRadarRatings(PlayerSeasonStats stats) {
        double g90 = stats.getGoalsPer90() != null ? stats.getGoalsPer90() : 0.0;
        double a90 = stats.getAssistsPer90() != null ? stats.getAssistsPer90() : 0.0;
        double xg90 = stats.getXgPer90() != null ? stats.getXgPer90() : 0.0;
        int kp = stats.getKeyPasses() != null ? stats.getKeyPasses() : 0;
        int tkl = stats.getTackles() != null ? stats.getTackles() : 0;
        int p = stats.getPressures() != null ? stats.getPressures() : 0;
        int passes = stats.getPasses() != null ? stats.getPasses() : 0;
        int duels = stats.getDuels() != null ? stats.getDuels() : 0;

        stats.setFinishingRating(Math.min(99, Math.max(50, (int) (g90 * 65.0 + xg90 * 30.0 + 40))));
        stats.setCreationRating(Math.min(99, Math.max(50, (int) (a90 * 60.0 + kp * 0.8 + 45))));
        stats.setProgressionRating(Math.min(99, Math.max(50, (int) (passes * 0.04 + 50))));
        stats.setPressingRating(Math.min(99, Math.max(50, (int) (p * 0.25 + 45))));
        stats.setDefendingRating(Math.min(99, Math.max(40, (int) (tkl * 1.5 + 40))));
        stats.setAerialRating(Math.min(99, Math.max(45, (int) (duels * 0.2 + 50))));
    }

    public List<ShotEvent> getPlayerShots(Long playerId, Long seasonId) {
        List<ShotEvent> list = new ArrayList<>();
        String sql = "SELECT toString(fe.event_id) as event_id, fe.match_id, fe.player_id, coalesce(dp.name, 'Player') as player_name, " +
                     "fe.team_id, fe.minute, fe.second, fe.x, fe.y, fe.outcome " +
                     "FROM fact_event fe " +
                     "LEFT JOIN dim_player dp ON fe.player_id = dp.player_id " +
                     "WHERE fe.player_id = " + playerId + " AND (fe.event_type IN ('SHOT', 'GOAL') OR fe.outcome = 'GOAL') " +
                     "ORDER BY fe.match_id, fe.minute, fe.second LIMIT 20";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                // Distance to goal mouth (120, 40)
                double dist = Math.sqrt(Math.pow(120.0 - x, 2) + Math.pow(40.0 - y, 2));
                double xg = Math.round(Math.max(0.04, Math.min(0.85, 1.0 / (1.0 + dist * 0.15))) * 100.0) / 100.0;
                String outcome = rs.getString("outcome");
                list.add(new ShotEvent(
                    rs.getString("event_id"),
                    rs.getLong("match_id"),
                    rs.getLong("player_id"),
                    rs.getString("player_name"),
                    rs.getLong("team_id"),
                    rs.getInt("minute"),
                    rs.getInt("second"),
                    x,
                    y,
                    xg,
                    outcome != null && !outcome.isBlank() ? outcome : "SHOT",
                    "RIGHT_FOOT",
                    "OPEN_PLAY"
                ));
            }
        } catch (Exception e) {
            log.error("Failed to fetch player shots for {}: {}", playerId, e.getMessage());
        }
        return list;
    }

    public Map<String, Long> getEntityCounts() {
        Map<String, Long> counts = new HashMap<>();
        String sql = "SELECT " +
                     "(SELECT count(*) FROM dim_competition) as competitions, " +
                     "(SELECT count(*) FROM dim_team) as teams, " +
                     "(SELECT count(*) FROM dim_player) as players, " +
                     "(SELECT count(*) FROM dim_match) as matches, " +
                     "(SELECT count(*) FROM fact_event WHERE event_type IN ('SHOT', 'GOAL') OR outcome = 'GOAL') as shot_events";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                counts.put("competitions", rs.getLong("competitions"));
                counts.put("teams", rs.getLong("teams"));
                counts.put("players", rs.getLong("players"));
                counts.put("matches", rs.getLong("matches"));
                counts.put("shot_events", rs.getLong("shot_events"));
                return counts;
            }
        } catch (Exception e) {
            log.error("Failed to fetch entity counts: {}", e.getMessage());
        }
        return Collections.emptyMap();
    }
}


