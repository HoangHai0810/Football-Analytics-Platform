import React from 'react';

export default function Header({ activeTab, setActiveTab }) {
  const tabs = [
    { id: 'matches', label: 'Lịch & Trận Đấu', icon: '⚽' },
    { id: 'players', label: 'Cầu Thủ', icon: '👤' },
    { id: 'compare', label: 'So Sánh', icon: '⚔️' },
    { id: 'ai-analyst', label: 'AI Phân Tích', icon: '⚡' },
  ];

  return (
    <header className="header">
      <div className="header-inner">
        <div className="logo-group" onClick={() => setActiveTab('matches')} style={{ cursor: 'pointer' }}>
          <div className="logo-badge">
            <span>⚽</span>
          </div>
          <div>
            <span className="logo-title">PitchPulse</span>
            <span className="logo-sub">Trung Tâm Phân Tích Bóng Đá</span>
          </div>
        </div>

        <nav className="nav-tabs">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              className={`nav-tab-btn ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              <span style={{ fontSize: '14px' }}>{tab.icon}</span>
              <span>{tab.label}</span>
            </button>
          ))}
        </nav>

        <div className="header-right-actions">
          <div className="header-season-badge">
            <span className="header-live-indicator">LIVE</span>
            <span>Mùa Giải 2024/26</span>
          </div>
        </div>
      </div>
    </header>
  );
}
