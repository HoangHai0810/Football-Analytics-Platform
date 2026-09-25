import React, { useState, useEffect } from 'react';
import { api } from '../services/api';

export default function MatchCenter({ onSelectPlayer }) {
  const [matches, setMatches] = useState([]);
  const [competitions, setCompetitions] = useState([]);
  const [selectedComp, setSelectedComp] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadData() {
      setLoading(true);
      try {
        const compRes = await api.getCompetitions();
        setCompetitions(compRes.data || []);

        const params = {};
        if (selectedComp !== 'ALL') params.competition_id = selectedComp;
        if (statusFilter !== 'ALL') params.status = statusFilter;

        const matchRes = await api.getMatches(params);
        setMatches(matchRes.data || []);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, [selectedComp, statusFilter]);

  const formatDate = (isoStr) => {
    if (!isoStr) return '';
    const d = new Date(isoStr);
    return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div>
      <div className="section-header">
        <div>
          <h1 className="section-title">
            <span>🏟️</span> Match Center & Expected Goals (xG)
          </h1>
          <p className="section-subtitle">
            Real-time event tracking, team performance, and statistical expected scoreline validation
          </p>
        </div>

        <div className="filter-bar">
          <button
            className={`filter-btn ${selectedComp === 'ALL' ? 'active' : ''}`}
            onClick={() => setSelectedComp('ALL')}
          >
            All Competitions
          </button>
          {competitions.map((c) => (
            <button
              key={c.competition_id}
              className={`filter-btn ${selectedComp === String(c.competition_id) ? 'active' : ''}`}
              onClick={() => setSelectedComp(String(c.competition_id))}
            >
              {c.name}
            </button>
          ))}

          <div style={{ width: '1px', height: '24px', background: 'var(--border-subtle)', margin: '0 4px' }} />

          {['ALL', 'FINISHED', 'LIVE'].map((s) => (
            <button
              key={s}
              className={`filter-btn ${statusFilter === s ? 'active' : ''}`}
              onClick={() => setStatusFilter(s)}
            >
              {s === 'ALL' ? 'All Status' : s}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-secondary)' }}>
          Loading match analytical data...
        </div>
      ) : (
        <div className="matches-grid">
          {matches.map((m) => {
            const totalXg = (m.home_xg || 0) + (m.away_xg || 0);
            const homeXgPercent = totalXg > 0 ? ((m.home_xg || 0) / totalXg) * 100 : 50;

            return (
              <div key={m.match_id} className={`card match-card ${m.status === 'LIVE' ? 'live' : ''}`}>
                <div className="match-header">
                  <span>{m.competition_name}</span>
                  <span className={`status-badge ${m.status.toLowerCase()}`}>
                    {m.status === 'LIVE' ? '● LIVE' : m.status}
                  </span>
                </div>

                <div className="teams-scoreboard">
                  <div className="team-entry">
                    <img src={m.home_team_logo} alt={m.home_team_name} className="team-crest" />
                    <span className="team-name">{m.home_team_name}</span>
                  </div>

                  <div className="score-display">
                    <div className="score-digits">
                      {m.home_score ?? 0} - {m.away_score ?? 0}
                    </div>
                    <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                      {formatDate(m.match_date)}
                    </span>
                  </div>

                  <div className="team-entry">
                    <img src={m.away_team_logo} alt={m.away_team_name} className="team-crest" />
                    <span className="team-name">{m.away_team_name}</span>
                  </div>
                </div>

                <div className="xg-bar-container">
                  <div className="xg-labels">
                    <span>xG: {m.home_xg?.toFixed(2)}</span>
                    <span style={{ fontWeight: 600, color: 'var(--text-muted)' }}>Expected Goals</span>
                    <span>xG: {m.away_xg?.toFixed(2)}</span>
                  </div>
                  <div className="xg-progress-track">
                    <div className="xg-fill-home" style={{ width: `${homeXgPercent}%` }} />
                    <div className="xg-fill-away" style={{ width: `${100 - homeXgPercent}%` }} />
                  </div>
                </div>

                <div style={{ marginTop: '12px', display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: 'var(--text-muted)' }}>
                  <span>🏟️ {m.stadium}</span>
                  <span>👥 {m.attendance?.toLocaleString()} spectators</span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
