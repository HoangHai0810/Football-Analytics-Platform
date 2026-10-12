import React, { useState, useEffect, useMemo, useRef } from 'react';
import { api } from '../services/api';
import MatchDetailModal from './MatchDetailModal';

function dateKey(isoStr) {
  if (!isoStr) return 'unknown';
  const d = new Date(isoStr);
  if (isNaN(d)) return 'unknown';
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function formatDateLabel(key) {
  if (key === 'ALL') return 'Tất cả các trận';
  if (key === 'unknown') return 'Khác';
  const d = new Date(`${key}T12:00:00`);
  if (isNaN(d)) return key;

  const now = new Date();
  const todayKey = dateKey(now.toISOString());
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  const yKey = dateKey(yesterday.toISOString());
  const tomorrow = new Date(now);
  tomorrow.setDate(now.getDate() + 1);
  const tKey = dateKey(tomorrow.toISOString());

  const dayMonth = `${d.getDate()}/${d.getMonth() + 1}`;
  const weekdayShort = d.toLocaleDateString('vi-VN', { weekday: 'short' });

  if (key === todayKey) return `Hôm nay · ${dayMonth}`;
  if (key === yKey) return `Hôm qua · ${dayMonth}`;
  if (key === tKey) return `Ngày mai · ${dayMonth}`;
  return `${weekdayShort} · ${dayMonth}`;
}

function formatKickoff(isoStr) {
  if (!isoStr) return { time: '', date: '' };
  const d = new Date(isoStr);
  if (isNaN(d)) return { time: '', date: '' };
  const isDateOnly = d.getUTCHours() === 0 && d.getUTCMinutes() === 0 && d.getUTCSeconds() === 0;
  return {
    time: isDateOnly ? '' : d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
    date: d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }),
    isDateOnly,
  };
}

function TeamCrest({ src, name, size = 32 }) {
  const [err, setErr] = useState(false);
  const initials = (name || '?')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0])
    .join('')
    .toUpperCase();

  if (!src || err) {
    return (
      <div
        className="team-crest-fallback"
        style={{
          width: size,
          height: size,
          fontSize: size * 0.36,
          borderRadius: 8,
          background: 'rgba(255, 255, 255, 0.08)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontWeight: 700,
          color: '#e2e8f0',
          flexShrink: 0,
        }}
        title={name}
      >
        {initials || '?'}
      </div>
    );
  }
  return (
    <img
      src={src}
      alt={name}
      className="team-crest"
      style={{
        width: size,
        height: size,
        objectFit: 'contain',
        flexShrink: 0,
        filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.4))',
      }}
      onError={() => setErr(true)}
    />
  );
}

export default function MatchCenter({ onAskAi }) {
  const [allMatches, setAllMatches] = useState([]);
  const [competitions, setCompetitions] = useState([]);
  const [selectedComp, setSelectedComp] = useState('ALL');
  const [selectedDate, setSelectedDate] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedMatch, setSelectedMatch] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const hasInitializedDateRef = useRef(false);

  useEffect(() => {
    async function loadData() {
      setLoading(true);
      setError(null);
      try {
        const compRes = await api.getCompetitions().catch(() => ({ data: [] }));
        setCompetitions(compRes.data || []);

        // Load complete match schedule once so date ribbon stays permanent and stable
        const matchRes = await api.getMatches({ limit: 500 });
        const list = Array.isArray(matchRes.data) ? [...matchRes.data] : [];

        // Sort newest date first, with LIVE prioritized
        list.sort((a, b) => {
          if (a.status === 'LIVE' && b.status !== 'LIVE') return -1;
          if (b.status === 'LIVE' && a.status !== 'LIVE') return 1;
          const ta = a.match_date ? new Date(a.match_date).getTime() : 0;
          const tb = b.match_date ? new Date(b.match_date).getTime() : 0;
          return tb - ta;
        });

        setAllMatches(list);

        // Auto-select date only on initial component mount
        if (!hasInitializedDateRef.current && list.length > 0) {
          const now = new Date();
          const todayK = dateKey(now.toISOString());
          const dateKeysSet = new Set(list.map((m) => dateKey(m.match_date)));
          if (dateKeysSet.has(todayK)) {
            setSelectedDate(todayK);
          } else {
            const sortedDates = [...dateKeysSet].filter((k) => k !== 'unknown').sort((a, b) => a.localeCompare(b));
            // Find closest date to today or the latest available date
            const todayOrLater = sortedDates.find((d) => d >= todayK);
            setSelectedDate(todayOrLater || sortedDates[sortedDates.length - 1] || 'ALL');
          }
          hasInitializedDateRef.current = true;
        }
      } catch (err) {
        console.error(err);
        setError('Không thể tải dữ liệu trận đấu. Vui lòng kiểm tra lại kết nối.');
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  // Distinct dates sorted CHRONOLOGICALLY based on all matches (Rock-solid, never mutates when filtering tournament!)
  const chronologicalDates = useMemo(() => {
    const set = new Set();
    for (const m of allMatches) {
      const k = dateKey(m.match_date);
      if (k !== 'unknown') {
        set.add(k);
      }
    }
    return [...set].sort((a, b) => a.localeCompare(b));
  }, [allMatches]);

  // Distinct competitions deduplicated by name and with accurate match count
  const displayCompetitions = useMemo(() => {
    const compMap = new Map();

    // 1. Collect all tournaments that actually have matches in allMatches
    for (const m of allMatches) {
      const name = m.competition_name ? m.competition_name.trim() : null;
      if (!name) continue;
      const key = name.toLowerCase();
      if (!compMap.has(key)) {
        compMap.set(key, {
          competition_id: m.competition_id,
          name: name,
          count: 0,
        });
      }
      compMap.get(key).count += 1;
    }

    // 2. If allMatches is still empty, fallback to api competitions (deduplicated by name)
    if (compMap.size === 0 && Array.isArray(competitions)) {
      for (const c of competitions) {
        const name = c.name ? c.name.trim() : null;
        if (!name) continue;
        const key = name.toLowerCase();
        if (!compMap.has(key)) {
          compMap.set(key, {
            competition_id: c.competition_id,
            name: name,
            count: 0,
          });
        }
      }
    }

    // Sort by match count descending, then alphabetically
    return [...compMap.values()].sort((a, b) => b.count - a.count || a.name.localeCompare(b.name));
  }, [allMatches, competitions]);

  // Handle previous / next date navigation
  const handlePrevDate = () => {
    if (selectedDate === 'ALL') {
      if (chronologicalDates.length > 0) setSelectedDate(chronologicalDates[0]);
      return;
    }
    const idx = chronologicalDates.indexOf(selectedDate);
    if (idx > 0) {
      setSelectedDate(chronologicalDates[idx - 1]);
    }
  };

  const handleNextDate = () => {
    if (selectedDate === 'ALL') {
      if (chronologicalDates.length > 0) setSelectedDate(chronologicalDates[chronologicalDates.length - 1]);
      return;
    }
    const idx = chronologicalDates.indexOf(selectedDate);
    if (idx >= 0 && idx < chronologicalDates.length - 1) {
      setSelectedDate(chronologicalDates[idx + 1]);
    }
  };

  // Filter matches client-side: tournament, status, date, search
  const filteredMatches = useMemo(() => {
    let result = allMatches;

    if (selectedComp !== 'ALL') {
      const scLower = selectedComp.toLowerCase().trim();
      result = result.filter(
        (m) =>
          (m.competition_name && m.competition_name.toLowerCase().trim() === scLower) ||
          String(m.competition_id) === String(selectedComp)
      );
    }

    if (statusFilter !== 'ALL') {
      result = result.filter((m) => m.status === statusFilter);
    }

    if (selectedDate !== 'ALL') {
      result = result.filter((m) => dateKey(m.match_date) === selectedDate);
    }

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      result = result.filter(
        (m) =>
          (m.home_team_name && m.home_team_name.toLowerCase().includes(q)) ||
          (m.away_team_name && m.away_team_name.toLowerCase().includes(q)) ||
          (m.competition_name && m.competition_name.toLowerCase().includes(q)) ||
          (m.stadium && m.stadium.toLowerCase().includes(q))
      );
    }

    return result;
  }, [allMatches, selectedComp, statusFilter, selectedDate, searchQuery]);

  // Group filtered matches by LEAGUE / COMPETITION
  const leagueGroups = useMemo(() => {
    const groups = new Map();
    for (const m of filteredMatches) {
      const compName = m.competition_name || 'Giải đấu khác';
      if (!groups.has(compName)) {
        groups.set(compName, {
          competition_id: m.competition_id,
          competition_name: compName,
          matches: [],
        });
      }
      groups.get(compName).matches.push(m);
    }
    return [...groups.values()];
  }, [filteredMatches]);

  const currentSelectedCompObj = useMemo(() => {
    if (selectedComp === 'ALL') return null;
    return competitions.find((c) => String(c.competition_id) === String(selectedComp)) || null;
  }, [competitions, selectedComp]);

  return (
    <div className="match-center-container">
      {/* Top Banner & Header */}
      <div className="match-center-header">
        <div>
          <h1 className="section-title">
            <span>⚽</span> Lịch Thi Đấu & Kết Quả
          </h1>
          <p className="section-subtitle">
            Cập nhật tỷ số trực tiếp, thông số kỳ vọng bàn thắng (xG) và phân tích trận đấu
          </p>
        </div>

        {/* Search Input */}
        <div className="match-search-box">
          <input
            type="search"
            placeholder="Tìm theo tên đội hoặc giải…"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="search-input"
            style={{ width: '250px' }}
          />
        </div>
      </div>

      {/* Chronological Date Ribbon Strip (FotMob / Sofascore standard) */}
      <div className="date-navigation-wrapper">
        <button
          className="date-nav-arrow"
          onClick={handlePrevDate}
          title="Ngày trước đó"
          disabled={selectedDate === 'ALL' || chronologicalDates.indexOf(selectedDate) === 0}
        >
          ‹
        </button>

        <div className="date-ribbon-bar">
          <button
            className={`date-ribbon-btn ${selectedDate === 'ALL' ? 'active' : ''}`}
            onClick={() => setSelectedDate('ALL')}
          >
            <span className="date-label">Tất cả các trận</span>
          </button>

          {chronologicalDates.map((dKey) => (
            <button
              key={dKey}
              className={`date-ribbon-btn ${selectedDate === dKey ? 'active' : ''}`}
              onClick={() => setSelectedDate(dKey)}
            >
              <span className="date-label">{formatDateLabel(dKey)}</span>
            </button>
          ))}
        </div>

        <button
          className="date-nav-arrow"
          onClick={handleNextDate}
          title="Ngày kế tiếp"
          disabled={selectedDate === 'ALL' || chronologicalDates.indexOf(selectedDate) === chronologicalDates.length - 1}
        >
          ›
        </button>
      </div>

      {/* Filter Toolbar: Competitions & Status */}
      <div className="match-filter-toolbar">
        {/* Status Filter Pills */}
        <div className="status-filter-pills">
          {[
            { id: 'ALL', label: 'Tất cả' },
            { id: 'LIVE', label: '🔴 LIVE' },
            { id: 'SCHEDULED', label: 'Sắp diễn ra' },
            { id: 'FINISHED', label: 'Đã kết thúc' },
          ].map((s) => (
            <button
              key={s.id}
              className={`filter-btn ${statusFilter === s.id ? 'active' : ''}`}
              onClick={() => setStatusFilter(s.id)}
            >
              {s.label}
            </button>
          ))}
        </div>

        {/* Competition Dropdown */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Giải đấu:</span>
          <select
            className="search-input"
            value={selectedComp}
            onChange={(e) => setSelectedComp(e.target.value)}
            style={{ padding: '6px 12px', fontSize: '13px', width: 'auto' }}
          >
            <option value="ALL">Tất cả giải đấu ({displayCompetitions.length})</option>
            {displayCompetitions.map((c) => (
              <option key={c.name} value={c.name}>
                {c.name} {c.count ? `(${c.count} trận)` : ''}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Match Content List */}
      {loading ? (
        <div className="empty-state">
          <div className="empty-state-title">Đang đồng bộ dữ liệu trận đấu…</div>
        </div>
      ) : error ? (
        <div className="empty-state" style={{ color: 'var(--crimson-danger)' }}>
          {error}
        </div>
      ) : filteredMatches.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-title">
            {selectedComp !== 'ALL'
              ? `${selectedComp} không có trận đấu vào ngày này`
              : 'Không có trận đấu nào trong ngày này'}
          </div>
          <div style={{ marginTop: '8px', color: 'var(--text-muted)' }}>
            {selectedComp !== 'ALL' && selectedDate !== 'ALL' ? (
              <span>
                Thử chọn ngày khác trên thanh lịch phía trên, hoặc bấm nút bên dưới để xem toàn bộ lịch thi đấu của giải.
              </span>
            ) : (
              <span>
                Hãy chọn các ngày khác trên thanh lịch hoặc bấm tab <strong>"Tất cả các trận"</strong>.
              </span>
            )}
          </div>
          {selectedDate !== 'ALL' && (
            <div style={{ marginTop: '16px' }}>
              <button
                className="filter-btn active"
                onClick={() => setSelectedDate('ALL')}
                style={{ padding: '8px 18px', fontWeight: 700 }}
              >
                📅 Xem tất cả lịch của {selectedComp !== 'ALL' ? selectedComp : 'các giải'}
              </button>
            </div>
          )}
        </div>
      ) : (
        <div className="tournament-groups-list">
          {leagueGroups.map((group) => (
            <section key={group.competition_name} className="tournament-section">
              {/* League Header Banner */}
              <div className="tournament-header">
                <div className="tournament-header-left">
                  <span className="tournament-icon">🏆</span>
                  <h2 className="tournament-title">{group.competition_name}</h2>
                </div>
                <span className="tournament-match-count">{group.matches.length} trận</span>
              </div>

              {/* Match Rows / Cards */}
              <div className="tournament-matches-list">
                {group.matches.map((m) => {
                  const kick = formatKickoff(m.match_date);
                  const isLive = m.status === 'LIVE';
                  const isFinished = m.status === 'FINISHED';
                  const showScore = isLive || isFinished;
                  const hasXg = (m.home_xg != null && m.home_xg > 0) || (m.away_xg != null && m.away_xg > 0);

                  return (
                    <article
                      key={m.match_id}
                      className={`match-row-card ${isLive ? 'live-border' : ''}`}
                      onClick={() => setSelectedMatch(m)}
                      title="Bấm để xem chi tiết trận đấu và phân tích AI"
                    >
                      {/* Left: Kickoff / Status */}
                      <div className="match-row-time">
                        {isLive ? (
                          <span className="live-status-pill">LIVE</span>
                        ) : isFinished ? (
                          <span className="finished-status-text">FT</span>
                        ) : (
                          <span className="kickoff-time-text">{kick.time || 'Sắp đá'}</span>
                        )}
                        {kick.date && <span className="match-row-date">{kick.date}</span>}
                      </div>

                      {/* Middle: Teams & Scoreline */}
                      <div className="match-row-fixture">
                        {/* Home Team */}
                        <div className="fixture-team home">
                          <span className="fixture-team-name">{m.home_team_name}</span>
                          <TeamCrest src={m.home_team_logo} name={m.home_team_name} size={32} />
                        </div>

                        {/* Score Box */}
                        <div className="fixture-score-badge">
                          {showScore ? (
                            <span className="score-text">
                              {m.home_score ?? 0} – {m.away_score ?? 0}
                            </span>
                          ) : (
                            <span className="vs-text">vs</span>
                          )}
                        </div>

                        {/* Away Team */}
                        <div className="fixture-team away">
                          <TeamCrest src={m.away_team_logo} name={m.away_team_name} size={32} />
                          <span className="fixture-team-name">{m.away_team_name}</span>
                        </div>
                      </div>

                      {/* Right: xG Badge or Venue */}
                      <div className="match-row-meta">
                        {hasXg ? (
                          <div className="match-row-xg">
                            <span>xG {(m.home_xg || 0).toFixed(2)} - {(m.away_xg || 0).toFixed(2)}</span>
                          </div>
                        ) : m.stadium ? (
                          <div className="match-row-venue truncate" title={m.stadium}>
                            📍 {m.stadium}
                          </div>
                        ) : (
                          <div className="match-row-venue">
                            Chi tiết →
                          </div>
                        )}
                      </div>
                    </article>
                  );
                })}
              </div>
            </section>
          ))}
        </div>
      )}

      {/* Match Detail Modal Popup */}
      {selectedMatch && (
        <MatchDetailModal
          match={selectedMatch}
          onClose={() => setSelectedMatch(null)}
          onAskAi={onAskAi}
        />
      )}
    </div>
  );
}
