import React from 'react';

export default function Header({ activeTab, setActiveTab, onOpenStatusModal, isLiveBackend }) {
  const tabs = [
    { id: 'matches', label: 'Match Center', icon: '⚽' },
    { id: 'players', label: 'Player Intelligence', icon: '👤' },
    { id: 'compare', label: 'H2H Comparison', icon: '⚔️' },
    { id: 'ai-analyst', label: 'AI Football Analyst', icon: '🧠' }
  ];

  return (
    <header className="header">
      <div className="header-inner">
        <div className="logo-group" onClick={() => setActiveTab('matches')}>
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

        <div className="header-status-pill" onClick={onOpenStatusModal}>
          <div className="pulse-dot"></div>
          <span>{isLiveBackend ? 'Spring Boot 3: Live' : 'BE: Standby (Active Store)'}</span>
        </div>
      </div>
    </header>
  );
}
