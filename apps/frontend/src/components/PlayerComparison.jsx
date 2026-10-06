import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import RadarChart from './RadarChart';

export default function PlayerComparison({ initialPlayerId }) {
  const [players, setPlayers] = useState([]);
  const [player1Id, setPlayer1Id] = useState(initialPlayerId || null);
  const [player2Id, setPlayer2Id] = useState(null);
  const [comparisonData, setComparisonData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function loadPlayersList() {
      try {
        const res = await api.getPlayers();
        const list = res.data || [];
        setPlayers(list);
        if (list.length >= 2) {
          const p1 = initialPlayerId && list.some((p) => p.player_id === initialPlayerId)
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
        setError('Không so sánh được — thiếu thống kê cho một trong hai cầu thủ.');
        setComparisonData(null);
      } finally {
        setLoading(false);
      }
    }
    loadComparison();
  }, [player1Id, player2Id]);

  const p1 = comparisonData?.players?.[0];
  const p2 = comparisonData?.players?.[1];
  const s1 = comparisonData?.season_stats?.[0];
  const s2 = comparisonData?.season_stats?.[1];
  const metrics = comparisonData?.metric_comparisons || {};

  return (
    <div>
      <div className="section-header">
        <div>
          <h1 className="section-title">So sánh cầu thủ</h1>
          <p className="section-subtitle">Đối chiếu chỉ số thật theo mùa — không ước lượng</p>
        </div>

        <div className="compare-selectors">
          <label>
            Cầu thủ 1
            <select
              value={player1Id || ''}
              onChange={(e) => setPlayer1Id(Number(e.target.value))}
              className="compare-select compare-select-a"
            >
              {players.map((p) => (
                <option key={p.player_id} value={p.player_id}>
                  {p.name}{p.team_name ? ` · ${p.team_name}` : ''}
                </option>
              ))}
            </select>
          </label>

          <span className="compare-vs">VS</span>

          <label>
            Cầu thủ 2
            <select
              value={player2Id || ''}
              onChange={(e) => setPlayer2Id(Number(e.target.value))}
              className="compare-select compare-select-b"
            >
              {players.map((p) => (
                <option key={p.player_id} value={p.player_id}>
                  {p.name}{p.team_name ? ` · ${p.team_name}` : ''}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      {players.length < 2 ? (
        <div className="empty-state">
          Cần ít nhất 2 cầu thủ trong hệ thống để so sánh. Đồng bộ dữ liệu rồi quay lại.
        </div>
      ) : loading ? (
        <div className="empty-state">Đang so sánh…</div>
      ) : error ? (
        <div className="empty-state" style={{ color: 'var(--crimson-danger)' }}>{error}</div>
      ) : !comparisonData ? (
        <div className="empty-state">Chọn hai cầu thủ khác nhau để bắt đầu.</div>
      ) : (
        <div className="comparison-layout">
          <div className="card" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <h3 className="card-heading">Radar đối đầu</h3>
            <p className="card-sub">
              Xanh lá: {p1?.name} · Xanh dương: {p2?.name}
            </p>

            <RadarChart
              stats1={s1}
              name1={p1?.name}
              color1="#10b981"
              stats2={s2}
              name2={p2?.name}
              color2="#06b6d4"
              size={340}
            />

            <div className="compare-summary-grid">
              <div className="compare-summary a">
                <div className="compare-summary-name">{p1?.name}</div>
                <div className="compare-summary-meta">{p1?.team_name} · {p1?.position || '—'}</div>
                <div className="compare-summary-stat">
                  {s1?.goals ?? '—'} bàn ({s1?.goals_per_90 ?? '—'}/90)
                </div>
              </div>
              <div className="compare-summary b">
                <div className="compare-summary-name">{p2?.name}</div>
                <div className="compare-summary-meta">{p2?.team_name} · {p2?.position || '—'}</div>
                <div className="compare-summary-stat">
                  {s2?.goals ?? '—'} bàn ({s2?.goals_per_90 ?? '—'}/90)
                </div>
              </div>
            </div>
          </div>

          <div className="card">
            <h3 className="card-heading">Bảng chỉ số</h3>
            <table className="comparison-table">
              <thead>
                <tr>
                  <th>Chỉ số</th>
                  <th style={{ color: '#10b981' }}>{p1?.name}</th>
                  <th style={{ color: '#06b6d4' }}>{p2?.name}</th>
                  <th>Chênh lệch</th>
                </tr>
              </thead>
              <tbody>
                {Object.values(metrics).map((m) => {
                  const isP1Leader = m.leader === p1?.name;
                  const isP2Leader = m.leader === p2?.name;
                  return (
                    <tr key={m.metric}>
                      <td style={{ fontWeight: 600, color: '#e2e8f0' }}>{m.metric}</td>
                      <td style={{ color: isP1Leader ? '#10b981' : 'var(--text-secondary)', fontWeight: isP1Leader ? 700 : 400 }}>
                        {m.player1Value}
                      </td>
                      <td style={{ color: isP2Leader ? '#06b6d4' : 'var(--text-secondary)', fontWeight: isP2Leader ? 700 : 400 }}>
                        {m.player2Value}
                      </td>
                      <td className="leader-cell" style={{ color: isP1Leader ? '#34d399' : (isP2Leader ? '#38bdf8' : 'var(--text-muted)') }}>
                        {m.leader === 'Tied' ? 'Hòa' : `${m.leader} (+${m.difference})`}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
