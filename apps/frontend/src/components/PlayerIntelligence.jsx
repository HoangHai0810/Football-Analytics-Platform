import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import RadarChart from './RadarChart';
import PitchShotMap from './PitchShotMap';

function PlayerAvatar({ src, name, size = 52 }) {
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
          borderRadius: size > 64 ? '50%' : 12,
          fontSize: size * 0.35,
          fontWeight: 800,
          background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.2), rgba(6, 182, 212, 0.2))',
          border: '1px solid rgba(16, 185, 129, 0.4)',
          color: '#34d399',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
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
      style={{
        width: size,
        height: size,
        borderRadius: size > 64 ? '50%' : 12,
        objectFit: 'cover',
        border: size > 64 ? '3px solid #10b981' : '1px solid rgba(255,255,255,0.1)',
        background: 'rgba(15,23,42,0.8)',
        flexShrink: 0,
      }}
      onError={() => setErr(true)}
    />
  );
}

function StatTile({ value, label, color = '#ffffff', sub = null }) {
  return (
    <div className="stat-tile">
      <div className="stat-tile-value" style={{ color }}>
        {value ?? '—'}
      </div>
      <div className="stat-tile-label">{label}</div>
      {sub && <div className="stat-tile-sub">{sub}</div>}
    </div>
  );
}

const POSITIONS = [
  { id: 'ALL', label: 'Tất cả' },
  { id: 'FW', label: 'Tiền đạo (FW)' },
  { id: 'MF', label: 'Tiền vệ (MF)' },
  { id: 'DF', label: 'Hậu vệ (DF)' },
  { id: 'GK', label: 'Thủ môn (GK)' },
];

export default function PlayerIntelligence({ onComparePlayer, onAskAi }) {
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
        setListError('Không tải được danh sách cầu thủ. Vui lòng kiểm tra kết nối API.');
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

  const handleAskAiAboutPlayer = () => {
    if (onAskAi && selectedPlayer) {
      const query = `Phân tích chuyên sâu phong độ, kỹ năng và hiệu suất thi đấu của cầu thủ ${selectedPlayer.name} (${selectedPlayer.team_name || 'Đội hình chính'})`;
      onAskAi(query, null, selectedPlayer.player_id);
    }
  };

  return (
    <div className="player-intelligence-container">
      {/* Top Header & Search Bar */}
      <div className="section-header">
        <div>
          <h1 className="section-title">
            <span>👤</span> Trung Tâm Dữ Liệu Cầu Thủ
          </h1>
          <p className="section-subtitle">
            Hồ sơ năng lực, bản đồ nhiệt dứt điểm và phân tích radar đa trục
          </p>
        </div>

        {/* Filter & Search Bar */}
        <div className="filter-bar" style={{ flexWrap: 'wrap' }}>
          <input
            type="search"
            placeholder="Tìm theo tên cầu thủ hoặc đội…"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="search-input"
            style={{ width: '260px' }}
          />

          <div className="position-pills">
            {POSITIONS.map((p) => (
              <button
                key={p.id}
                className={`filter-btn ${positionFilter === p.id ? 'active' : ''}`}
                onClick={() => setPositionFilter(p.id)}
              >
                {p.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {loading ? (
        <div className="empty-state">Đang tải cơ sở dữ liệu cầu thủ…</div>
      ) : listError ? (
        <div className="empty-state" style={{ color: 'var(--crimson-danger)' }}>{listError}</div>
      ) : players.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-title">Không tìm thấy cầu thủ</div>
          <div style={{ marginTop: '8px', color: 'var(--text-muted)' }}>
            Thử nhập từ khóa khác hoặc đặt lại bộ lọc vị trí về "Tất cả".
          </div>
        </div>
      ) : (
        <div className="player-layout-grid">
          {/* Left: Player Roster Feed */}
          <div className="player-roster-pane">
            <div className="roster-header">
              <span>Danh sách ({players.length})</span>
            </div>
            <div className="roster-list-scroll">
              {players.map((p) => {
                const isSelected = selectedPlayer?.player_id === p.player_id;
                return (
                  <div
                    key={p.player_id}
                    className={`player-roster-card ${isSelected ? 'active' : ''}`}
                    onClick={() => selectPlayer(p)}
                  >
                    <PlayerAvatar src={p.avatar_url} name={p.name} size={42} />
                    <div className="roster-card-info">
                      <div className="roster-player-name">{p.name}</div>
                      <div className="roster-player-meta">
                        {p.position && <span className="position-badge">{p.position}</span>}
                        <span className="truncate">{p.team_name || 'Đội tuyển'}</span>
                        {p.jersey_number > 0 && <span>#{p.jersey_number}</span>}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Right: Player Showcase Profile */}
          {selectedPlayer && (
            <div className="player-showcase-pane">
              {/* Hero Profile Banner */}
              <div className="player-hero-banner">
                <div className="hero-avatar-group">
                  <PlayerAvatar src={selectedPlayer.avatar_url} name={selectedPlayer.name} size={84} />
                  <div className="hero-text-group">
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                      <h2 className="hero-player-name">{selectedPlayer.name}</h2>
                      {selectedPlayer.jersey_number > 0 && (
                        <span className="jersey-pill">#{selectedPlayer.jersey_number}</span>
                      )}
                    </div>

                    <div className="hero-sub-meta">
                      {selectedPlayer.position && (
                        <span className="position-tag-accent">{selectedPlayer.position}</span>
                      )}
                      <span className="hero-team-text">{selectedPlayer.team_name || 'Biên chế câu lạc bộ'}</span>
                      {selectedPlayer.nationality && (
                        <span>🌍 {selectedPlayer.nationality}</span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Quick Action Buttons */}
                <div className="hero-action-btns">
                  <button className="ask-ai-match-btn" onClick={handleAskAiAboutPlayer}>
                    <span>⚡</span>
                    <span>Hỏi AI Phân Tích</span>
                  </button>
                  <button
                    className="filter-btn active"
                    onClick={() => onComparePlayer(selectedPlayer.player_id)}
                    style={{ padding: '10px 18px', fontWeight: 700 }}
                  >
                    ⚔️ So Sánh
                  </button>
                </div>
              </div>

              {/* Stats & Visual Analytics */}
              {statsLoading ? (
                <div className="empty-state" style={{ padding: 48 }}>Đang truy xuất thông số mùa giải…</div>
              ) : playerStats ? (
                <>
                  {/* Primary Performance Metric Tiles */}
                  <div className="stat-tiles-grid">
                    <StatTile value={playerStats.goals} label="Bàn Thắng" color="#10b981" sub={`xG: ${(playerStats.xg || 0).toFixed(2)}`} />
                    <StatTile value={playerStats.assists} label="Kiến Tạo" color="#38bdf8" sub={`xA: ${(playerStats.xa || 0).toFixed(2)}`} />
                    <StatTile value={playerStats.matches_played} label="Trận Đấu" color="#f8fafc" sub={`${playerStats.minutes || 0} phút`} />
                    <StatTile value={playerStats.goals_per_90 != null ? Number(playerStats.goals_per_90).toFixed(2) : '—'} label="Bàn / 90'" color="#fbbf24" />
                    <StatTile value={playerStats.assists_per_90 != null ? Number(playerStats.assists_per_90).toFixed(2) : '—'} label="Kiến Tạo / 90'" color="#a78bfa" />
                    <StatTile value={playerStats.shots || 0} label="Tổng Cú Sút" color="#f8fafc" sub={`Trúng đích: ${playerStats.shots_on_target || 0}`} />
                  </div>

                  {/* Visual Analysis View Switcher */}
                  <div className="tab-row" style={{ marginTop: '20px' }}>
                    <button
                      className={`tab-btn ${activeTab === 'radar' ? 'active' : ''}`}
                      onClick={() => setActiveTab('radar')}
                    >
                      📊 Biểu Đồ Radar 6 Trục Kỹ Năng
                    </button>
                    <button
                      className={`tab-btn ${activeTab === 'shotmap' ? 'active' : ''}`}
                      onClick={() => setActiveTab('shotmap')}
                    >
                      🎯 Bản Đồ Dứt Điểm Shot Map ({playerShots.length})
                    </button>
                  </div>

                  {/* Visual Display */}
                  <div className="visual-analytics-card">
                    {activeTab === 'radar' ? (
                      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '14px 0' }}>
                        <RadarChart
                          stats1={playerStats}
                          name1={selectedPlayer.name}
                          color1="#10b981"
                          size={360}
                        />
                        <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '8px' }}>
                          Trục điểm chuẩn hóa theo chuẩn vị trí từ dữ liệu thực tế (thang điểm 100)
                        </div>
                      </div>
                    ) : (
                      <PitchShotMap shots={playerShots} playerName={selectedPlayer.name} />
                    )}
                  </div>
                </>
              ) : (
                <div className="empty-state" style={{ padding: 48, background: 'rgba(0,0,0,0.2)', borderRadius: 'var(--radius-lg)' }}>
                  <div className="empty-state-title">Hồ Sơ Cầu Thủ Đã Ghi Nhận</div>
                  <div style={{ marginTop: '8px', color: 'var(--text-secondary)', maxWidth: '500px', margin: '8px auto 0 auto' }}>
                    Cầu thủ <strong>{selectedPlayer.name}</strong> đã có tên trong danh sách thi đấu của {selectedPlayer.team_name || 'câu lạc bộ'}.
                    Thống kê sự kiện chi tiết mùa giải đang được hoàn thiện khi trận đấu diễn ra.
                  </div>
                  <div style={{ marginTop: '16px' }}>
                    <button className="ask-ai-match-btn" onClick={handleAskAiAboutPlayer} style={{ margin: '0 auto' }}>
                      <span>⚡</span>
                      <span>Hỏi AI Nhận Định Về Cầu Thủ Này</span>
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
