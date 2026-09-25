package com.football.analytics.repository;

import com.football.analytics.model.*;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class SeedDataStore {
    private final Map<Long, Competition> competitions = new ConcurrentHashMap<>();
    private final Map<Long, List<Season>> seasons = new ConcurrentHashMap<>();
    private final Map<Long, Team> teams = new ConcurrentHashMap<>();
    private final Map<Long, Player> players = new ConcurrentHashMap<>();
    private final Map<Long, Match> matches = new ConcurrentHashMap<>();
    private final Map<Long, List<ShotEvent>> playerShots = new ConcurrentHashMap<>();
    private final Map<String, PlayerSeasonStats> playerStats = new ConcurrentHashMap<>();

    @PostConstruct
    public void initData() {
        populateCompetitions();
        populateTeams();
        populatePlayers();
        populateMatches();
        populatePlayerSeasonStats();
        populateShotEvents();
    }

    private void populateCompetitions() {
        competitions.put(2021L, new Competition(2021L, "Premier League", "England", "LEAGUE", LocalDateTime.now()));
        competitions.put(2001L, new Competition(2001L, "UEFA Champions League", "Europe", "CUP", LocalDateTime.now()));
        competitions.put(2014L, new Competition(2014L, "La Liga", "Spain", "LEAGUE", LocalDateTime.now()));

        seasons.put(2021L, List.of(
            new Season(2024L, 2021L, "2024/2025", LocalDate.of(2024, 8, 16), LocalDate.of(2025, 5, 25)),
            new Season(2023L, 2021L, "2023/2024", LocalDate.of(2023, 8, 11), LocalDate.of(2024, 5, 19))
        ));

        seasons.put(2001L, List.of(
            new Season(2024L, 2001L, "2024/2025", LocalDate.of(2024, 9, 17), LocalDate.of(2025, 5, 31))
        ));

        seasons.put(2014L, List.of(
            new Season(2024L, 2014L, "2024/2025", LocalDate.of(2024, 8, 15), LocalDate.of(2025, 5, 25))
        ));
    }

    private void populateTeams() {
        teams.put(65L, new Team(65L, "Manchester City FC", "Man City", "England", "Etihad Stadium", "https://crests.football-data.org/65.png"));
        teams.put(57L, new Team(57L, "Arsenal FC", "Arsenal", "England", "Emirates Stadium", "https://crests.football-data.org/57.png"));
        teams.put(64L, new Team(64L, "Liverpool FC", "Liverpool", "England", "Anfield", "https://crests.football-data.org/64.png"));
        teams.put(86L, new Team(86L, "Real Madrid CF", "Real Madrid", "Spain", "Santiago Bernabéu", "https://crests.football-data.org/86.png"));
        teams.put(81L, new Team(81L, "FC Barcelona", "Barcelona", "Spain", "Spotify Camp Nou", "https://crests.football-data.org/81.png"));
    }

    private void populatePlayers() {
        players.put(1024L, new Player(1024L, 65L, "Manchester City FC", "Erling Haaland", LocalDate.of(2000, 7, 21), "Norway", "FW", "LEFT", 9, "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=150"));
        players.put(1032L, new Player(1032L, 65L, "Manchester City FC", "Kevin De Bruyne", LocalDate.of(1991, 6, 28), "Belgium", "MF", "RIGHT", 17, "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=150"));
        players.put(1035L, new Player(1035L, 65L, "Manchester City FC", "Rodri", LocalDate.of(1996, 6, 22), "Spain", "MF", "RIGHT", 16, "https://images.unsplash.com/photo-1517466787929-bc90951d0974?w=150"));
        players.put(1045L, new Player(1045L, 57L, "Arsenal FC", "Bukayo Saka", LocalDate.of(2001, 9, 5), "England", "FW", "LEFT", 7, "https://images.unsplash.com/photo-1543351611-58f69d7c1781?w=150"));
        players.put(1046L, new Player(1046L, 57L, "Arsenal FC", "Martin Ødegaard", LocalDate.of(1998, 12, 17), "Norway", "MF", "LEFT", 8, "https://images.unsplash.com/photo-1517466787929-bc90951d0974?w=150"));
        players.put(1050L, new Player(1050L, 64L, "Liverpool FC", "Mohamed Salah", LocalDate.of(1992, 6, 15), "Egypt", "FW", "LEFT", 11, "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=150"));
        players.put(1088L, new Player(1088L, 86L, "Real Madrid CF", "Kylian Mbappé", LocalDate.of(1998, 12, 20), "France", "FW", "RIGHT", 9, "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=150"));
        players.put(1089L, new Player(1089L, 86L, "Real Madrid CF", "Jude Bellingham", LocalDate.of(2003, 6, 29), "England", "MF", "RIGHT", 5, "https://images.unsplash.com/photo-1543351611-58f69d7c1781?w=150"));
    }

    private void populateMatches() {
        matches.put(3890251L, new Match(3890251L, 2021L, "Premier League", 2024L,
            65L, "Manchester City FC", "https://crests.football-data.org/65.png",
            57L, "Arsenal FC", "https://crests.football-data.org/57.png",
            2, 2, 2.38, 0.95,
            LocalDateTime.of(2024, 9, 22, 16, 30), "FINISHED", "Etihad Stadium", 52846L));

        matches.put(3890252L, new Match(3890252L, 2021L, "Premier League", 2024L,
            64L, "Liverpool FC", "https://crests.football-data.org/64.png",
            65L, "Manchester City FC", "https://crests.football-data.org/65.png",
            2, 0, 2.15, 1.20,
            LocalDateTime.of(2024, 12, 1, 16, 0), "FINISHED", "Anfield", 60124L));

        matches.put(3890253L, new Match(3890253L, 2014L, "La Liga", 2024L,
            86L, "Real Madrid CF", "https://crests.football-data.org/86.png",
            81L, "FC Barcelona", "https://crests.football-data.org/81.png",
            0, 4, 1.45, 2.82,
            LocalDateTime.of(2024, 10, 26, 20, 0), "FINISHED", "Santiago Bernabéu", 78192L));

        matches.put(3890254L, new Match(3890254L, 2001L, "UEFA Champions League", 2024L,
            65L, "Manchester City FC", "https://crests.football-data.org/65.png",
            86L, "Real Madrid CF", "https://crests.football-data.org/86.png",
            3, 1, 2.65, 1.10,
            LocalDateTime.of(2024, 11, 27, 20, 0), "FINISHED", "Etihad Stadium", 53120L));

        matches.put(3890255L, new Match(3890255L, 2021L, "Premier League", 2024L,
            57L, "Arsenal FC", "https://crests.football-data.org/57.png",
            64L, "Liverpool FC", "https://crests.football-data.org/64.png",
            2, 2, 1.85, 1.92,
            LocalDateTime.of(2024, 10, 27, 16, 30), "FINISHED", "Emirates Stadium", 60238L));

        matches.put(3890256L, new Match(3890256L, 2021L, "Premier League", 2024L,
            65L, "Manchester City FC", "https://crests.football-data.org/65.png",
            64L, "Liverpool FC", "https://crests.football-data.org/64.png",
            0, 0, 0.42, 0.35,
            LocalDateTime.now().plusHours(2), "LIVE", "Etihad Stadium", 53400L));
    }

    private void populatePlayerSeasonStats() {
        // Erling Haaland - 2024/2025
        PlayerSeasonStats haaland = new PlayerSeasonStats();
        haaland.setPlayerId(1024L);
        haaland.setSeasonId(2024L);
        haaland.setSeasonName("2024/2025");
        haaland.setMatchesPlayed(24);
        haaland.setMinutes(2120);
        haaland.setGoals(22);
        haaland.setAssists(4);
        haaland.setXg(18.75);
        haaland.setXa(3.20);
        haaland.setShots(88);
        haaland.setShotsOnTarget(48);
        haaland.setPasses(380);
        haaland.setKeyPasses(22);
        haaland.setPassCompletionRate(78.5);
        haaland.setGoalsPer90(0.93);
        haaland.setAssistsPer90(0.17);
        haaland.setXgPer90(0.80);
        haaland.setXaPer90(0.14);
        haaland.setShotsPer90(3.74);
        haaland.setTackles(8);
        haaland.setInterceptions(4);
        haaland.setDuels(142);
        haaland.setDuelWinRate(52.4);
        haaland.setPressures(165);
        haaland.setFinishingRating(98);
        haaland.setCreationRating(72);
        haaland.setProgressionRating(68);
        haaland.setPressingRating(65);
        haaland.setAerialRating(89);
        haaland.setDefendingRating(42);
        playerStats.put("1024:2024", haaland);

        // Kylian Mbappé - 2024/2025
        PlayerSeasonStats mbappe = new PlayerSeasonStats();
        mbappe.setPlayerId(1088L);
        mbappe.setSeasonId(2024L);
        mbappe.setSeasonName("2024/2025");
        mbappe.setMatchesPlayed(23);
        mbappe.setMinutes(1980);
        mbappe.setGoals(18);
        mbappe.setAssists(5);
        mbappe.setXg(16.40);
        mbappe.setXa(4.80);
        mbappe.setShots(82);
        mbappe.setShotsOnTarget(42);
        mbappe.setPasses(512);
        mbappe.setKeyPasses(34);
        mbappe.setPassCompletionRate(83.2);
        mbappe.setGoalsPer90(0.82);
        mbappe.setAssistsPer90(0.23);
        mbappe.setXgPer90(0.75);
        mbappe.setXaPer90(0.22);
        mbappe.setShotsPer90(3.73);
        mbappe.setTackles(12);
        mbappe.setInterceptions(6);
        mbappe.setDuels(158);
        mbappe.setDuelWinRate(48.2);
        mbappe.setPressures(140);
        mbappe.setFinishingRating(94);
        mbappe.setCreationRating(84);
        mbappe.setProgressionRating(91);
        mbappe.setPressingRating(58);
        mbappe.setAerialRating(62);
        mbappe.setDefendingRating(38);
        playerStats.put("1088:2024", mbappe);

        // Bukayo Saka - 2024/2025
        PlayerSeasonStats saka = new PlayerSeasonStats();
        saka.setPlayerId(1045L);
        saka.setSeasonId(2024L);
        saka.setSeasonName("2024/2025");
        saka.setMatchesPlayed(22);
        saka.setMinutes(1890);
        saka.setGoals(12);
        saka.setAssists(11);
        saka.setXg(9.80);
        saka.setXa(10.45);
        saka.setShots(54);
        saka.setShotsOnTarget(26);
        saka.setPasses(740);
        saka.setKeyPasses(58);
        saka.setPassCompletionRate(81.8);
        saka.setGoalsPer90(0.57);
        saka.setAssistsPer90(0.52);
        saka.setXgPer90(0.47);
        saka.setXaPer90(0.50);
        saka.setShotsPer90(2.57);
        saka.setTackles(38);
        saka.setInterceptions(18);
        saka.setDuels(198);
        saka.setDuelWinRate(55.2);
        saka.setPressures(280);
        saka.setFinishingRating(86);
        saka.setCreationRating(95);
        saka.setProgressionRating(93);
        saka.setPressingRating(82);
        saka.setAerialRating(54);
        saka.setDefendingRating(68);
        playerStats.put("1045:2024", saka);

        // Kevin De Bruyne - 2024/2025
        PlayerSeasonStats kdb = new PlayerSeasonStats();
        kdb.setPlayerId(1032L);
        kdb.setSeasonId(2024L);
        kdb.setSeasonName("2024/2025");
        kdb.setMatchesPlayed(18);
        kdb.setMinutes(1450);
        kdb.setGoals(6);
        kdb.setAssists(14);
        kdb.setXg(4.20);
        kdb.setXa(12.80);
        kdb.setShots(38);
        kdb.setShotsOnTarget(16);
        kdb.setPasses(910);
        kdb.setKeyPasses(68);
        kdb.setPassCompletionRate(84.5);
        kdb.setGoalsPer90(0.37);
        kdb.setAssistsPer90(0.87);
        kdb.setXgPer90(0.26);
        kdb.setXaPer90(0.79);
        kdb.setShotsPer90(2.36);
        kdb.setTackles(22);
        kdb.setInterceptions(14);
        kdb.setDuels(110);
        kdb.setDuelWinRate(46.8);
        kdb.setPressures(190);
        kdb.setFinishingRating(82);
        kdb.setCreationRating(99);
        kdb.setProgressionRating(96);
        kdb.setPressingRating(70);
        kdb.setAerialRating(50);
        kdb.setDefendingRating(55);
        playerStats.put("1032:2024", kdb);

        // Rodri - 2024/2025
        PlayerSeasonStats rodri = new PlayerSeasonStats();
        rodri.setPlayerId(1035L);
        rodri.setSeasonId(2024L);
        rodri.setSeasonName("2024/2025");
        rodri.setMatchesPlayed(21);
        rodri.setMinutes(1880);
        rodri.setGoals(5);
        rodri.setAssists(6);
        rodri.setXg(3.40);
        rodri.setXa(5.10);
        rodri.setShots(30);
        rodri.setShotsOnTarget(12);
        rodri.setPasses(1820);
        rodri.setKeyPasses(38);
        rodri.setPassCompletionRate(92.8);
        rodri.setGoalsPer90(0.24);
        rodri.setAssistsPer90(0.29);
        rodri.setXgPer90(0.16);
        rodri.setXaPer90(0.24);
        rodri.setShotsPer90(1.44);
        rodri.setTackles(58);
        rodri.setInterceptions(36);
        rodri.setDuels(220);
        rodri.setDuelWinRate(67.4);
        rodri.setPressures(310);
        rodri.setFinishingRating(75);
        rodri.setCreationRating(89);
        rodri.setProgressionRating(97);
        rodri.setPressingRating(88);
        rodri.setAerialRating(84);
        rodri.setDefendingRating(94);
        playerStats.put("1035:2024", rodri);

        // Mohamed Salah - 2024/2025
        PlayerSeasonStats salah = new PlayerSeasonStats();
        salah.setPlayerId(1050L);
        salah.setSeasonId(2024L);
        salah.setSeasonName("2024/2025");
        salah.setMatchesPlayed(25);
        salah.setMinutes(2200);
        salah.setGoals(21);
        salah.setAssists(13);
        salah.setXg(17.90);
        salah.setXa(11.20);
        salah.setShots(84);
        salah.setShotsOnTarget(44);
        salah.setPasses(810);
        salah.setKeyPasses(62);
        salah.setPassCompletionRate(80.5);
        salah.setGoalsPer90(0.86);
        salah.setAssistsPer90(0.53);
        salah.setXgPer90(0.73);
        salah.setXaPer90(0.46);
        salah.setShotsPer90(3.44);
        salah.setTackles(18);
        salah.setInterceptions(10);
        salah.setDuels(170);
        salah.setDuelWinRate(49.5);
        salah.setPressures(210);
        salah.setFinishingRating(96);
        salah.setCreationRating(94);
        salah.setProgressionRating(90);
        salah.setPressingRating(72);
        salah.setAerialRating(56);
        salah.setDefendingRating(45);
        playerStats.put("1050:2024", salah);
    }

    private void populateShotEvents() {
        // Pitch coordinates standard: x in [0.0..120.0], y in [0.0..80.0]
        // Goal mouth is at x = 120.0, y = [36.0..44.0] (center y = 40.0)
        List<ShotEvent> haalandShots = List.of(
            new ShotEvent("sh-01", 3890251L, 1024L, "Erling Haaland", 65L, 9, 14, 112.5, 41.2, 0.48, "GOAL", "LEFT_FOOT", "OPEN_PLAY"),
            new ShotEvent("sh-02", 3890251L, 1024L, "Erling Haaland", 65L, 24, 50, 106.2, 37.8, 0.22, "SAVED", "LEFT_FOOT", "OPEN_PLAY"),
            new ShotEvent("sh-03", 3890251L, 1024L, "Erling Haaland", 65L, 54, 12, 114.0, 39.5, 0.65, "GOAL", "HEAD", "CORNER"),
            new ShotEvent("sh-04", 3890251L, 1024L, "Erling Haaland", 65L, 71, 33, 102.0, 44.0, 0.12, "BLOCKED", "LEFT_FOOT", "OPEN_PLAY"),
            new ShotEvent("sh-05", 3890251L, 1024L, "Erling Haaland", 65L, 88, 45, 110.0, 36.5, 0.38, "MISSED", "RIGHT_FOOT", "OPEN_PLAY"),
            new ShotEvent("sh-06", 3890254L, 1024L, "Erling Haaland", 65L, 18, 20, 113.2, 40.1, 0.55, "GOAL", "LEFT_FOOT", "OPEN_PLAY"),
            new ShotEvent("sh-07", 3890254L, 1024L, "Erling Haaland", 65L, 39, 10, 108.0, 38.0, 0.76, "GOAL", "LEFT_FOOT", "PENALTY"),
            new ShotEvent("sh-08", 3890254L, 1024L, "Erling Haaland", 65L, 63, 40, 101.5, 46.0, 0.09, "SAVED", "LEFT_FOOT", "OPEN_PLAY")
        );
        playerShots.put(1024L, haalandShots);

        List<ShotEvent> mbappeShots = List.of(
            new ShotEvent("mb-01", 3890253L, 1088L, "Kylian Mbappé", 86L, 12, 30, 109.5, 33.2, 0.35, "SAVED", "RIGHT_FOOT", "OPEN_PLAY"),
            new ShotEvent("mb-02", 3890253L, 1088L, "Kylian Mbappé", 86L, 30, 15, 115.0, 42.0, 0.58, "GOAL", "RIGHT_FOOT", "OPEN_PLAY"),
            new ShotEvent("mb-03", 3890253L, 1088L, "Kylian Mbappé", 86L, 55, 40, 104.0, 31.0, 0.14, "BLOCKED", "RIGHT_FOOT", "OPEN_PLAY"),
            new ShotEvent("mb-04", 3890253L, 1088L, "Kylian Mbappé", 86L, 78, 12, 111.0, 39.8, 0.42, "GOAL", "RIGHT_FOOT", "OPEN_PLAY")
        );
        playerShots.put(1088L, mbappeShots);

        List<ShotEvent> sakaShots = List.of(
            new ShotEvent("sk-01", 3890255L, 1045L, "Bukayo Saka", 57L, 9, 22, 108.5, 48.0, 0.32, "GOAL", "LEFT_FOOT", "OPEN_PLAY"),
            new ShotEvent("sk-02", 3890255L, 1045L, "Bukayo Saka", 57L, 33, 40, 102.0, 52.5, 0.08, "SAVED", "LEFT_FOOT", "OPEN_PLAY"),
            new ShotEvent("sk-03", 3890255L, 1045L, "Bukayo Saka", 57L, 66, 15, 111.0, 43.5, 0.44, "GOAL", "LEFT_FOOT", "OPEN_PLAY")
        );
        playerShots.put(1045L, sakaShots);

        List<ShotEvent> salahShots = List.of(
            new ShotEvent("sl-01", 3890252L, 1050L, "Mohamed Salah", 64L, 15, 45, 109.0, 47.0, 0.38, "GOAL", "LEFT_FOOT", "OPEN_PLAY"),
            new ShotEvent("sl-02", 3890252L, 1050L, "Mohamed Salah", 64L, 77, 20, 108.0, 40.0, 0.76, "GOAL", "LEFT_FOOT", "PENALTY")
        );
        playerShots.put(1050L, salahShots);
    }

    public List<Competition> getAllCompetitions() {
        return new ArrayList<>(competitions.values());
    }

    public Optional<Competition> getCompetition(Long id) {
        return Optional.ofNullable(competitions.get(id));
    }

    public List<Season> getSeasonsByCompetition(Long competitionId) {
        return seasons.getOrDefault(competitionId, Collections.emptyList());
    }

    public List<Team> getAllTeams() {
        return new ArrayList<>(teams.values());
    }

    public Optional<Team> getTeam(Long id) {
        return Optional.ofNullable(teams.get(id));
    }

    public List<Player> getAllPlayers(String query, String position) {
        return players.values().stream()
            .filter(p -> query == null || query.isBlank() || p.getName().toLowerCase().contains(query.toLowerCase()) || p.getTeamName().toLowerCase().contains(query.toLowerCase()))
            .filter(p -> position == null || position.isBlank() || p.getPosition().equalsIgnoreCase(position))
            .collect(Collectors.toList());
    }

    public Optional<Player> getPlayer(Long id) {
        return Optional.ofNullable(players.get(id));
    }

    public Optional<PlayerSeasonStats> getPlayerStats(Long playerId, Long seasonId) {
        String key = playerId + ":" + seasonId;
        return Optional.ofNullable(playerStats.get(key));
    }

    public List<ShotEvent> getPlayerShots(Long playerId, Long seasonId) {
        return playerShots.getOrDefault(playerId, Collections.emptyList());
    }

    public List<Match> getAllMatches(Long competitionId, Long seasonId, String status) {
        return matches.values().stream()
            .filter(m -> competitionId == null || m.getCompetitionId().equals(competitionId))
            .filter(m -> seasonId == null || m.getSeasonId().equals(seasonId))
            .filter(m -> status == null || status.isBlank() || m.getStatus().equalsIgnoreCase(status))
            .sorted(Comparator.comparing(Match::getMatchDate).reversed())
            .collect(Collectors.toList());
    }

    public Optional<Match> getMatch(Long id) {
        return Optional.ofNullable(matches.get(id));
    }

    public Map<String, Long> getEntityCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("competitions", (long) competitions.size());
        counts.put("teams", (long) teams.size());
        counts.put("players", (long) players.size());
        counts.put("matches", (long) matches.size());
        counts.put("shot_events", (long) playerShots.values().stream().mapToInt(List::size).sum());
        return counts;
    }
}
