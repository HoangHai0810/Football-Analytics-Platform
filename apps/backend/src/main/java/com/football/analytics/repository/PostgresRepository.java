package com.football.analytics.repository;

import com.football.analytics.model.*;
import jakarta.annotation.PostConstruct;
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
        props.setProperty("connectTimeout", "10");
        props.setProperty("socketTimeout", "30");
        if (ssl) {
            props.setProperty("ssl", "true");
            props.setProperty("sslmode", "require");
        }
        return DriverManager.getConnection(url, props);
    }

    @PostConstruct
    public void ensureSchema() {
        String[] ddl = {
            "ALTER TABLE dim_player ADD COLUMN IF NOT EXISTS jersey_number INTEGER DEFAULT 0",
            "ALTER TABLE dim_player ADD COLUMN IF NOT EXISTS avatar_url TEXT DEFAULT ''",
            "ALTER TABLE dim_player ADD COLUMN IF NOT EXISTS team_id BIGINT DEFAULT 0",
            "ALTER TABLE dim_player ALTER COLUMN preferred_foot DROP NOT NULL",
            "ALTER TABLE dim_player ALTER COLUMN preferred_foot SET DEFAULT ''",
            "CREATE INDEX IF NOT EXISTS idx_dim_player_name ON dim_player (lower(name))",
            "CREATE INDEX IF NOT EXISTS idx_dim_player_team ON dim_player (team_id)",
            // Cleanup invented / mis-mapped presentation fields
            "UPDATE dim_team SET stadium = '', updated_at = NOW() WHERE stadium ILIKE 'Stadium of %' OR lower(stadium) = 'stadium'",
            "UPDATE dim_team t SET logo_url = '', updated_at = NOW() WHERE t.logo_url LIKE 'https://crests.football-data.org/%' AND EXISTS (SELECT 1 FROM dim_competition c WHERE c.name = t.country)",
            "UPDATE dim_player SET avatar_url = '' WHERE avatar_url LIKE '%ui-avatars.com%' OR avatar_url LIKE '%crests.football-data.org%'"
        };
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : ddl) {
                try {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    log.warn("Schema ensure skipped [{}]: {}", sql, e.getMessage());
                }
            }
            log.info("PostgreSQL schema ensure completed for dim_player");
        } catch (Exception e) {
            log.warn("Could not ensure schema on startup (will retry on query): {}", e.getMessage());
        }
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
                list.add(mapTeam(rs));
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
                return Optional.of(mapTeam(rs));
            }
        } catch (Exception e) {
            log.error("Failed to fetch team {}: {}", id, e.getMessage());
        }
        return Optional.empty();
    }

    private Team mapTeam(ResultSet rs) throws SQLException {
        String name = rs.getString("name");
        String shortName = name != null ? name.replace(" FC", "").replace(" CF", "") : "";
        String stadium = sanitizeStadium(rs.getString("stadium"));
        String logo = sanitizeLogoUrl(rs.getString("logo_url"));
        String country = rs.getString("country");
        // Competition name wrongly stored as country — blank it for honesty
        if (country != null && (country.contains("League") || country.contains("Liga")
                || country.contains("Serie") || country.contains("Bundesliga")
                || country.contains("Championship") || country.contains("Cup"))) {
            country = "";
        }
        return new Team(
            rs.getLong("team_id"),
            name,
            shortName,
            country != null ? country : "",
            stadium,
            logo
        );
    }

    private String sanitizeStadium(String stadium) {
        if (stadium == null || stadium.isBlank()) return "";
        if (stadium.equalsIgnoreCase("Stadium") || stadium.startsWith("Stadium of ")) return "";
        return stadium;
    }

    private String sanitizeLogoUrl(String logo) {
        if (logo == null || logo.isBlank()) return "";
        // ui-avatars is not a real crest
        if (logo.contains("ui-avatars.com")) return "";
        return logo;
    }

    public List<Player> getAllPlayers(String query, String position) {
        List<Object> params = new ArrayList<>();
        String where = buildPlayerWhere(query, position, params, "p");

        // 1) Full join (team from dim_player.team_id or latest fact_player_match)
        String fullSql =
            "SELECT p.player_id, p.name, p.date_of_birth, "
                + "coalesce(p.nationality, '') as nationality, "
                + "coalesce(p.position, '') as position, "
                + "coalesce(p.preferred_foot, '') as preferred_foot, "
                + "coalesce(p.jersey_number, 0) as jersey_number, "
                + "coalesce(nullif(p.avatar_url, ''), '') as avatar_url, "
                + "coalesce(nullif(p.team_id, 0), fpm.team_id, 0) as team_id, "
                + "coalesce(t.name, '') as team_name "
                + "FROM dim_player p "
                + "LEFT JOIN LATERAL ("
                + "  SELECT team_id FROM fact_player_match "
                + "  WHERE player_id = p.player_id AND team_id > 0 "
                + "  ORDER BY match_id DESC LIMIT 1"
                + ") fpm ON true "
                + "LEFT JOIN dim_team t ON t.team_id = coalesce(nullif(p.team_id, 0), fpm.team_id) "
                + where
                + " ORDER BY p.name ASC LIMIT 200";

        try {
            List<Player> list = executePlayerQuery(fullSql, params);
            if (!list.isEmpty()) {
                log.info("getAllPlayers returned {} row(s) via full join", list.size());
                return list;
            }
        } catch (Exception e) {
            log.error("Full players query failed: {}", e.getMessage(), e);
        }

        // 2) dim_player + team_id only (no LATERAL / fact_player_match)
        params = new ArrayList<>();
        where = buildPlayerWhere(query, position, params, "p");
        String teamSql =
            "SELECT p.player_id, p.name, p.date_of_birth, "
                + "coalesce(p.nationality, '') as nationality, "
                + "coalesce(p.position, '') as position, "
                + "coalesce(p.preferred_foot, '') as preferred_foot, "
                + "coalesce(p.jersey_number, 0) as jersey_number, "
                + "coalesce(nullif(p.avatar_url, ''), '') as avatar_url, "
                + "coalesce(p.team_id, 0) as team_id, "
                + "coalesce(t.name, '') as team_name "
                + "FROM dim_player p "
                + "LEFT JOIN dim_team t ON t.team_id = nullif(p.team_id, 0) "
                + where
                + " ORDER BY p.name ASC LIMIT 200";
        try {
            List<Player> list = executePlayerQuery(teamSql, params);
            if (!list.isEmpty()) {
                log.info("getAllPlayers returned {} row(s) via team join", list.size());
                return list;
            }
        } catch (Exception e) {
            log.error("Team-join players query failed: {}", e.getMessage(), e);
        }

        // 3) Absolute minimal — must work if dim_player has rows (entity_counts.players > 0)
        params = new ArrayList<>();
        where = buildPlayerWhere(query, position, params, null);
        String minimal =
            "SELECT player_id, name, date_of_birth, "
                + "coalesce(nationality,'') as nationality, "
                + "coalesce(position,'') as position, "
                + "coalesce(preferred_foot,'') as preferred_foot, "
                + "0 as jersey_number, '' as avatar_url, 0 as team_id, '' as team_name "
                + "FROM dim_player "
                + where
                + " ORDER BY name ASC LIMIT 200";
        try {
            List<Player> list = executePlayerQuery(minimal, params);
            log.info("getAllPlayers minimal returned {} row(s)", list.size());
            return list;
        } catch (Exception e) {
            log.error("Minimal players query failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private String buildPlayerWhere(String query, String position, List<Object> params, String alias) {
        String prefix = (alias == null || alias.isBlank()) ? "" : alias + ".";
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        if (query != null && !query.isBlank()) {
            String q = "%" + query.trim().toLowerCase() + "%";
            if (alias != null) {
                where.append(" AND (lower(").append(prefix).append("name) LIKE ? OR lower(coalesce(t.name, '')) LIKE ?) ");
                params.add(q);
                params.add(q);
            } else {
                where.append(" AND lower(name) LIKE ? ");
                params.add(q);
            }
        }
        if (position != null && !position.isBlank() && !"ALL".equalsIgnoreCase(position)) {
            where.append(" AND upper(").append(prefix).append("position) = ? ");
            params.add(position.trim().toUpperCase());
        }
        return where.toString();
    }

    private List<Player> executePlayerQuery(String sql, List<Object> params) throws SQLException {
        List<Player> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        list.add(mapPlayer(rs));
                    } catch (Exception rowEx) {
                        log.warn("Skipping corrupt player row: {}", rowEx.getMessage());
                    }
                }
            }
        }
        return list;
    }

    private Player mapPlayer(ResultSet rs) throws SQLException {
        LocalDate dob = null;
        try {
            java.sql.Date dobDate = rs.getDate("date_of_birth");
            if (dobDate != null) dob = dobDate.toLocalDate();
        } catch (SQLException ignored) {}

        String name = rs.getString("name");
        if (name == null || name.isBlank()) {
            throw new SQLException("player name missing");
        }

        String avatar = "";
        try {
            avatar = rs.getString("avatar_url");
        } catch (SQLException ignored) {}
        // Never treat generated placeholders / team crests as player photos
        if (avatar != null && (avatar.contains("ui-avatars.com") || avatar.contains("crests.football-data.org"))) {
            avatar = "";
        }

        long teamId = 0L;
        try { teamId = rs.getLong("team_id"); } catch (SQLException ignored) {}
        String teamName = "";
        try { teamName = rs.getString("team_name"); } catch (SQLException ignored) {}
        int jersey = 0;
        try { jersey = rs.getInt("jersey_number"); } catch (SQLException ignored) {}

        String nationality = "";
        try { nationality = rs.getString("nationality"); } catch (SQLException ignored) {}
        String position = "";
        try { position = rs.getString("position"); } catch (SQLException ignored) {}
        String foot = "";
        try { foot = rs.getString("preferred_foot"); } catch (SQLException ignored) {}
        if (foot == null) foot = "";

        return new Player(
            rs.getLong("player_id"),
            teamId,
            teamName != null ? teamName : "",
            name.trim(),
            dob,
            nationality != null ? nationality : "",
            position != null ? position : "",
            foot,
            jersey,
            avatar != null ? avatar : ""
        );
    }

    public Optional<Player> getPlayer(Long id) {
        String fullSql =
            "SELECT p.player_id, p.name, p.date_of_birth, "
                + "coalesce(p.nationality, '') as nationality, "
                + "coalesce(p.position, '') as position, "
                + "coalesce(p.preferred_foot, '') as preferred_foot, "
                + "coalesce(p.jersey_number, 0) as jersey_number, "
                + "coalesce(nullif(p.avatar_url, ''), '') as avatar_url, "
                + "coalesce(nullif(p.team_id, 0), fpm.team_id, 0) as team_id, "
                + "coalesce(t.name, '') as team_name "
                + "FROM dim_player p "
                + "LEFT JOIN LATERAL ("
                + "  SELECT team_id FROM fact_player_match "
                + "  WHERE player_id = p.player_id AND team_id > 0 "
                + "  ORDER BY match_id DESC LIMIT 1"
                + ") fpm ON true "
                + "LEFT JOIN dim_team t ON t.team_id = coalesce(nullif(p.team_id, 0), fpm.team_id) "
                + "WHERE p.player_id = ?";
        try {
            List<Player> list = executePlayerQuery(fullSql, List.of(id));
            if (!list.isEmpty()) return Optional.of(list.get(0));
        } catch (Exception e) {
            log.error("getPlayer full query {} failed: {}", id, e.getMessage());
        }

        String minimal =
            "SELECT player_id, name, date_of_birth, coalesce(nationality,'') as nationality, "
                + "coalesce(position,'') as position, coalesce(preferred_foot,'') as preferred_foot, "
                + "0 as jersey_number, '' as avatar_url, 0 as team_id, '' as team_name "
                + "FROM dim_player WHERE player_id = ?";
        try {
            List<Player> list = executePlayerQuery(minimal, List.of(id));
            if (!list.isEmpty()) return Optional.of(list.get(0));
        } catch (Exception e) {
            log.error("getPlayer {} failed: {}", id, e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Match> getAllMatches(Long competitionId, Long seasonId, String status) {
        return getAllMatches(competitionId, seasonId, status, null, null, 100);
    }

    public List<Match> getAllMatches(Long competitionId, Long seasonId, String status,
                                     LocalDateTime dateFrom, LocalDateTime dateTo, int limit) {
        List<Match> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT m.match_id, m.competition_id, coalesce(c.name, '') as competition_name, m.season_id, ")
           .append("m.home_team_id, coalesce(ht.name, '') as home_team_name, coalesce(ht.logo_url, '') as home_team_logo, ")
           .append("m.away_team_id, coalesce(awt.name, '') as away_team_name, coalesce(awt.logo_url, '') as away_team_logo, ")
           .append("coalesce(fm.home_score, 0) as home_score, coalesce(fm.away_score, 0) as away_score, ")
           .append("fm.home_xg as home_xg, fm.away_xg as away_xg, ")
           .append("m.match_date, m.status, coalesce(nullif(ht.stadium, ''), '') as stadium, coalesce(fm.attendance, 0) as attendance ")
           .append("FROM dim_match m ")
           .append("LEFT JOIN fact_match fm ON m.match_id = fm.match_id ")
           .append("LEFT JOIN dim_competition c ON m.competition_id = c.competition_id ")
           .append("LEFT JOIN dim_team ht ON m.home_team_id = ht.team_id ")
           .append("LEFT JOIN dim_team awt ON m.away_team_id = awt.team_id WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (competitionId != null) {
            sql.append(" AND m.competition_id = ? ");
            params.add(competitionId);
        }
        if (seasonId != null) {
            sql.append(" AND m.season_id = ? ");
            params.add(seasonId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND upper(m.status) = ? ");
            params.add(status.trim().toUpperCase());
        }
        if (dateFrom != null) {
            sql.append(" AND m.match_date >= ? ");
            params.add(Timestamp.valueOf(dateFrom));
        }
        if (dateTo != null) {
            sql.append(" AND m.match_date <= ? ");
            params.add(Timestamp.valueOf(dateTo));
        }
        int safeLimit = Math.max(1, Math.min(limit > 0 ? limit : 100, 200));
        sql.append(" ORDER BY m.match_date DESC LIMIT ").append(safeLimit);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapMatch(rs));
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch matches: {}", e.getMessage(), e);
        }
        return list;
    }

    private Match mapMatch(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("match_date");
        LocalDateTime matchDate = ts != null ? ts.toLocalDateTime() : null;
        Double homeXg = rs.getObject("home_xg") != null ? rs.getDouble("home_xg") : null;
        Double awayXg = rs.getObject("away_xg") != null ? rs.getDouble("away_xg") : null;
        // Treat zero-only invented placeholders as absent when both are exactly the sync-formula pattern isn't detectable;
        // UI already hides xG when both are null/0.
        String stadium = sanitizeStadium(rs.getString("stadium"));
        // Treat placeholder 0/0 xG as absent when both sides are exactly 0 (sync default)
        if (homeXg != null && awayXg != null && homeXg == 0.0 && awayXg == 0.0) {
            homeXg = null;
            awayXg = null;
        }
        return new Match(
            rs.getLong("match_id"),
            rs.getLong("competition_id"),
            rs.getString("competition_name"),
            rs.getLong("season_id"),
            rs.getLong("home_team_id"),
            rs.getString("home_team_name"),
            sanitizeLogoUrl(rs.getString("home_team_logo")),
            rs.getLong("away_team_id"),
            rs.getString("away_team_name"),
            sanitizeLogoUrl(rs.getString("away_team_logo")),
            rs.getInt("home_score"),
            rs.getInt("away_score"),
            homeXg,
            awayXg,
            matchDate,
            rs.getString("status"),
            stadium,
            rs.getLong("attendance")
        );
    }

    public Optional<Match> getMatch(Long id) {
        String sql = "SELECT m.match_id, m.competition_id, coalesce(c.name, '') as competition_name, m.season_id, "
                + "m.home_team_id, coalesce(ht.name, '') as home_team_name, coalesce(ht.logo_url, '') as home_team_logo, "
                + "m.away_team_id, coalesce(awt.name, '') as away_team_name, coalesce(awt.logo_url, '') as away_team_logo, "
                + "coalesce(fm.home_score, 0) as home_score, coalesce(fm.away_score, 0) as away_score, "
                + "fm.home_xg as home_xg, fm.away_xg as away_xg, "
                + "m.match_date, m.status, coalesce(nullif(ht.stadium, ''), '') as stadium, coalesce(fm.attendance, 0) as attendance "
                + "FROM dim_match m "
                + "LEFT JOIN fact_match fm ON m.match_id = fm.match_id "
                + "LEFT JOIN dim_competition c ON m.competition_id = c.competition_id "
                + "LEFT JOIN dim_team ht ON m.home_team_id = ht.team_id "
                + "LEFT JOIN dim_team awt ON m.away_team_id = awt.team_id "
                + "WHERE m.match_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapMatch(rs));
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch match {}: {}", id, e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<PlayerSeasonStats> getPlayerStats(Long playerId, Long seasonId) {
        // Try dbt mart first: mart_player_season_stats
        // 1. If seasonId provided, try that specific season
        if (seasonId != null) {
            String martSql = "SELECT * FROM mart_player_season_stats WHERE player_id = " + playerId +
                             " AND season_id = " + seasonId + " LIMIT 1";
            Optional<PlayerSeasonStats> res = extractMartStats(martSql);
            if (res.isPresent()) return res;
        }

        // 2. If not found or seasonId omitted, take the player's most active season
        String bestSeasonSql = "SELECT * FROM mart_player_season_stats WHERE player_id = " + playerId +
                               " ORDER BY total_minutes DESC LIMIT 1";
        Optional<PlayerSeasonStats> bestRes = extractMartStats(bestSeasonSql);
        if (bestRes.isPresent()) return bestRes;

        // 3. Fallback: direct aggregation from fact_player_match
        String aggSql = "SELECT player_id, count(DISTINCT match_id) as total_matches, sum(minutes) as total_minutes, " +
                        "sum(goals) as total_goals, sum(assists) as total_assists, sum(shots) as total_shots, " +
                        "sum(shots_on_target) as total_shots_on_target, sum(passes) as total_passes, " +
                        "sum(key_passes) as total_key_passes, round(cast(sum(xg) as numeric), 2) as total_xg, round(cast(sum(xa) as numeric), 2) as total_xa, " +
                        "sum(tackles) as total_tackles, sum(interceptions) as total_interceptions, " +
                        "sum(duels) as total_duels, sum(pressures) as total_pressures " +
                        "FROM fact_player_match WHERE player_id = " + playerId + " GROUP BY player_id";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(aggSql)) {
            if (rs.next()) {
                PlayerSeasonStats stats = new PlayerSeasonStats();
                stats.setPlayerId(rs.getLong("player_id"));
                stats.setSeasonId(seasonId);
                stats.setSeasonName(seasonId != null ? ("Season " + seasonId) : "All available");
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
                // Do NOT invent pass/duel rates — leave null when source data is unavailable
                stats.setPassCompletionRate(null);
                stats.setDuelWinRate(null);

                applyDisplayRadarFromRealPer90(stats);
                return Optional.of(stats);
            }
        } catch (Exception e) {
            log.error("Failed to fetch player stats for {}: {}", playerId, e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<PlayerSeasonStats> extractMartStats(String sql) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                PlayerSeasonStats stats = new PlayerSeasonStats();
                stats.setPlayerId(rs.getLong("player_id"));
                long sId = rs.getLong("season_id");
                stats.setSeasonId(sId);
                stats.setSeasonName("Season " + sId);
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
                stats.setPassCompletionRate(null);
                stats.setDuelWinRate(null);

                applyDisplayRadarFromRealPer90(stats);
                return Optional.of(stats);
            }
        } catch (Exception e) {
            log.debug("mart extraction failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Display-only radar axes scaled from real per-90 / volume stats.
     * Caps are explicit visualization scales (not invented performance claims).
     * Missing inputs → axis score 0 (UI should treat sparse radars carefully).
     */
    private void applyDisplayRadarFromRealPer90(PlayerSeasonStats stats) {
        double g90 = stats.getGoalsPer90() != null ? stats.getGoalsPer90() : 0.0;
        double a90 = stats.getAssistsPer90() != null ? stats.getAssistsPer90() : 0.0;
        double xg90 = stats.getXgPer90() != null ? stats.getXgPer90() : 0.0;
        double shots90 = stats.getShotsPer90() != null ? stats.getShotsPer90() : 0.0;
        int kp = stats.getKeyPasses() != null ? stats.getKeyPasses() : 0;
        int tkl = stats.getTackles() != null ? stats.getTackles() : 0;
        int pressures = stats.getPressures() != null ? stats.getPressures() : 0;
        int duels = stats.getDuels() != null ? stats.getDuels() : 0;
        int minutes = stats.getMinutes() != null ? stats.getMinutes() : 0;

        stats.setFinishingRating(scale01(Math.max(g90 / 1.0, xg90 / 1.0)));
        stats.setCreationRating(scale01(Math.max(a90 / 0.8, (minutes > 0 ? (kp * 90.0 / minutes) : 0) / 3.0)));
        stats.setProgressionRating(scale01(shots90 / 4.0));
        stats.setPressingRating(scale01((minutes > 0 ? (pressures * 90.0 / minutes) : 0) / 25.0));
        stats.setDefendingRating(scale01((minutes > 0 ? (tkl * 90.0 / minutes) : 0) / 4.0));
        stats.setAerialRating(scale01((minutes > 0 ? (duels * 90.0 / minutes) : 0) / 15.0));
    }

    private int scale01(double ratio) {
        if (Double.isNaN(ratio) || ratio <= 0) return 0;
        return (int) Math.round(Math.min(100.0, ratio * 100.0));
    }

    public List<ShotEvent> getPlayerShots(Long playerId, Long seasonId) {
        List<ShotEvent> list = new ArrayList<>();
        // fact_event has no xG column — do not invent geometric xG / body part / situation
        String sql = "SELECT cast(fe.event_id as text) as event_id, fe.match_id, fe.player_id, coalesce(dp.name, '') as player_name, " +
                     "fe.team_id, fe.minute, fe.second, fe.x, fe.y, fe.outcome " +
                     "FROM fact_event fe " +
                     "LEFT JOIN dim_player dp ON fe.player_id = dp.player_id " +
                     "WHERE fe.player_id = ? AND (fe.event_type IN ('SHOT', 'GOAL') OR fe.outcome = 'GOAL') " +
                     "ORDER BY fe.match_id, fe.minute, fe.second LIMIT 50";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String outcome = rs.getString("outcome");
                    list.add(new ShotEvent(
                        rs.getString("event_id"),
                        rs.getLong("match_id"),
                        rs.getLong("player_id"),
                        rs.getString("player_name"),
                        rs.getLong("team_id"),
                        rs.getInt("minute"),
                        rs.getInt("second"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        null, // xG unavailable in event store
                        outcome != null && !outcome.isBlank() ? outcome : "SHOT",
                        null,
                        null
                    ));
                }
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


