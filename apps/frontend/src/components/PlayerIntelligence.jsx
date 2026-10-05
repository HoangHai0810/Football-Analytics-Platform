import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import RadarChart from './RadarChart';
import PitchShotMap from './PitchShotMap';

export default function PlayerIntelligence({ onComparePlayer }) {
  const [players, setPlayers] = useState([]);
  const [selectedPlayer, setSelectedPlayer] = useState(null);
  const [playerStats, setPlayerStats] = useState(null);
  const [playerShots, setPlayerShots] = useState([]);
  const [activeTab, setActiveTab] = useState('radar'); // 'radar' | 'shotmap'
  const [searchQuery, setSearchQuery] = useState('');
  const [positionFilter, setPositionFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadPlayers() {
      setLoading(true);
      try {
        const res = await api.getPlayers({ query: searchQuery, position: positionFilter });
        const list = res.data || [];
        setPlayers(list);
        if (list.length > 0 && !selectedPlayer) {
          selectPlayer(list[0]);
        }
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    }
    loadPlayers();
  }, [searchQuery, positionFilter]);

  const selectPlayer = async (player) => {
    setSelectedPlayer(player);
    try {
      const statsRes = await api.getPlayerStats(player.player_id);
      setPlayerStats(statsRes.data);

      const shotsRes = await api.getPlayerShots(player.player_id);
      setPlayerShots(shotsRes.data || []);
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div>
      <div className="section-header">
        <div>
          <h1 className="section-title">
            <span>👤</span> Player Intelligence & Tactical Profiles
          </h1>
          <p className="section-subtitle">
            Comprehensive per-90 metrics, multi-dimensional radar polygons, and coordinate-precise shot maps
          </p>
        </div>

        <div className="filter-bar">
          <input
            type="text"
            placeholder="Search player or team..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              background: 'rgba(15, 23, 42, 0.8)',
              border: '1px solid var(--border-subtle)',
              color: '#fff',
              padding: '6px 14px',
              borderRadius: 'var(--radius-md)',
              fontSize: '13px',
              outline: 'none',
              minWidth: '220px'
            }}
          />

          {['ALL', 'FW', 'MF', 'DF'].map((pos) => (
            <button
              key={pos}
              className={`filter-btn ${positionFilter === pos ? 'active' : ''}`}
              onClick={() => setPositionFilter(pos)}
            >
              {pos === 'ALL' ? 'All Positions' : pos}
            </button>
          ))}
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(320px, 380px) 1fr', gap: '24px' }}>
        {/* Left Column: Player Cards List */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', maxHeight: '780px', overflowY: 'auto' }}>
          {loading ? (
            <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-secondary)' }}>
              Loading player data...
            </div>
          ) : (
            players.map((p) => {
              const isSelected = selectedPlayer?.player_id === p.player_id;
              return (
                <div
                  key={p.player_id}
                  className={`card player-card ${isSelected ? 'selected' : ''}`}
                  onClick={() => selectPlayer(p)}
                  style={{
                    borderColor: isSelected ? '#10b981' : undefined,
                    boxShadow: isSelected ? '0 0 20px rgba(16, 185, 129, 0.25)' : undefined
                  }}
                >
                  <div className="player-card-header">
                    <img
                      src={p.avatar_url || `https://ui-avatars.com/api/?name=${encodeURIComponent(p.name)}&background=0f172a&color=38bdf8&bold=true`}
                      alt={p.name}
                      className="player-avatar"
                      onError={(e) => {
                        e.currentTarget.onerror = null;
                        e.currentTarget.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(p.name)}&background=0f172a&color=38bdf8&bold=true`;
                      }}
                    />
                    <div>
                      <div className="player-name-text">{p.name}</div>
                      <div className="player-meta-info">
                        <span className="position-tag">{p.position}</span>
                        <span>{p.team_name}</span>
                        <span>• #{p.jersey_number}</span>
                      </div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: 'var(--text-muted)' }}>
                    <span>🌍 {p.nationality}</span>
                    <span>👟 Foot: {p.preferred_foot}</span>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Right Column: Active Player Analytics Detail */}
        {selectedPlayer && playerStats && (
          <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {/* Player Detailed Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
              <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
                <img
                  src={selectedPlayer.avatar_url || `https://ui-avatars.com/api/?name=${encodeURIComponent(selectedPlayer.name)}&background=0f172a&color=38bdf8&bold=true`}
                  alt={selectedPlayer.name}
                  style={{ width: '72px', height: '72px', borderRadius: 'var(--radius-md)', objectFit: 'cover', border: '3px solid #10b981' }}
                  onError={(e) => {
                    e.currentTarget.onerror = null;
                    e.currentTarget.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(selectedPlayer.name)}&background=0f172a&color=38bdf8&bold=true`;
                  }}
                />
                <div>
                  <h2 style={{ fontSize: '24px', fontWeight: 800, color: '#fff' }}>{selectedPlayer.name}</h2>
                  <div style={{ display: 'flex', gap: '10px', alignItems: 'center', marginTop: '4px', fontSize: '13px', color: 'var(--text-secondary)' }}>
                    <span className="position-tag">{selectedPlayer.position}</span>
                    <span style={{ fontWeight: 600 }}>{selectedPlayer.team_name}</span>
                    <span>• Season {playerStats.season_name}</span>
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '10px' }}>
                <button
                  className="filter-btn active"
                  onClick={() => onComparePlayer(selectedPlayer.player_id)}
                  style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                >
                  <span>⚔️</span> Compare Player
                </button>
              </div>
            </div>

            {/* Core Metrics Grid */}
            <div className="metrics-row" style={{ gridTemplateColumns: 'repeat(6, 1fr)' }}>
              <div>
                <div className="metric-item-val" style={{ color: '#10b981' }}>{playerStats.goals}</div>
                <div className="metric-item-lbl">Goals</div>
              </div>
              <div>
                <div className="metric-item-val" style={{ color: '#38bdf8' }}>{playerStats.assists}</div>
                <div className="metric-item-lbl">Assists</div>
              </div>
              <div>
                <div className="metric-item-val">{playerStats.xg?.toFixed(2)}</div>
                <div className="metric-item-lbl">xG Total</div>
              </div>
              <div>
                <div className="metric-item-val">{playerStats.goals_per_90?.toFixed(2)}</div>
                <div className="metric-item-lbl">Goals/90</div>
              </div>
              <div>
                <div className="metric-item-val">{playerStats.xg_per_90?.toFixed(2)}</div>
                <div className="metric-item-lbl">xG/90</div>
              </div>
              <div>
                <div className="metric-item-val" style={{ color: '#f59e0b' }}>{playerStats.pass_completion_rate}%</div>
                <div className="metric-item-lbl">Pass Acc %</div>
              </div>
            </div>

            {/* Visualizer Tab Selector: Radar vs Shot Map */}
            <div style={{ display: 'flex', borderBottom: '1px solid var(--border-subtle)', gap: '16px' }}>
              <button
                onClick={() => setActiveTab('radar')}
                style={{
                  background: 'none',
                  border: 'none',
                  borderBottom: activeTab === 'radar' ? '2px solid #10b981' : '2px solid transparent',
                  color: activeTab === 'radar' ? '#34d399' : 'var(--text-secondary)',
                  padding: '8px 16px',
                  fontWeight: 600,
                  fontSize: '14px',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px'
                }}
              >
                <span>🕸️</span> Ability Radar (Polygon)
              </button>

              <button
                onClick={() => setActiveTab('shotmap')}
                style={{
                  background: 'none',
                  border: 'none',
                  borderBottom: activeTab === 'shotmap' ? '2px solid #10b981' : '2px solid transparent',
                  color: activeTab === 'shotmap' ? '#34d399' : 'var(--text-secondary)',
                  padding: '8px 16px',
                  fontWeight: 600,
                  fontSize: '14px',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px'
                }}
              >
                <span>🎯</span> Pitch Shot Map ({playerShots.length} Events)
              </button>
            </div>

            {/* Visualizer Content */}
            {activeTab === 'radar' ? (
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <RadarChart
                  stats1={playerStats}
                  name1={selectedPlayer.name}
                  color1="#10b981"
                  size={360}
                />
                <p style={{ fontSize: '12px', color: 'var(--text-muted)', textAlign: 'center', marginTop: '12px' }}>
                  6-dimension radar normalized against elite top-5 European league benchmarks
                </p>
              </div>
            ) : (
              <PitchShotMap
                shots={playerShots}
                playerName={selectedPlayer.name}
              />
            )}
          </div>
        )}
      </div>
    </div>
  );
}
