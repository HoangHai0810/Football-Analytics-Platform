import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import RadarChart from './RadarChart';

function PlayerAvatarFallback({ name, color = '#10b981', size = 64 }) {
  const initials = (name || '?')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0])
    .join('')
    .toUpperCase();

  return (
    <div
      style={{
        width: size,
        height: size,
        borderRadius: '50%',
        background: `radial-gradient(circle, ${color}22 0%, rgba(15,23,42,0.9) 100%)`,
        border: `2px solid ${color}`,
        color: color,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: size * 0.36,
        fontWeight: 800,
        boxShadow: `0 0 16px ${color}33`,
        flexShrink: 0,
      }}
    >
      {initials || '?'}
    </div>
  );
}

function MetricBattleRow({ label, val1, val2, p1Name, p2Name, isHigherBetter = true }) {
  const num1 = Number(val1) || 0;
  const num2 = Number(val2) || 0;
  const total = num1 + num2;

  let pct1 = 50;
  let pct2 = 50;
  if (total > 0) {
    pct1 = Math.round((num1 / total) * 100);
    pct2 = 100 - pct1;
  }

  const p1Wins = isHigherBetter ? num1 > num2 : num1 < num2;
  const p2Wins = isHigherBetter ? num2 > num1 : num2 < num1;
  const isTied = num1 === num2;

  return (
    <div className="battle-row">
      <div className="battle-row-top">
        <div className={`battle-val p1 ${p1Wins ? 'winner' : ''}`}>
          {p1Wins && <span className="winner-crown">👑</span>}
          <span>{val1 ?? '—'}</span>
        </div>
        <div className="battle-label">{label}</div>
        <div className={`battle-val p2 ${p2Wins ? 'winner' : ''}`}>
          <span>{val2 ?? '—'}</span>
          {p2Wins && <span className="winner-crown">👑</span>}
        </div>
      </div>

      <div className="battle-meter-track">
        <div
          className={`battle-meter-fill p1 ${p1Wins ? 'winner-glow' : ''}`}
          style={{ width: `${pct1}%` }}
          title={`${p1Name}: ${val1} (${pct1}%)`}
        />
        <div className="battle-meter-divider" />
        <div
          className={`battle-meter-fill p2 ${p2Wins ? 'winner-glow' : ''}`}
          style={{ width: `${pct2}%` }}
          title={`${p2Name}: ${val2} (${pct2}%)`}
        />
      </div>
    </div>
  );
}

export default function PlayerComparison({ initialPlayerId, onAskAi }) {
  const [players, setPlayers] = useState([]);
  const [player1Id, setPlayer1Id] = useState(initialPlayerId || null);
  const [player2Id, setPlayer2Id] = useState(null);
  const [comparisonData, setComparisonData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('battle'); // 'battle' | 'radar' | 'table'

  useEffect(() => {
    async function loadPlayersList() {
      try {
        const res = await api.getPlayers();
        const list = res.data || [];
        setPlayers(list);
        if (list.length >= 2) {
          const p1 =
            initialPlayerId && list.some((p) => p.player_id === initialPlayerId)
              ? initialPlayerId
              : list[0].player_id;
          const p2 = list.find((p) => p.player_id !== p1)?.player_id || list[1].player_id;
          setPlayer1Id(p1);
          setPlayer2Id(p2);
        } else if (list.length === 1) {
          setPlayer1Id(list[0].player_id);
        }
      } catch (err) {
        console.error(err);
        setError('Không tải được danh sách cầu thủ để so sánh.');
      }
    }
    loadPlayersList();
  }, [initialPlayerId]);

  useEffect(() => {
    async function loadComparison() {
      if (!player1Id || !player2Id || player1Id === player2Id) {
        setComparisonData(null);
        return;
      }
      setLoading(true);
      setError(null);
      try {
        const res = await api.comparePlayers(player1Id, player2Id);
        setComparisonData(res.data);
      } catch (err) {
        console.error(err);
        setError('Không so sánh được — thiếu dữ liệu thống kê của cầu thủ.');
        setComparisonData(null);
      } finally {
        setLoading(false);
      }
    }
    loadComparison();
  }, [player1Id, player2Id]);

  const handleSwap = () => {
    const temp = player1Id;
    setPlayer1Id(player2Id);
    setPlayer2Id(temp);
  };

  const p1 = comparisonData?.players?.[0];
  const p2 = comparisonData?.players?.[1];
  const s1 = comparisonData?.season_stats?.[0];
  const s2 = comparisonData?.season_stats?.[1];
  const metrics = comparisonData?.metric_comparisons || {};

  // Tally wins
  let p1WinsCount = 0;
  let p2WinsCount = 0;
  Object.values(metrics).forEach((m) => {
    if (m.leader === p1?.name) p1WinsCount++;
    else if (m.leader === p2?.name) p2WinsCount++;
  });

  const handleTriggerAiComparison = () => {
    if (onAskAi && p1 && p2) {
      onAskAi(`So sánh chi tiết phong độ và chỉ số chiến thuật giữa ${p1.name} và ${p2.name} mùa giải 2026/27`);
    }
  };

  return (
    <div className="comparison-arena-page">
      <div className="section-header">
        <div>
          <h1 className="section-title">
            <span>⚔️</span> Đối Đầu & So Sánh Cầu Thủ
          </h1>
          <p className="section-subtitle">
            Phân tích số liệu thực tế theo mùa giải (xG, xA, Bàn thắng, Tần suất dứt điểm/90')
          </p>
        </div>

        {p1 && p2 && onAskAi && (
          <button onClick={handleTriggerAiComparison} className="ai-compare-btn">
            <span>⚡</span> Hỏi AI Đánh Giá So Sánh
          </button>
        )}
      </div>

      {/* Dual Player Selection Cards Arena */}
      <div className="vs-arena-bar">
        {/* Player 1 Card */}
        <div className="vs-player-card p1-card">
          <div className="vs-avatar-wrapper">
            <PlayerAvatarFallback name={p1?.name} color="#10b981" size={72} />
            <span className="vs-role-badge p1-badge">Cầu thủ 1</span>
          </div>

          <div className="vs-info-col">
            <div className="vs-player-name">{p1?.name || 'Đang chọn…'}</div>
            <div className="vs-player-sub">
              {p1?.team_name || 'Đội bóng'} {p1?.position ? `· ${p1.position}` : ''}
            </div>

            <div className="vs-selector-row">
              <label className="vs-label">Đổi cầu thủ:</label>
              <select
                value={player1Id || ''}
                onChange={(e) => setPlayer1Id(Number(e.target.value))}
                className="vs-select vs-select-p1"
              >
                {players.map((p) => (
                  <option key={p.player_id} value={p.player_id}>
                    {p.name} {p.team_name ? `(${p.team_name})` : ''}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        {/* Center VS Crest & Swap */}
        <div className="vs-center-node">
          <div className="vs-glow-badge">VS</div>
          <button
            onClick={handleSwap}
            className="vs-swap-btn"
            title="Đổi vị trí 2 cầu thủ"
            disabled={loading || !player1Id || !player2Id}
          >
            ⇄ Đổi bên
          </button>
        </div>

        {/* Player 2 Card */}
        <div className="vs-player-card p2-card">
          <div className="vs-avatar-wrapper">
            <PlayerAvatarFallback name={p2?.name} color="#06b6d4" size={72} />
            <span className="vs-role-badge p2-badge">Cầu thủ 2</span>
          </div>

          <div className="vs-info-col">
            <div className="vs-player-name">{p2?.name || 'Đang chọn…'}</div>
            <div className="vs-player-sub">
              {p2?.team_name || 'Đội bóng'} {p2?.position ? `· ${p2.position}` : ''}
            </div>

            <div className="vs-selector-row">
              <label className="vs-label">Đổi cầu thủ:</label>
              <select
                value={player2Id || ''}
                onChange={(e) => setPlayer2Id(Number(e.target.value))}
                className="vs-select vs-select-p2"
              >
                {players.map((p) => (
                  <option key={p.player_id} value={p.player_id}>
                    {p.name} {p.team_name ? `(${p.team_name})` : ''}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>
      </div>

      {/* Main Comparison Area */}
      {players.length < 2 ? (
        <div className="empty-state">
          <div className="empty-state-title">Chưa đủ dữ liệu so sánh</div>
          <p>Cần ít nhất 2 cầu thủ trong hệ thống để đối đầu trực tiếp.</p>
        </div>
      ) : loading ? (
        <div className="empty-state">
          <span className="pulse-dot" />
          <p>Đang tải và tính toán đối đầu chỉ số…</p>
        </div>
      ) : error ? (
        <div className="empty-state" style={{ color: 'var(--crimson-danger)' }}>
          {error}
        </div>
      ) : !comparisonData ? (
        <div className="empty-state">Chọn 2 cầu thủ để bắt đầu so sánh.</div>
      ) : (
        <div className="comparison-content-wrapper">
          {/* Winner Score Banner */}
          <div className="battle-summary-strip">
            <div className="summary-col p1-win-col">
              <div className="win-score-badge p1">{p1WinsCount}</div>
              <div className="win-score-label">Chỉ số dẫn trước của {p1?.name}</div>
            </div>
            <div className="summary-vs-divider">
              <span>TỔNG ĐỐI ĐẦU</span>
            </div>
            <div className="summary-col p2-win-col">
              <div className="win-score-badge p2">{p2WinsCount}</div>
              <div className="win-score-label">Chỉ số dẫn trước của {p2?.name}</div>
            </div>
          </div>

          {/* Tab Switcher */}
          <div className="comparison-tabs-bar">
            <button
              className={`comp-tab-btn ${activeTab === 'battle' ? 'active' : ''}`}
              onClick={() => setActiveTab('battle')}
            >
              📊 Thước Đo Đối Đầu (Battle Meters)
            </button>
            <button
              className={`comp-tab-btn ${activeTab === 'radar' ? 'active' : ''}`}
              onClick={() => setActiveTab('radar')}
            >
              🕸️ Radar Tổng Thể
            </button>
            <button
              className={`comp-tab-btn ${activeTab === 'table' ? 'active' : ''}`}
              onClick={() => setActiveTab('table')}
            >
              📋 Bảng Số Liệu Chi Tiết
            </button>
          </div>

          {/* TAB 1: BATTLE METERS */}
          {activeTab === 'battle' && (
            <div className="card battle-meters-card">
              <div className="battle-card-header">
                <div className="legend-item p1-legend">
                  <span className="legend-dot p1-dot" />
                  <span>{p1?.name} ({p1?.team_name})</span>
                </div>
                <div className="legend-center">ĐỐI ĐẦU TỪNG CHỈ SỐ</div>
                <div className="legend-item p2-legend">
                  <span className="legend-dot p2-dot" />
                  <span>{p2?.name} ({p2?.team_name})</span>
                </div>
              </div>

              <div className="battle-rows-list">
                <MetricBattleRow
                  label="Bàn thắng ghi được"
                  val1={s1?.goals}
                  val2={s2?.goals}
                  p1Name={p1?.name}
                  p2Name={p2?.name}
                />
                <MetricBattleRow
                  label="Kiến tạo thành bàn"
                  val1={s1?.assists}
                  val2={s2?.assists}
                  p1Name={p1?.name}
                  p2Name={p2?.name}
                />
                <MetricBattleRow
                  label="Bàn thắng kỳ vọng (xG)"
                  val1={s1?.xg}
                  val2={s2?.xg}
                  p1Name={p1?.name}
                  p2Name={p2?.name}
                />
                <MetricBattleRow
                  label="Kiến tạo kỳ vọng (xA)"
                  val1={s1?.xa}
                  val2={s2?.xa}
                  p1Name={p1?.name}
                  p2Name={p2?.name}
                />
                <MetricBattleRow
                  label="Bàn thắng / 90 phút"
                  val1={s1?.goals_per_90}
                  val2={s2?.goals_per_90}
                  p1Name={p1?.name}
                  p2Name={p2?.name}
                />
                <MetricBattleRow
                  label="Kiến tạo / 90 phút"
                  val1={s1?.assists_per_90}
                  val2={s2?.assists_per_90}
                  p1Name={p1?.name}
                  p2Name={p2?.name}
                />
                <MetricBattleRow
                  label="Số phút thi đấu thực tế"
                  val1={s1?.minutes}
                  val2={s2?.minutes}
                  p1Name={p1?.name}
                  p2Name={p2?.name}
                />
                <MetricBattleRow
                  label="Số trận ra sân"
                  val1={s1?.matches_played}
                  val2={s2?.matches_played}
                  p1Name={p1?.name}
                  p2Name={p2?.name}
                />
              </div>
            </div>
          )}

          {/* TAB 2: RADAR CHART */}
          {activeTab === 'radar' && (
            <div className="card radar-comparison-card">
              <div className="radar-head">
                <h3 className="card-heading">Radar Đối Chiếu Kỹ Năng & Phong Độ</h3>
                <p className="card-sub">
                  Xanh ngọc: <strong>{p1?.name}</strong> vs Xanh cyan: <strong>{p2?.name}</strong>
                </p>
              </div>

              <div className="radar-view-box">
                <RadarChart
                  stats1={s1}
                  name1={p1?.name}
                  color1="#10b981"
                  stats2={s2}
                  name2={p2?.name}
                  color2="#06b6d4"
                  size={380}
                />
              </div>
            </div>
          )}

          {/* TAB 3: TABLE */}
          {activeTab === 'table' && (
            <div className="card table-card">
              <h3 className="card-heading">Bảng Thống Kê Đối Chiếu</h3>
              <table className="comparison-table">
                <thead>
                  <tr>
                    <th>Chỉ số</th>
                    <th style={{ color: '#10b981' }}>{p1?.name}</th>
                    <th style={{ color: '#06b6d4' }}>{p2?.name}</th>
                    <th>Bên vượt trội</th>
                  </tr>
                </thead>
                <tbody>
                  {Object.values(metrics).map((m) => {
                    const isP1Leader = m.leader === p1?.name;
                    const isP2Leader = m.leader === p2?.name;
                    return (
                      <tr key={m.metric}>
                        <td style={{ fontWeight: 600, color: '#e2e8f0' }}>{m.metric}</td>
                        <td
                          style={{
                            color: isP1Leader ? '#10b981' : 'var(--text-secondary)',
                            fontWeight: isP1Leader ? 700 : 400,
                          }}
                        >
                          {m.player1Value}
                        </td>
                        <td
                          style={{
                            color: isP2Leader ? '#06b6d4' : 'var(--text-secondary)',
                            fontWeight: isP2Leader ? 700 : 400,
                          }}
                        >
                          {m.player2Value}
                        </td>
                        <td
                          className="leader-cell"
                          style={{
                            color: isP1Leader ? '#34d399' : isP2Leader ? '#38bdf8' : 'var(--text-muted)',
                          }}
                        >
                          {m.leader === 'Tied'
                            ? 'Ngang bằng'
                            : `${m.leader} (+${m.difference})`}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
