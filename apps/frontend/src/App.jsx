import React, { useState } from 'react';
import Header from './components/Header';
import MatchCenter from './components/MatchCenter';
import PlayerIntelligence from './components/PlayerIntelligence';
import PlayerComparison from './components/PlayerComparison';
import AiAnalystChat from './components/AiAnalystChat';

export default function App() {
  const [activeTab, setActiveTab] = useState('matches');
  const [comparedPlayerId, setComparedPlayerId] = useState(null);

  const handleComparePlayer = (playerId) => {
    setComparedPlayerId(playerId);
    setActiveTab('compare');
  };

  return (
    <div className="app-container">
      <Header activeTab={activeTab} setActiveTab={setActiveTab} />

      <main className="main-content">
        {activeTab === 'matches' && <MatchCenter />}

        {activeTab === 'players' && (
          <PlayerIntelligence onComparePlayer={handleComparePlayer} />
        )}

        {activeTab === 'compare' && (
          <PlayerComparison initialPlayerId={comparedPlayerId} />
        )}

        {activeTab === 'ai-analyst' && <AiAnalystChat />}
      </main>

      <footer className="app-footer">
        <div className="app-footer-inner">
          PitchPulse — phân tích bóng đá từ dữ liệu đã đồng bộ
        </div>
      </footer>
    </div>
  );
}
