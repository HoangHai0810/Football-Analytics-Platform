import React, { useState } from 'react';
import Header from './components/Header';
import MatchCenter from './components/MatchCenter';
import PlayerIntelligence from './components/PlayerIntelligence';
import PlayerComparison from './components/PlayerComparison';
import AiAnalystChat from './components/AiAnalystChat';

export default function App() {
  const [activeTab, setActiveTab] = useState('matches');
  const [comparedPlayerId, setComparedPlayerId] = useState(1024);

  const handleComparePlayer = (playerId) => {
    setComparedPlayerId(playerId);
    setActiveTab('compare');
  };

  return (
    <div className="app-container">
      <Header
        activeTab={activeTab}
        setActiveTab={setActiveTab}
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
          padding: '20px 24px',
          textAlign: 'center',
          fontSize: '13px',
          color: 'var(--text-muted)',
          background: 'rgba(10, 14, 23, 0.95)'
        }}
      >
        <div style={{ maxWidth: '1400px', margin: '0 auto', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            ⚽ <strong>PitchPulse</strong> — Nền Tảng Phân Tích Dữ Liệu Bóng Đá Chuyên Sâu
          </div>
          <div>
            Dữ liệu mở chính thức từ <strong>StatsBomb Open Data (2015 – 2024)</strong>
          </div>
        </div>
      </footer>
    </div>
  );
}
