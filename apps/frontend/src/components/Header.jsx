import React from 'react';

export default function Header({ activeTab, setActiveTab }) {
  const tabs = [
    { id: 'matches', label: 'Trận Đấu', icon: '⚽' },
    { id: 'players', label: 'Cầu Thủ', icon: '👤' },
    { id: 'compare', label: 'So Sánh', icon: '⚔️' },
    { id: 'ai-analyst', label: 'AI Phân Tích', icon: '🧠' },
  ];

  return (
    <header className="header">
      <div className="header-inner">
        <div className="logo-group" onClick={() => setActiveTab('matches')} style={{ cursor: 'pointer' }}>
          <div className="logo-badge">⚽</div>
          <div>
            <span className="logo-title">PitchPulse</span>
            <span className="logo-sub">Football Analytics Platform</span>
          </div>
        </div>

        <nav className="nav-tabs">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              className={`nav-tab-btn ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              <span>{tab.icon}</span>
              <span>{tab.label}</span>
            </button>
          ))}
        </nav>

        <div style={{
          display: 'flex', alignItems: 'center', gap: '8px',
          padding: '6px 14px', borderRadius: '20px',
          background: 'rgba(99, 102, 241, 0.15)',
          border: '1px solid rgba(99, 102, 241, 0.3)',
          fontSize: '12px', color: '#a5b4fc', fontWeight: 600,
          whiteSpace: 'nowrap',
        }}>
          <span style={{ fontSize: '14px' }}>🌍</span>
          <span>StatsBomb Open Data • 2015–2024</span>
        </div>
      </div>
    </header>
  );
}
