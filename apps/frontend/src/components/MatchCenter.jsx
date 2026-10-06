import React, { useState, useEffect, useMemo } from 'react';
import { api } from '../services/api';

function dateKey(isoStr) {
  if (!isoStr) return 'unknown';
  const d = new Date(isoStr);
  if (isNaN(d)) return 'unknown';
  // Local calendar day key for grouping (YYYY-MM-DD)
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function formatDateHeader(key) {
  if (key === 'unknown') return 'Không rõ ngày';
  const d = new Date(`${key}T12:00:00`);
  if (isNaN(d)) return key;
  const today = new Date();
  const todayKey = dateKey(today.toISOString());
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);
  const yKey = dateKey(yesterday.toISOString());
  const tomorrow = new Date(today);
  tomorrow.setDate(today.getDate() + 1);
  const tKey = dateKey(tomorrow.toISOString());

  const base = d.toLocaleDateString('vi-VN', {
    weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric',
  });
  if (key === todayKey) return `Hôm nay · ${base}`;
  if (key === yKey) return `Hôm qua · ${base}`;
  if (key === tKey) return `Ngày mai · ${base}`;
  return base;
}

function formatKickoff(isoStr) {
  if (!isoStr) return { time: '--:--', date: '' };
  const d = new Date(isoStr);
  if (isNaN(d)) return { time: '--:--', date: '' };
  // Midnight UTC often means date-only (StatsBomb) — hide misleading 07:00 local
  const isDateOnly = d.getUTCHours() === 0 && d.getUTCMinutes() === 0 && d.getUTCSeconds() === 0;
  return {
    time: isDateOnly
      ? ''
      : d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
    date: d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }),
    isDateOnly,
  };
}

function TeamCrest({ src, name, size = 44 }) {
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
        style={{ width: size, height: size, fontSize: size * 0.32 }}
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
      style={{ width: size, height: size }}
      onError={() => setErr(true)}
    />
  );
}

function statusLabel(status) {
  if (status === 'LIVE') return '● LIVE';
  if (status === 'FINISHED') return 'Kết thúc';
  if (status === 'SCHEDULED') return 'Sắp đá';
  if (status === 'POSTPONED') return 'Hoãn';
  return status || '—';
}

export default function MatchCenter() {
  const [matches, setMatches] = useState([]);
  const [competitions, setCompetitions] = useState([]);
  const [selectedComp, setSelectedComp] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function loadData() {
      setLoading(true);
      setError(null);
      try {
        const compRes = await api.getCompetitions();
        setCompetitions(compRes.data || []);

        const params = { limit: 150 };
        if (selectedComp !== 'ALL') params.competition_id = selectedComp;
        if (statusFilter !== 'ALL') params.status = statusFilter;

        const matchRes = await api.getMatches(params);
        const list = Array.isArray(matchRes.data) ? [...matchRes.data] : [];
        // Newest first (API should already, but enforce client-side)
        list.sort((a, b) => {
          const ta = a.match_date ? new Date(a.match_date).getTime() : 0;
          const tb = b.match_date ? new Date(b.match_date).getTime() : 0;
          return tb - ta;
        });
        setMatches(list);
      } catch (err) {
        console.error(err);
        setError('Không thể tải trận đấu. Kiểm tra kết nối API và thử lại.');
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, [selectedComp, statusFilter]);

  const grouped = useMemo(() => {
    const map = new Map();
    for (const m of matches) {
      const key = dateKey(m.match_date);
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(m);
    }
    // Sort matches within each day by kickoff desc
    for (const [, arr] of map) {
      arr.sort((a, b) => {
        const ta = a.match_date ? new Date(a.match_date).getTime() : 0;
        const tb = b.match_date ? new Date(b.match_date).getTime() : 0;
        return tb - ta;
      });
    }
    // Date keys newest first
    return [...map.entries()].sort((a, b) => b[0].localeCompare(a[0]));
  }, [matches]);

  const newest = matches[0]?.match_date ? formatKickoff(matches[0].match_date).date : null;
  const oldest = matches.length
    ? formatKickoff(matches[matches.length - 1].match_date).date
    : null;

  return (
    <div>
      <div className="section-header">
        <div>
          <h1 className="section-title">Trận đấu</h1>
          <p className="section-subtitle">
            {matches.length > 0
              ? `${matches.length} trận · ${newest && oldest ? `${oldest} → ${newest}` : 'đã tải'}`
              : 'Lịch và kết quả theo ngày mới nhất'}
          </p>
        </div>

        <div className="filter-bar" style={{ flexWrap: 'wrap' }}>
          <button
            className={`filter-btn ${selectedComp === 'ALL' ? 'active' : ''}`}
            onClick={() => setSelectedComp('ALL')}
          >
            Tất cả giải
          </button>
          {competitions.slice(0, 12).map((c) => (
            <button
              key={c.competition_id}
              className={`filter-btn ${selectedComp === String(c.competition_id) ? 'active' : ''}`}
              onClick={() => setSelectedComp(String(c.competition_id))}
            >
              {c.name}
            </button>
          ))}

          <div className="filter-divider" />

          {[
            { id: 'ALL', label: 'Tất cả' },
            { id: 'LIVE', label: 'Live' },
            { id: 'SCHEDULED', label: 'Sắp đá' },
            { id: 'FINISHED', label: 'Đã đá' },
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
      </div>

      {loading ? (
        <div className="empty-state">Đang tải trận đấu…</div>
      ) : error ? (
        <div className="empty-state" style={{ color: 'var(--crimson-danger)' }}>{error}</div>
      ) : matches.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-title">Chưa có trận trong bộ lọc này</div>
          <div>
            Thử chọn “Tất cả” hoặc đợi pipeline đồng bộ (cần cấu hình FOOTBALL_DATA_API_KEY trên GitHub Secrets).
          </div>
        </div>
      ) : (
        <div className="match-day-list">
          {grouped.map(([key, dayMatches]) => (
            <section key={key} className="match-day-section">
              <div className="match-day-header">
                <h2>{formatDateHeader(key)}</h2>
                <span>{dayMatches.length} trận</span>
              </div>

              <div className="matches-grid">
                {dayMatches.map((m) => {
                  const kick = formatKickoff(m.match_date);
                  const isLive = m.status === 'LIVE';
                  const hasXg = (m.home_xg != null && m.home_xg > 0) || (m.away_xg != null && m.away_xg > 0);
                  const totalXg = (m.home_xg || 0) + (m.away_xg || 0);
                  const homeXgPct = totalXg > 0 ? ((m.home_xg || 0) / totalXg) * 100 : 50;
                  const showScore = m.status === 'FINISHED' || m.status === 'LIVE';

                  return (
                    <article key={m.match_id} className={`card match-card ${isLive ? 'live' : ''}`}>
                      <div className="match-header">
                        <span className="match-comp">{m.competition_name || 'Giải đấu'}</span>
                        <div className="match-meta-right">
                          {kick.time && <span className="match-kickoff">{kick.time}</span>}
                          <span className={`status-badge ${(m.status || '').toLowerCase()}`}>
                            {statusLabel(m.status)}
                          </span>
                        </div>
                      </div>

                      <div className="teams-scoreboard">
                        <div className="team-entry">
                          <TeamCrest src={m.home_team_logo} name={m.home_team_name} />
                          <span className="team-name">{m.home_team_name || '—'}</span>
                        </div>

                        <div className="score-display">
                          <div className="score-digits">
                            {showScore
                              ? `${m.home_score ?? 0} – ${m.away_score ?? 0}`
                              : 'vs'}
                          </div>
                          <span className="score-date">{kick.date}</span>
                        </div>

                        <div className="team-entry">
                          <TeamCrest src={m.away_team_logo} name={m.away_team_name} />
                          <span className="team-name">{m.away_team_name || '—'}</span>
                        </div>
                      </div>

                      {hasXg && (
                        <div className="xg-bar-container">
                          <div className="xg-labels">
                            <span>xG {(m.home_xg || 0).toFixed(2)}</span>
                            <span>Expected Goals</span>
                            <span>xG {(m.away_xg || 0).toFixed(2)}</span>
                          </div>
                          <div className="xg-progress-track">
                            <div className="xg-fill-home" style={{ width: `${homeXgPct}%` }} />
                            <div className="xg-fill-away" style={{ width: `${100 - homeXgPct}%` }} />
                          </div>
                        </div>
                      )}

                      {m.stadium ? (
                        <div className="match-venue">{m.stadium}</div>
                      ) : null}
                    </article>
                  );
                })}
              </div>
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
