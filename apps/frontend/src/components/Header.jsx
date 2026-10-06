import React, { useState, useEffect } from 'react';
import { api } from '../services/api';

export default function Header({ activeTab, setActiveTab }) {
  const [dbStats, setDbStats] = useState(null);

  useEffect(() => {
    api.getSystemStatus()
      .then((r) => setDbStats(r.data))
      .catch(() => {});
  }, []);

  const tabs = [
    { id: 'matches', label: 'Trận Đấu' },
    { id: 'players', label: 'Cầu Thủ' },
    { id: 'compare', label: 'So Sánh' },
    { id: 'ai-analyst', label: 'AI Phân Tích' },
  ];

  const counts = dbStats?.entity_counts || {};
  const matchCount = Number(counts.matches || 0);
  const playerCount = Number(counts.players || 0);
  const healthy = dbStats?.status === 'HEALTHY';

  return (
    <header className="header">
      <div className="header-inner">
        <div className="logo-group" onClick={() => setActiveTab('matches')} style={{ cursor: 'pointer' }}>
          <div className="logo-badge">PP</div>
          <div>
            <span className="logo-title">PitchPulse</span>
            <span className="logo-sub">Football Analytics</span>
          </div>
        </div>

        <nav className="nav-tabs">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              className={`nav-tab-btn ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </nav>

        {dbStats && (
          <div className={`header-status-pill ${healthy ? '' : 'degraded'}`}>
            <span className="pulse-dot" />
            <span>
              {matchCount.toLocaleString('vi-VN')} trận · {playerCount.toLocaleString('vi-VN')} cầu thủ
            </span>
          </div>
        )}
      </div>
    </header>
  );
}
