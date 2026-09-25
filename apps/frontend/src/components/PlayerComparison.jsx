import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import RadarChart from './RadarChart';

export default function PlayerComparison({ initialPlayerId }) {
  const [players, setPlayers] = useState([]);
  const [player1Id, setPlayer1Id] = useState(initialPlayerId || 1024);
  const [player2Id, setPlayer2Id] = useState(1088);
  const [comparisonData, setComparisonData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadPlayersList() {
      try {
        const res = await api.getPlayers();
        setPlayers(res.data || []);
      } catch (err) {
        console.error(err);
      }
    }
    loadPlayersList();
  }, []);

  useEffect(() => {
    async function loadComparison() {
      if (!player1Id || !player2Id) return;
      setLoading(true);
      try {
        const res = await api.comparePlayers(player1Id, player2Id);
        setComparisonData(res.data);
      } catch (err) {
        console.error(err);
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
          <h1 className="section-title">
            <span>⚔️</span> Head-to-Head Player Comparison
          </h1>
          <p className="section-subtitle">
            Direct analytical benchmarking across per-90 metrics, dual radar polygons, and delta gap calculations
          </p>
        </div>

        {/* Player Selectors */}
        <div style={{ display: 'flex', gap: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '13px', color: '#10b981', fontWeight: 600 }}>Player 1:</span>
            <select
              value={player1Id}
              onChange={(e) => setPlayer1Id(Number(e.target.value))}
              style={{
                background: 'rgba(15, 23, 42, 0.9)',
                border: '1px solid rgba(16, 185, 129, 0.4)',
                color: '#fff',
                padding: '6px 12px',
                borderRadius: 'var(--radius-md)',
                outline: 'none',
                fontFamily: 'var(--font-body)'
              }}
            >
              {players.map((p) => (
                <option key={p.player_id} value={p.player_id}>
                  {p.name} ({p.team_name})
                </option>
              ))}
            </select>
          </div>

          <span style={{ color: 'var(--text-muted)', fontWeight: 800 }}>VS</span>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '13px', color: '#06b6d4', fontWeight: 600 }}>Player 2:</span>
            <select
              value={player2Id}
              onChange={(e) => setPlayer2Id(Number(e.target.value))}
              style={{
                background: 'rgba(15, 23, 42, 0.9)',
                border: '1px solid rgba(6, 182, 212, 0.4)',
                color: '#fff',
                padding: '6px 12px',
                borderRadius: 'var(--radius-md)',
                outline: 'none',
                fontFamily: 'var(--font-body)'
              }}
            >
              {players.map((p) => (
                <option key={p.player_id} value={p.player_id}>
                  {p.name} ({p.team_name})
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {loading || !comparisonData ? (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-secondary)' }}>
          Computing comparative analytical models...
        </div>
      ) : (
        <div className="comparison-layout">
          {/* Left Column: Overlaid Dual Radar Chart */}
          <div className="card" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <h3 style={{ fontSize: '18px', marginBottom: '8px', color: '#fff' }}>
              Multi-Axis Radar Comparison
            </h3>
            <p style={{ fontSize: '12px', color: 'var(--text-muted)', textAlign: 'center', marginBottom: '16px' }}>
              Green: {p1?.name} &nbsp;|&nbsp; Cyan: {p2?.name}
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

            <div style={{ marginTop: '20px', width: '100%', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div style={{ background: 'rgba(16, 185, 129, 0.1)', padding: '12px', borderRadius: 'var(--radius-md)', border: '1px solid rgba(16, 185, 129, 0.3)' }}>
                <div style={{ fontWeight: 700, color: '#34d399' }}>{p1?.name}</div>
                <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>{p1?.team_name} • {p1?.position}</div>
                <div style={{ marginTop: '6px', fontSize: '13px', fontWeight: 600 }}>
                  {s1?.goals} Goals ({s1?.goals_per_90}/90)
                </div>
              </div>

              <div style={{ background: 'rgba(6, 182, 212, 0.1)', padding: '12px', borderRadius: 'var(--radius-md)', border: '1px solid rgba(6, 182, 212, 0.3)' }}>
                <div style={{ fontWeight: 700, color: '#38bdf8' }}>{p2?.name}</div>
                <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>{p2?.team_name} • {p2?.position}</div>
                <div style={{ marginTop: '6px', fontSize: '13px', fontWeight: 600 }}>
                  {s2?.goals} Goals ({s2?.goals_per_90}/90)
                </div>
              </div>
            </div>
          </div>

          {/* Right Column: Comparative Metric Delta Table */}
          <div className="card">
            <h3 style={{ fontSize: '18px', marginBottom: '14px', color: '#fff' }}>
              Head-to-Head Metric Breakdown
            </h3>

            <table className="comparison-table">
              <thead>
                <tr>
                  <th>Metric</th>
                  <th style={{ color: '#10b981' }}>{p1?.name}</th>
                  <th style={{ color: '#06b6d4' }}>{p2?.name}</th>
                  <th>Leader / Delta</th>
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
                        {m.leader === 'Tied' ? 'Tied' : `${m.leader} (+${m.difference})`}
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
