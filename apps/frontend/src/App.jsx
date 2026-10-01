import React, { useState, useEffect } from 'react';
import { api } from './services/api';
import Header from './components/Header';
import MatchCenter from './components/MatchCenter';
import PlayerIntelligence from './components/PlayerIntelligence';
import PlayerComparison from './components/PlayerComparison';
import AiAnalystChat from './components/AiAnalystChat';
import SystemModal from './components/SystemModal';

export default function App() {
  const [activeTab, setActiveTab] = useState('matches');
  const [comparedPlayerId, setComparedPlayerId] = useState(1024);
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [isLiveBackend, setIsLiveBackend] = useState(false);

  useEffect(() => {
    // Probe Spring Boot backend health
    async function checkHealth() {
      const base = api.getBaseUrl ? api.getBaseUrl() : 'http://localhost:8000/api/v1';
      const healthUrl = base.startsWith('/') ? '/health' : base.replace(/\/api\/v\d+.*$/, '') + '/health';
      try {
        const res = await fetch(healthUrl);
        if (res.ok) {
          setIsLiveBackend(true);
        }
      } catch (err) {
        setIsLiveBackend(false);
      }
    }
    checkHealth();
    const interval = setInterval(checkHealth, 15000);
    return () => clearInterval(interval);
  }, []);

  const handleComparePlayer = (playerId) => {
    setComparedPlayerId(playerId);
    setActiveTab('compare');
  };

  return (
    <div className="app-container">
      <Header
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onOpenStatusModal={() => setIsStatusModalOpen(true)}
        isLiveBackend={isLiveBackend}
      />

      <main className="main-content">
        {activeTab === 'matches' && (
          <MatchCenter
            onSelectPlayer={(id) => {
              setComparedPlayerId(id);
              setActiveTab('players');
            }}
          />
        )}

        {activeTab === 'players' && (
          <PlayerIntelligence onComparePlayer={handleComparePlayer} />
        )}

        {activeTab === 'compare' && (
          <PlayerComparison initialPlayerId={comparedPlayerId} />
        )}

        {activeTab === 'ai-analyst' && (
          <AiAnalystChat />
        )}
      </main>

      {/* Footer */}
      <footer
        style={{
          borderTop: '1px solid var(--border-subtle)',
          padding: '24px 20px',
          textAlign: 'center',
          fontSize: '12px',
          color: 'var(--text-muted)',
          background: 'rgba(10, 14, 23, 0.9)'
        }}
      >
        <div style={{ maxWidth: '1400px', margin: '0 auto', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            ⚽ <strong>PitchPulse Analytics</strong> — Enterprise Full-Stack Football Intelligence Platform
          </div>
          <div>
            Backend: Spring Boot 3.3.4 (Java 17) &nbsp;|&nbsp; Frontend: ReactJS (Vite) &nbsp;|&nbsp; OLAP: ClickHouse Star Schema
          </div>
        </div>
      </footer>

      {isStatusModalOpen && (
        <SystemModal onClose={() => setIsStatusModalOpen(false)} />
      )}
    </div>
  );
}
