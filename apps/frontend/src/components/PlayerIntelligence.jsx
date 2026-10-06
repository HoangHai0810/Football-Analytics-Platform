import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import RadarChart from './RadarChart';
import PitchShotMap from './PitchShotMap';

function PlayerAvatar({ src, name, size = 48 }) {
  const [err, setErr] = useState(false);
  const initials = (name || '?')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0])
    .join('')
    .toUpperCase();

  const hasPhoto = src && !err && !src.includes('ui-avatars') && !src.includes('crests.football-data');

  if (!hasPhoto) {
    return (
      <div
        className="player-avatar-fallback"
        style={{
          width: size,
          height: size,
          borderRadius: size > 60 ? '50%' : 10,
          fontSize: size * 0.32,
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
      style={{
        width: size,
        height: size,
        borderRadius: size > 60 ? '50%' : 10,
        objectFit: 'cover',
        border: size > 60 ? '3px solid #10b981' : 'none',
        background: 'rgba(15,23,42,0.8)',
        flexShrink: 0,
      }}
      onError={() => setErr(true)}
    />
  );
}

function StatBox({ value, label, color }) {
  return (
    <div style={{ textAlign: 'center' }}>
      <div style={{ fontSize: '22px', fontWeight: 800, color: color || '#fff', lineHeight: 1 }}>
        {value ?? '—'}
      </div>
      <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '4px' }}>{label}</div>
    </div>
  );
}

const POSITIONS = ['ALL', 'GK', 'DF', 'MF', 'FW'];

export default function PlayerIntelligence({ onComparePlayer }) {
  const [players, setPlayers] = useState([]);
  const [selectedPlayer, setSelectedPlayer] = useState(null);
  const [playerStats, setPlayerStats] = useState(null);
  const [playerShots, setPlayerShots] = useState([]);
  const [activeTab, setActiveTab] = useState('radar');
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [positionFilter, setPositionFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [statsLoading, setStatsLoading] = useState(false);
  const [listError, setListError] = useState(null);

  useEffect(() => {
    const t = setTimeout(() => setDebouncedQuery(searchQuery.trim()), 300);
    return () => clearTimeout(t);
  }, [searchQuery]);

  useEffect(() => {
    async function loadPlayers() {
      setLoading(true);
      setListError(null);
      try {
        const res = await api.getPlayers({ query: debouncedQuery, position: positionFilter });
        const list = res.data || [];
        setPlayers(list);
        if (list.length > 0) {
          const stillSelected = selectedPlayer && list.some((p) => p.player_id === selectedPlayer.player_id);
          if (!stillSelected) {
            await selectPlayer(list[0]);
          }
        } else {
          setSelectedPlayer(null);
          setPlayerStats(null);
          setPlayerShots([]);
        }
      } catch (err) {
        console.error(err);
        setListError('Không tải được danh sách cầu thủ.');
        setPlayers([]);
      } finally {
        setLoading(false);
      }
    }
    loadPlayers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedQuery, positionFilter]);

  const selectPlayer = async (player) => {
    setSelectedPlayer(player);
    setPlayerStats(null);
    setPlayerShots([]);
    setStatsLoading(true);
    try {
      const [statsRes, shotsRes] = await Promise.all([
        api.getPlayerStats(player.player_id).catch(() => ({ data: null })),
        api.getPlayerShots(player.player_id).catch(() => ({ data: [] })),
      ]);
      setPlayerStats(statsRes.data);
      setPlayerShots(shotsRes.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setStatsLoading(false);
    }
  };

  const TABS = [
    { id: 'radar', label: 'Radar' },
    { id: 'shotmap', label: `Bản đồ sút (${playerShots.length})` },
  ];

  return (
    <div>
      <div className="section-header">
        <div>
          <h1 className="section-title">Cầu thủ</h1>
          <p className="section-subtitle">
            {players.length > 0 ? `${players.length} cầu thủ` : 'Tìm theo tên hoặc đội'}
          </p>
        </div>

        <div className="filter-bar">
          <input
            type="search"
            placeholder="Tìm cầu thủ hoặc đội…"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="search-input"
          />
          {POSITIONS.map((pos) => (
            <button
              key={pos}
              className={`filter-btn ${positionFilter === pos ? 'active' : ''}`}
              onClick={() => setPositionFilter(pos)}
            >
              {pos === 'ALL' ? 'Tất cả' : pos}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <div className="empty-state">Đang tải danh sách cầu thủ…</div>
      ) : listError ? (
        <div className="empty-state" style={{ color: 'var(--crimson-danger)' }}>{listError}</div>
      ) : players.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-title">Không có cầu thủ</div>
          <div>
            API đang trống hoặc chưa đồng bộ đội hình. Chạy rolling sync với FOOTBALL_DATA_API_KEY
            (hoặc full backfill) rồi thử lại.
          </div>
        </div>
      ) : (
        <div className="player-layout">
          <div className="player-list-pane">
            {players.map((p) => {
              const isSelected = selectedPlayer?.player_id === p.player_id;
              return (
                <div
                  key={p.player_id}
                  className={`card player-card ${isSelected ? 'selected' : ''}`}
                  onClick={() => selectPlayer(p)}
                >
                  <div className="player-card-header">
                    <PlayerAvatar src={p.avatar_url} name={p.name} size={48} />
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div className="player-name-text" style={{ fontSize: '14px' }}>{p.name}</div>
                      <div className="player-meta-info">
                        {p.position ? <span className="position-tag">{p.position}</span> : null}
                        <span className="truncate">{p.team_name || '—'}</span>
                        {p.jersey_number > 0 && <span>#{p.jersey_number}</span>}
                      </div>
                    </div>
                  </div>
                  {p.nationality ? (
                    <div className="player-card-footer">{p.nationality}</div>
                  ) : null}
                </div>
              );
            })}
          </div>

          {selectedPlayer && (
            <div className="card player-detail-pane">
              <div className="player-detail-header">
                <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
                  <PlayerAvatar src={selectedPlayer.avatar_url} name={selectedPlayer.name} size={80} />
                  <div>
                    <h2 className="player-detail-name">{selectedPlayer.name}</h2>
                    <div className="player-meta-info" style={{ marginTop: 6 }}>
                      {selectedPlayer.position ? (
                        <span className="position-tag">{selectedPlayer.position}</span>
                      ) : null}
                      <span>{selectedPlayer.team_name || '—'}</span>
                      {selectedPlayer.jersey_number > 0 && <span>#{selectedPlayer.jersey_number}</span>}
                      {selectedPlayer.nationality && <span>{selectedPlayer.nationality}</span>}
                    </div>
                  </div>
                </div>

                <button
                  className="filter-btn active"
                  onClick={() => onComparePlayer(selectedPlayer.player_id)}
                >
                  So sánh
                </button>
              </div>

              {statsLoading ? (
                <div className="empty-state" style={{ padding: 40 }}>Đang tải thống kê…</div>
              ) : playerStats ? (
                <>
                  <div className="stats-grid">
                    <StatBox value={playerStats.goals} label="Bàn thắng" color="#10b981" />
                    <StatBox value={playerStats.assists} label="Kiến tạo" color="#38bdf8" />
                    <StatBox value={playerStats.matches_played} label="Trận" />
                    <StatBox
                      value={playerStats.xg != null ? Number(playerStats.xg).toFixed(2) : '—'}
                      label="xG"
                    />
                    <StatBox
                      value={playerStats.goals_per_90 != null ? Number(playerStats.goals_per_90).toFixed(2) : '—'}
                      label="Bàn/90'"
                      color="#f59e0b"
                    />
                    <StatBox value={playerStats.shots} label="Sút" />
                  </div>

                  <div className="tab-row">
                    {TABS.map((tab) => (
                      <button
                        key={tab.id}
                        className={`tab-btn ${activeTab === tab.id ? 'active' : ''}`}
                        onClick={() => setActiveTab(tab.id)}
                      >
                        {tab.label}
                      </button>
                    ))}
                  </div>

                  {activeTab === 'radar' ? (
                    <div style={{ display: 'flex', justifyContent: 'center' }}>
                      <RadarChart
                        stats1={playerStats}
                        name1={selectedPlayer.name}
                        color1="#10b981"
                        size={340}
                      />
                    </div>
                  ) : (
                    <PitchShotMap shots={playerShots} playerName={selectedPlayer.name} />
                  )}
                </>
              ) : (
                <div className="empty-state" style={{ padding: 48 }}>
                  <div className="empty-state-title">Chưa có thống kê</div>
                  <div>Cầu thủ này chưa có dữ liệu mùa giải trong kho phân tích.</div>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
